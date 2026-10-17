package com.folia.xiaomi.data.model

/** 音源来源 */
enum class Source {
    /** 本地音乐文件 */
    LOCAL,

    /** 在线搜索（第三方音源 API，演示用途） */
    ONLINE,
}

/**
 * 一首歌的统一模型。
 * - 本地：uri = content://media/...，artworkUri = 专辑封面 content uri
 * - 在线：id = 音源歌曲 id，playUrl 为解析出的播放地址，artworkUri 为封面 http 地址
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    val uri: String = "",
    val playUrl: String = "",
    val artworkUri: String? = null,
    val source: Source = Source.LOCAL,
) {
    /** 实际播放地址：在线用 playUrl，本地用 uri */
    val playbackUri: String
        get() = if (source == Source.ONLINE && playUrl.isNotBlank()) playUrl else uri
}
