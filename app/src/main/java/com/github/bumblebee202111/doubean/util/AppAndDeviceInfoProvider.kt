package com.github.bumblebee202111.doubean.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生成与豆瓣移动端一致的设备指纹字段，供 UA / 图片请求头使用。
 */
@Singleton
class AppAndDeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val apiClient: String = "api-client/1 com.douban.frodo/7.77.0"
    val doubanVersion: String = "7.77.0"
    val android: String = Build.VERSION.RELEASE ?: "14"
    val product: String = Build.PRODUCT ?: "unknown"
    val vendor: String = Build.MANUFACTURER ?: "unknown"
    val model: String = Build.MODEL ?: "unknown"
    val brand: String = Build.BRAND ?: "unknown"
    val rom: String = (Build.DISPLAY ?: "unknown").replace(" ", "_")
    val network: String = "wifi"
    val platform: String = "android"
    val udid: String = deviceUdid()

    private fun deviceUdid(): String {
        val raw = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return if (raw.isNullOrBlank()) {
            UUID.nameUUIDFromBytes("doubean".toByteArray()).toString().replace("-", "")
        } else {
            raw
        }
    }

    /** 详情页 / 列表请求使用的 UA，形如豆瓣客户端。 */
    fun getUserAgent(): String = String.format(
        Locale.US,
        "api-client/1 com.douban.frodo/%s Android/%s product/%s vendor/%s model/%s brand/%s rom/%s network/%s udid/%s platform/%s",
        doubanVersion, android, product, vendor, model, brand, rom, network, udid, platform,
    )
}
