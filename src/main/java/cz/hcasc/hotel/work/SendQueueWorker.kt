package cz.hcasc.hotel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cz.hcasc.hotel.repo.QueueRepo
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker pro odesílání offline fronty reportů.
 *
 * Požadavky:
 * - běží jen když je dostupná síť (constraints nastaví plánovač)
 * - používá retry/backoff (WorkManager backoff)
 * - po úspěšném odeslání musí být lokální data (včetně fotek) smazána
 * - nesmí padat při chybách sítě/serveru
 */
class SendQueueWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = QueueRepo.get(applicationContext)

        return try {
            val summary = repo.sendAllQueued(
                maxToSend = 10,
                perItemTimeoutMillis = 35_000,
            )

            // Pokud je stále něco ve frontě, necháme WorkManager pokračovat (retry nebo další run).
            // Nejbezpečnější je vrátit success a nechat periodic/one-off chain, ale zde vracíme retry,
            // jen pokud víme, že došlo k přechodné chybě.
            // Repo nám dá flags.
            val out = workDataOf(
                "sent" to summary.sentCount,
                "failed" to summary.failedCount,
                "remaining" to summary.remainingCount,
                "lastErrorCode" to (summary.lastErrorCode ?: ""),
            )

            // Pokud jsme nic neposlali a něco selhalo přechodně, zkusit retry.
            if (summary.sentCount == 0 && summary.failedCount > 0 && summary.shouldRetry) {
                Result.retry()
            } else {
                Result.success(out)
            }
        } catch (ce: CancellationException) {
            // Work zrušen - propagovat.
            throw ce
        } catch (t: Throwable) {
            // Konzervativně retry (síť, timeouty, 5xx). WorkManager backoff to ošetří.
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "hotel_send_queue_periodic"
        private const val UNIQUE_ONEOFF = "hotel_send_queue_oneoff"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = PeriodicWorkRequestBuilder<SendQueueWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
        }

        /**
         * Trigger immediate send attempt after user creates a report.
         * If it fails, periodic worker will retry later (queue remains in DB).
         */
        fun triggerOneOff(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val req = OneTimeWorkRequestBuilder<SendQueueWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONEOFF,
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }
    }
}
