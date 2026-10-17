package com.folia.xiaomi.adapt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 小米设备专项适配。
 *
 * 覆盖点：
 * - HyperOS / MIUI 全面屏：刘海/挖孔（DisplayCutout）、全面屏手势导航条
 * - 深色模式：状态栏/导航栏图标明暗随主题切换
 * - 电池优化：引导用户将本应用设为"不省电"，保证锁屏播放不被打断
 * - 长屏 / 高密度屏：统一用 dp 布局 + 边缘到边沉浸式窗口
 */
object XiaomiAdapt {

    /** 是否小米系设备（小米 / Redmi / POCO） */
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.let { m ->
            m.contains("xiaomi", ignoreCase = true) ||
                m.contains("redmi", ignoreCase = true) ||
                m.contains("poco", ignoreCase = true)
        }

    /** 布局延伸到系统栏区域，ContentPadding 交给 Compose 处理 */
    fun edgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    /** 设置状态栏/导航栏图标为深色（浅色背景）或浅色（深色背景） */
    fun applySystemBarIcons(activity: Activity, lightIcons: Boolean) {
        val controller = WindowInsetsControllerCompat(
            activity.window, activity.window.decorView
        )
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
    }

    /** 进入全屏沉浸：隐藏状态栏与手势条（用于全屏歌词） */
    fun enterImmersive(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** 退出全屏沉浸：恢复系统栏，并设置图标明暗 */
    fun exitImmersive(activity: Activity, lightIcons: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            isAppearanceLightStatusBars = lightIcons
            isAppearanceLightNavigationBars = lightIcons
        }
    }

    /** 保持屏幕常亮（全屏歌词/长按播放时使用），用后需 release */
    fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "FoliaXiaomi:lyrics")
            .apply { acquire(2 * 60 * 60 * 1000L) }
    }

    /** 引导用户将本应用加入小米"不省电"白名单（可选，弹系统设置） */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // 部分 ROM 不支持该 Intent，忽略
        }
    }

    /** 判断播放进程是否被系统杀死过（用于错误提示时的检测提示），此处仅作占位工具 */
    fun deviceBrand(): String = Build.BRAND
}
