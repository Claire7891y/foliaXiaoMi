package com.folia.xiaomi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

/**
 * 应用入口：持有全局唯一的 ExoPlayer（由前台媒体服务与 UI 共享），并创建播放通知渠道。
 */
class FoliaApp : Application() {

    /** 全局播放器实例 */
    val player: ExoPlayer by lazy {
        val renderersFactory = DefaultRenderersFactory(this)
            // 优先使用硬件/FFmpeg 扩展解码，兼容小米不同 SoC（骁龙/天玑）
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        createPlaybackChannel()
    }

    private fun createPlaybackChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_MEDIA, "播放控制", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "前台播放与小米锁屏/控制中心播放控制"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_MEDIA = "media_playback"
    }
}
