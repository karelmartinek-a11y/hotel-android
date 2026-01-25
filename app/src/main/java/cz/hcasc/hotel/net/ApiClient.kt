package cz.hcasc.hotel.net

import android.content.Context
import cz.hcasc.hotel.config.BuildConfigExt
import cz.hcasc.hotel.repo.DeviceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun create(context: Context, deviceRepo: DeviceRepo?): HotelApi {
        val client = buildOkHttp(deviceRepo)
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfigExt.BASE_URL)
            .client(client)
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
        return retrofit.create(HotelApi::class.java)
    }

    private fun buildOkHttp(deviceRepo: DeviceRepo?): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            val builder = req.newBuilder()
            runCatching {
                val token = deviceRepo?.getDeviceTokenOrNull()
                val deviceId = deviceRepo?.getDeviceIdOrNull()
                val displayName = deviceRepo?.getDisplayName()?.takeIf { it.isNotBlank() }
                if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
                if (!deviceId.isNullOrBlank()) builder.header("X-Device-Id", deviceId)
                if (!displayName.isNullOrBlank()) builder.header("X-Device-Name", displayName)
            }
            chain.proceed(builder.build())
        }

        val userAgentInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "HCASC-HotelApp/1 (Android)")
                    .build()
            )
        }

        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        return OkHttpClient.Builder()
            .connectTimeout(BuildConfigExt.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(BuildConfigExt.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(BuildConfigExt.HTTP_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(log)
            .build()
    }
}
