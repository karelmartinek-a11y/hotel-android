package cz.hcasc.hotel

import android.app.Application
import cz.hcasc.hotel.db.AppDatabase
import cz.hcasc.hotel.net.ApiClient
import cz.hcasc.hotel.net.HotelApi
import cz.hcasc.hotel.repo.DeviceRepo
import cz.hcasc.hotel.repo.QueueRepo
import cz.hcasc.hotel.repo.ReportsRepo
import cz.hcasc.hotel.security.ChallengeSigner
import cz.hcasc.hotel.security.DeviceIdentity
import cz.hcasc.hotel.work.SendQueueWorker
import cz.hcasc.hotel.work.PollNewWorker

class App : Application() {

    // Very small "manual DI" without heavy frameworks.
    // These objects are app-singletons.
    lateinit var db: AppDatabase
        private set

    lateinit var identity: DeviceIdentity
        private set

    lateinit var api: HotelApi
        private set

    lateinit var deviceRepo: DeviceRepo
        private set

    lateinit var queueRepo: QueueRepo
        private set

    lateinit var reportsRepo: ReportsRepo
        private set

    override fun onCreate() {
        super.onCreate()

        db = AppDatabase.build(this)
        identity = DeviceIdentity
        api = ApiClient.create(this, null)

        deviceRepo = DeviceRepo(
            appContext = this,
            db = db,
            api = api,
            identity = identity,
            signer = ChallengeSigner
        )

        queueRepo = QueueRepo(
            appContext = this,
            api = api,
            db = db
        )

        reportsRepo = ReportsRepo(api = api, deviceRepo = deviceRepo)

        // Background jobs (network constrained):
        // - periodic polling for new OPEN items (no FCM)
        // - periodic flushing of offline queue (reports with photos)
        PollNewWorker.schedule(this)
        SendQueueWorker.schedulePeriodic(this)
    }
}
