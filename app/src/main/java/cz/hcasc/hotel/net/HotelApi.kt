package cz.hcasc.hotel.net

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Minimal Retrofit rozhraní pro potřeby mobilní appky.
 */
interface HotelApi {
    // Device lifecycle
    @POST("api/device/register")
    suspend fun deviceRegister(@Body body: DeviceRegisterRequest): DeviceStatusResponse

    @GET("api/device/status")
    suspend fun deviceStatus(@Query("device_id") deviceId: String): DeviceStatusResponse

    @POST("api/device/challenge")
    suspend fun deviceChallenge(@Body body: DeviceChallengeRequest): DeviceChallengeResponse

    @POST("api/device/verify")
    suspend fun deviceVerify(@Body body: DeviceVerifyRequest): DeviceVerifyResponse

    // Reports
    @Multipart
    @POST("api/reports")
    suspend fun createReport(
        @Part("type") type: String,
        @Part("room") room: Int,
        @Part("description") description: String?,
        @Part("createdAtEpochMs") createdAtEpochMs: Long,
        @Part photos: List<MultipartBody.Part>
    ): CreateReportResponse

    @GET("api/reports/open")
    suspend fun listOpenReports(
        @Header("Authorization") auth: String?,
        @Query("category") category: String
    ): ReportListResponse

    @POST("api/reports/mark-done")
    suspend fun markReportDone(
        @Query("id") id: String,
        @Header("Authorization") auth: String?
    ): GenericOkResponse

    // Polling
    @GET("api/poll/new-since")
    suspend fun newSince(
        @Header("Authorization") auth: String?,
        @Query("device_id") deviceId: String?,
        @Query("last_seen_find_id") lastSeenOpenFindsId: Long?,
        @Query("last_seen_issue_id") lastSeenOpenIssuesId: Long?
    ): NewSinceResponse
}
