package cz.hcasc.hotel.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cz.hcasc.hotel.notifications.Notifier
import cz.hcasc.hotel.repo.DeviceRepo
import cz.hcasc.hotel.repo.QueueRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodicky kontroluje, zda jsou nové OPEN záznamy (nálezy/závady) pro recepci/údržbu.
 * Bez FCM: polling přes WorkManager.
 *
 * Požadavky:
 * - rozumný interval (min. 15 min periodicky)
 * - bez pádů při výpadku internetu
 * - používá pouze last_seen (čas nebo id) uložené lokálně
 * - po kliknutí na notifikaci otevře aplikaci na příslušné záložce
 */
class PollNewWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val deviceRepo = DeviceRepo.from(applicationContext)
            val queueRepo = QueueRepo.get(applicationContext)

            // Pokud nejsme aktivovaní, nemá smysl pollovat (a nechceme spamovat endpointy).
            val activation = deviceRepo.getActivationSnapshot()
            if (!activation.isActivated) {
                return@withContext Result.success()
            }

            // Nejprve se pokusíme odeslat případnou offline frontu (pokud už je síť).
            // Tím se zároveň minimalizují false-positive notifikace.
            runCatching { queueRepo.sendAllQueued(maxToSend = 5) }

            val since = deviceRepo.getLastSeenSnapshot()

            val api = deviceRepo.api()
            val auth = deviceRepo.getAuthHeaderOrNull() ?: return@withContext Result.success()
            val body = api.newSince(
                auth = auth,
                deviceId = activation.deviceId,
                lastSeenOpenFindsId = since.lastSeenOpenFindsId,
                lastSeenOpenIssuesId = since.lastSeenOpenIssuesId
            )

            // Uložit serverem vrácené lastSeen hodnoty (i když nejsou nové položky)
            // aby se minimalizoval zbytečný polling.
            deviceRepo.updateLastSeen(
                lastSeenOpenFindsId = body.lastSeenOpenFindsId,
                lastSeenOpenIssuesId = body.lastSeenOpenIssuesId
            )

            val nm: NotificationManager? = applicationContext.getSystemService()
            if (nm == null) return@withContext Result.success()

            // Notifikace pouze pokud přibylo něco nového.
            if (body.newOpenFindsCount > 0) {
                Notifier.notifyNewFind(applicationContext)
            }
            if (body.newOpenIssuesCount > 0) {
                Notifier.notifyNewIssue(applicationContext)
            }

            Result.success()
        } catch (_: Exception) {
            // Záměrně nelogujeme citlivé údaje.
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "hotel_poll_new"

        /**
         * Periodický polling (min. 15 min) + síťová podmínka.
         * Volat při startu aplikace (např. v App.kt).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = PeriodicWorkRequestBuilder<PollNewWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }
    }
}
