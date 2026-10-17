package com.folia.xiaomi.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.folia.xiaomi.data.lyrics.LrcParser
import com.folia.xiaomi.data.lyrics.LyricLine
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.text.Charsets

/**
 * 歌词仓库：
 * - 在线歌曲：从音源 API 拉取 LRC（含 LDDC 逐字增强格式）
 * - 本地歌曲：优先读取音频同目录同名 .lrc
 * - 均失败时：返回内置演示歌词，保证沉浸式歌词页始终有内容可渲染
 */
object LyricsRepository {

    suspend fun getLyric(context: Context, song: Song): List<LyricLine> =
        withContext(Dispatchers.IO) {
            val raw = when (song.source) {
                Source.ONLINE -> OnlineMusicRepository.fetchLyric(song.id)
                Source.LOCAL -> readLocalLrc(context, song.uri)
            }
            val parsed = LrcParser.parse(raw)
            if (parsed.isNotEmpty()) parsed else sampleLyric(song.title)
        }

    private fun readLocalLrc(context: Context, uriString: String): String = try {
        val uri = Uri.parse(uriString)
        val dataPath = context.contentResolver.query(
            uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: return ""
        if (dataPath.isBlank()) return ""
        val lrc = File(dataPath.substringBeforeLast('.') + ".lrc")
        if (lrc.exists()) lrc.readText(Charsets.UTF_8) else ""
    } catch (e: Exception) {
        ""
    }

    /** 内置演示歌词：当无网络歌词或本地歌词时兜底展示 */
    private fun sampleLyric(title: String): List<LyricLine> {
        val pool = listOf(
            "此刻，让歌词成为画面",
            "《$title》",
            "一字一句，随节拍呼吸",
            "仿佛叶脉在风中舒展",
            "把耳朵交给我",
            "把眼睛交给这段旋律",
            "—— Folia · 辞曲新境",
        )
        val step = 4000L
        return pool.mapIndexed { i, text ->
            val start = i * step
            val end = start + step * 2
            LyricLine(start, end, listOf(com.folia.xiaomi.data.lyrics.LyricWord(text, start, end)), text)
        }
    }
}
