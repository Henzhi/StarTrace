package com.startrace.core.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release 构建中使用：WARN+ 级别日志同时写入 Firebase Crashlytics
 */
class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return

        // 写入系统 logcat
        Log.println(priority, tag, message)

        // 写入 Crashlytics（非异常日志，用于辅助排查）
        if (t != null) {
            FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
