package com.lucidera.investigations.data

import android.content.Context
import com.lucidera.investigations.BuildConfig
import com.lucidera.investigations.data.local.FieldbookDatabase
import com.lucidera.investigations.data.network.WaybackApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val database = FieldbookDatabase.getDatabase(context)

    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        Retrofit.Builder()
            .baseUrl("https://archive.org/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val waybackApi: WaybackApi by lazy {
        retrofit.create(WaybackApi::class.java)
    }

    val repository: InvestigationRepository by lazy {
        InvestigationRepository(
            caseDao = database.caseDao(),
            leadDao = database.leadDao(),
            entityDao = database.entityProfileDao(),
            attachmentDao = database.attachmentDao(),
            waybackApi = waybackApi
        )
    }
}
