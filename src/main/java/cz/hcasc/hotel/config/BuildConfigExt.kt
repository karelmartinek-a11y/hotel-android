package cz.hcasc.hotel.config

/**
 * Runtime constants.
 *
 * IMPORTANT:
 * - No external third-party runtime services.
 * - Canonical domain is always: https://hotel.hcasc.cz
 */
object BuildConfigExt {
    /** Base URL used by Retrofit. */
    const val BASE_URL: String = "https://hotel.hcasc.cz/"

    /** API base path (server: Nginx -> backend). */
    const val API_BASE_PATH: String = "api/"

    /** Periodic work intervals (minutes). */
    const val WORK_SEND_QUEUE_INTERVAL_MINUTES: Long = 30
    const val WORK_POLL_INTERVAL_MINUTES: Long = 15

    /**
     * WorkManager polling interval for notifications.
     * Must be >= 15 minutes for periodic work.
     */
    const val POLL_PERIODIC_MINUTES: Long = 15

    /** Minimum delay for retry/backoff (in seconds). */
    const val RETRY_BACKOFF_MIN_SECONDS: Long = 10

    /** Maximum delay for retry/backoff (in seconds). */
    const val RETRY_BACKOFF_MAX_SECONDS: Long = 5 * 60

    /**
     * Offline queue limits.
     *
     * The app is allowed to store only:
     * 1) activation token/hash
     * 2) temporary offline queue of unsent reports (including photos)
     *
     * After successful upload, local report data must be deleted.
     */
    const val OFFLINE_QUEUE_MAX_REPORTS: Int = 50

    /** Max photos per report (spec: 1..5). */
    const val MAX_PHOTOS_PER_REPORT: Int = 5

    /**
     * Client-side photo constraints before upload.
     *
     * We compress photos to JPEG to reduce bandwidth and storage.
     * (Server also enforces its own limits.)
     */
    const val PHOTO_JPEG_QUALITY: Int = 82

    /** Max longer side in pixels after downscale. */
    const val PHOTO_MAX_SIDE_PX: Int = 1600

    /**
     * Hard limit for a single photo file stored in offline queue (bytes).
     * If exceeded even after compression, we reject the photo.
     */
    const val PHOTO_MAX_BYTES: Long = 1_800_000 // ~1.8 MB

    /** Network timeouts. */
    const val HTTP_CONNECT_TIMEOUT_SECONDS: Long = 10
    const val HTTP_READ_TIMEOUT_SECONDS: Long = 30
    const val HTTP_WRITE_TIMEOUT_SECONDS: Long = 30

    /**
     * Welcome screen activation check.
     *
     * When online and not activated, we periodically check activation.
     */
    const val ACTIVATION_CHECK_MIN_INTERVAL_SECONDS: Long = 10
    const val ACTIVATION_CHECK_MAX_INTERVAL_SECONDS: Long = 120

    /**
     * Local notification channel.
     */
    const val NOTIFICATION_CHANNEL_ID: String = "reports"
    const val NOTIFICATION_CHANNEL_NAME: String = "Hotel hlášení"
}
