package cz.hcasc.hotel.repo

import cz.hcasc.hotel.net.HotelApi
import cz.hcasc.hotel.repo.model.ReportCategory
import cz.hcasc.hotel.repo.model.ReportItem

class ReportsRepo(private val api: HotelApi, private val deviceRepo: DeviceRepo) {

    suspend fun listOpen(category: ReportCategory): List<ReportItem> {
        val token = deviceRepo.getAuthHeaderOrNull()
        val resp = api.listOpenReports(auth = token, category = category.name)
        return resp.items.map {
            ReportItem(
                id = it.id,
                room = it.room,
                description = it.description,
                createdAtHuman = it.createdAt,
                thumbnailUrls = it.thumbnailUrls,
                photoUrls = it.photos.ifEmpty { it.thumbnailUrls }
            )
        }
    }

    suspend fun markDone(reportId: String) {
        val token = deviceRepo.getAuthHeaderOrNull()
        api.markReportDone(id = reportId, auth = token)
    }
}
