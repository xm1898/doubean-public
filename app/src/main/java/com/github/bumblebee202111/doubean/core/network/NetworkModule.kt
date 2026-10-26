package com.github.bumblebee202111.doubean.core.network

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.github.bumblebee202111.doubean.coroutines.ApplicationScope
import com.github.bumblebee202111.doubean.security.DOUBAN_API_KEY
import com.github.bumblebee202111.doubean.util.AppAndDeviceInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.addCookie
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Singleton
    @Provides
    fun provideCookieStorage(
        @ApplicationScope coroutineScope: CoroutineScope,
    ): CookiesStorage =
        AcceptAllCookiesStorage().apply {
            coroutineScope.launch {
                addCookie(
                    "https://www.douban.com",
                    Cookie(
                        name = "apiKey",
                        value = DOUBAN_API_KEY
                    )
                )
            }
        }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        cookiesStorage: CookiesStorage,
        json: Json,
        appAndDeviceInfoProvider: AppAndDeviceInfoProvider,
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }
        install(HttpCookies) {
            storage = cookiesStorage
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, appAndDeviceInfoProvider.getUserAgent())
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.AcceptLanguage, "zh-CN,zh;q=0.9")
        }
    }

    @Provides
    @Singleton
    fun imageLoader(
        @ApplicationContext application: Context,
        appAndDeviceInfoProvider: AppAndDeviceInfoProvider,
    ): ImageLoader =
        ImageLoader.Builder(application)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder().addInterceptor { chain ->
                                return@addInterceptor chain.proceed(
                                    chain.request().newBuilder()
                                        .addHeader(
                                            "User-Agent",
                                            appAndDeviceInfoProvider.getImageUA()
                                        ).build()
                                )
                            }.build()
                        }
                    )
                )
            }
            .build()

    private fun AppAndDeviceInfoProvider.getImageUA(): String {
        return "api-client/$apiClient com.douban.frodo/$doubanVersion Android/$android product/$product vendor/$vendor model/$model brand/$brand  rom/$rom  network/$network  udid/$udid  platform/$platform udid/$udid"
    }
}
