package cz.hcasc.hotel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QueuedReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QueuedReportEntity): Long

    @Query("SELECT * FROM queued_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QueuedReportEntity?

    @Query("DELETE FROM queued_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE queued_reports SET last_error = :error WHERE id = :id")
    suspend fun setLastError(id: Long, error: String?)

    @Query("SELECT COUNT(*) FROM queued_reports")
    suspend fun countQueued(): Int

    @Query("SELECT * FROM queued_reports ORDER BY created_at_ms ASC LIMIT :limit")
    suspend fun listOldestFirst(limit: Int): List<QueuedReportEntity>
}

@Dao
interface QueuedPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QueuedPhotoEntity): Long

    @Query("SELECT * FROM queued_photos WHERE queued_report_id = :reportId ORDER BY photo_index ASC")
    suspend fun listByReportId(reportId: Long): List<QueuedPhotoEntity>

    @Query("DELETE FROM queued_photos WHERE queued_report_id = :reportId")
    suspend fun deleteByReportId(reportId: Long)
}

@Dao
interface DeviceStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceStateEntity)

    @Query("SELECT * FROM device_state LIMIT 1")
    suspend fun getRaw(): DeviceStateEntity?

    @Transaction
    suspend fun get(): DeviceStateEntity {
        val current = getRaw()
        if (current != null) return current
        val seeded = DeviceStateEntity()
        upsert(seeded)
        return seeded
    }
}
