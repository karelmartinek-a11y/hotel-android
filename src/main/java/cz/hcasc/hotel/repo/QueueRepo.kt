package cz.hcasc.hotel.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import cz.hcasc.hotel.db.AppDatabase
import cz.hcasc.hotel.db.QueuedPhotoEntity
import cz.hcasc.hotel.db.QueuedReportEntity
import cz.hcasc.hotel.net.HotelApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Offline fronta reportů (1..5 fotek) – jediná úloha: uložit, pokusit se odeslat, po úspěchu smazat.
 */
class QueueRepo(
    private val appContext: Context,
    private val api: HotelApi,
    private val db: AppDatabase
) {
    data class SendSummary(val sentCount: Int, val failedCount: Int, val remainingCount: Int, val shouldRetry: Boolean, val lastErrorCode: String?)

    companion object {
        fun get(context: Context): QueueRepo {
            val app = context.applicationContext as cz.hcasc.hotel.App
            return app.queueRepo
        }

        private const val MAX_PHOTOS = 5
        private const val JPEG_QUALITY = 78
        private const val MAX_DIMENSION_PX = 1600
    }

    suspend fun enqueueReport(
        type: String,
        room: Int,
        description: String?,
        photoUris: List<Uri>,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        require(photoUris.isNotEmpty()) { "At least 1 photo is required" }
        require(photoUris.size <= MAX_PHOTOS) { "Too many photos" }
        val reportId = db.queuedReportDao().insert(
            QueuedReportEntity(
                localUuid = UUID.randomUUID().toString(),
                type = type,
                room = room,
                description = description?.takeIf { it.isNotBlank() },
                createdAtEpochMs = createdAtEpochMs
            )
        )
        photoUris.forEachIndexed { idx, uri ->
            val outFile = persistCompressedJpeg(uri, reportId, idx)
            db.queuedPhotoDao().insert(
                QueuedPhotoEntity(
                    queuedReportId = reportId,
                    index = idx,
                    localPath = outFile.absolutePath,
                    mimeType = "image/jpeg",
                    sizeBytes = outFile.length()
                )
            )
        }
        reportId
    }

    suspend fun sendOne(queuedReportId: Long) = withContext(Dispatchers.IO) {
        val report = db.queuedReportDao().getById(queuedReportId) ?: return@withContext
        val photos = db.queuedPhotoDao().listByReportId(queuedReportId)

        try {
            val parts = buildMultipart(photos)
            api.createReport(
                type = report.type,
                room = report.room,
                description = report.description,
                createdAtEpochMs = report.createdAtEpochMs,
                photos = parts
            )
            deleteLocalFilesQuietly(photos)
            db.queuedPhotoDao().deleteByReportId(queuedReportId)
            db.queuedReportDao().deleteById(queuedReportId)
        } catch (e: Exception) {
            db.queuedReportDao().setLastError(queuedReportId, e.message)
            throw e
        }
    }

    suspend fun sendAllQueued(maxToSend: Int = 50, perItemTimeoutMillis: Long = 30_000): SendSummary =
        withContext(Dispatchers.IO) {
            var sent = 0
            var failed = 0
            var lastError: String? = null
            val items = db.queuedReportDao().listOldestFirst(maxToSend)
            for (item in items) {
                try {
                    withContext(Dispatchers.IO) {
                        sendOne(item.id)
                    }
                    sent++
                } catch (t: Throwable) {
                    failed++
                    lastError = t.javaClass.simpleName
                }
            }
            val remaining = db.queuedReportDao().countQueued()
            SendSummary(sent, failed, remaining, failed > 0, lastError)
        }

    private fun buildMultipart(photos: List<QueuedPhotoEntity>): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()
        photos.sortedBy { it.index }.forEach { p ->
            val file = File(p.localPath)
            val body = file.asRequestBody((p.mimeType).toMediaType())
            parts += MultipartBody.Part.createFormData(
                name = "photos",
                filename = "photo_${p.index}.jpg",
                body = body
            )
        }
        return parts
    }

    private fun persistCompressedJpeg(sourceUri: Uri, reportId: Long, index: Int): File {
        val reportsDir = File(appContext.filesDir, "queued_reports")
        val reportDir = File(reportsDir, reportId.toString())
        reportDir.mkdirs()
        val out = File(reportDir, "photo_$index.jpg")
        val bytes = readAndCompressJpeg(sourceUri)
        out.outputStream().use { it.write(bytes) }
        return out
    }

    private fun readAndCompressJpeg(sourceUri: Uri): ByteArray {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val (w, h) = bounds.outWidth to bounds.outHeight
        require(w > 0 && h > 0) { "Neplatný obrázek" }

        val sampleSize = computeInSampleSize(w, h, MAX_DIMENSION_PX, MAX_DIMENSION_PX)
        val decodeOpts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
        }
        val bmp = resolver.openInputStream(sourceUri).use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: throw IllegalArgumentException("Nelze načíst obrázek")
        val scaled = scaleDownIfNeeded(bmp, MAX_DIMENSION_PX)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        if (scaled !== bmp) bmp.recycle()
        scaled.recycle()
        return baos.toByteArray()
    }

    private fun scaleDownIfNeeded(bmp: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bmp.width, bmp.height)
        if (maxSide <= maxDim) return bmp
        val scale = maxDim.toFloat() / maxSide.toFloat()
        val nw = (bmp.width * scale).toInt().coerceAtLeast(1)
        val nh = (bmp.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }

    private fun computeInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        var halfW = width / 2
        var halfH = height / 2
        while (halfW / inSampleSize >= reqW && halfH / inSampleSize >= reqH) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun deleteLocalFilesQuietly(photos: List<QueuedPhotoEntity>) {
        photos.forEach { p ->
            runCatching {
                val f = File(p.localPath)
                if (f.exists()) f.delete()
            }
        }
        runCatching {
            photos.mapNotNull { File(it.localPath).parentFile }
                .distinct()
                .forEach { dir ->
                    dir.listFiles()?.let { if (it.isEmpty()) dir.delete() }
                }
        }
    }
}
