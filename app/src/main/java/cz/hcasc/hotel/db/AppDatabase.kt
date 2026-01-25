package cz.hcasc.hotel.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        QueuedReportEntity::class,
        QueuedPhotoEntity::class,
        DeviceStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queuedReportDao(): QueuedReportDao
    abstract fun queuedPhotoDao(): QueuedPhotoDao
    abstract fun deviceStateDao(): DeviceStateDao

    companion object {
        private const val DB_NAME = "hotel_app.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
