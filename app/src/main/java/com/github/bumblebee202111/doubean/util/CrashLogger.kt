package com.github.bumblebee202111.doubean.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.github.bumblebee202111.doubean.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把未捕获异常写进 App 私有目录，方便定位闪退。
 * 路径：Android/data/<包名>/files/crash-yyyyMMdd-HHmmss.txt
 */
object CrashLogger {

    private const val TAG = "DoubeanCrash"

    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val text = buildString {
            appendLine("time: $stamp")
            appendLine("thread: ${thread.name}")
            appendLine(
                "device: ${Build.MANUFACTURER} ${Build.MODEL} / Android " +
                    "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine()
            appendLine(Log.getStackTraceString(throwable))
        }
        runCatching { File(dir, "crash-$stamp.txt").writeText(text) }
        runCatching { Log.e(TAG, text) }
    }
}
