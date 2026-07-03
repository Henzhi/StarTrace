package com.startrace

import android.app.Application
import com.google.firebase.BuildConfig
import com.startrace.core.util.CrashlyticsTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 星迹 Application 入口
 *
 * Hilt DI 根节点 + Timber 日志初始化。
 * Firebase 在 Release 构建中自动初始化（通过 google-services plugin）。
 */
@HiltAndroidApp
class StarTraceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Timber 日志：Debug 全量输出，Release 仅 WARN+ 上报 Crashlytics
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    companion object {
        lateinit var instance: StarTraceApplication
            private set
    }
}
