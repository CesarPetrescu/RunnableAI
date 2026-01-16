package ai.runnable.local.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}
