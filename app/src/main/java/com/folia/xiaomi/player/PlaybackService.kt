package com.folia.xiaomi.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.folia.xiaomi.FoliaApp
import com.folia.xiaomi.MainActivity

/**
 * 前台媒体服务。
 * 基于 Media3 的 MediaSessionService：播放时自动升级为前台服务，
 * 并接管系统媒体通知、小米锁屏与控制中心的播放控制（耳机/蓝牙按键、上一首/下一首等）。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as FoliaApp
        mediaSession = MediaSession.Builder(this, app.player).build()
        mediaSession?.setSessionActivity(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.let { it.release() }
        mediaSession = null
        super.onDestroy()
    }
}
