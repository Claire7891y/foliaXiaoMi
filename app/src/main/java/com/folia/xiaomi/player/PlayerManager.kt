package com.folia.xiaomi.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.folia.xiaomi.FoliaApp
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.model.Source
import com.folia.xiaomi.data.repository.OnlineMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放管理单例：UI 只与本对象交互，真正播放由 FoliaApp 持有的 ExoPlayer 完成，
 * 播放状态通过 StateFlow 暴露给 Compose。
 */
object PlayerManager {

    private lateinit var appContext: Context
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 初始化：绑定全局播放器并启动进度轮询 */
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        player = (appContext as FoliaApp).player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) _isPlaying.value = false
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = player.currentMediaItemIndex
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.value = error.message ?: "播放失败，可能是版权受限或网络异常"
            }
        })
        scope.launch {
            while (isActive) {
                _positionMs.value = if (player.isPlaying) player.currentPosition else _positionMs.value
                delay(200)
            }
        }
    }

    val currentSong: Song?
        get() = _queue.value.getOrNull(_currentIndex.value)

    val durationMs: Long
        get() = if (player.duration > 0) player.duration else 0L

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        // 在线歌曲需要先解析真实播放地址
        scope.launch {
            val resolved = songs.map { song ->
                if (song.source == Source.ONLINE && song.playUrl.isBlank()) {
                    song.copy(playUrl = OnlineMusicRepository.resolveUrl(song.id))
                } else song
            }
            commitQueue(resolved, startIndex)
        }
    }

    fun playIndex(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        player.seekTo(index, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (_queue.value.isEmpty()) return
        val idx = (_currentIndex.value + 1).mod(_queue.value.size)
        player.seekTo(idx, 0L)
        player.prepare()
        player.play()
    }

    fun previous() {
        if (_queue.value.isEmpty()) return
        // 已播放超过 3s 则回到开头，否则上一首
        if (player.currentPosition > 3000) {
            player.seekTo(_currentIndex.value, 0L)
        } else {
            val idx = (_currentIndex.value - 1).mod(_queue.value.size)
            player.seekTo(idx, 0L)
        }
        player.prepare()
        player.play()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceIn(0L, durationMs))
    }

    private fun commitQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        player.setMediaItems(songs.map(::songToMediaItem), startIndex, 0L)
        player.prepare()
        player.play()
        _currentIndex.value = startIndex
        _error.value = null
        ensureService()
    }

    private fun songToMediaItem(song: Song): MediaItem {
        val uri = Uri.parse(song.playbackUri)
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.artworkUri?.let(Uri::parse))
                    .build()
            )
            .build()
    }

    /** 启动前台媒体服务：锁屏与控制中心可见 */
    private fun ensureService() {
        val intent = Intent(appContext, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }
}
