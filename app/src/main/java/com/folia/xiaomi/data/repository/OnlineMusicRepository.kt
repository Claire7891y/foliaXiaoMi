package com.folia.xiaomi.data.repository

import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 在线音源仓库。
 *
 * 兼容社区常见的 NeteaseCloudMusicApiEnhanced / 网易云 API 增强版返回结构：
 *   GET {base}/search?keywords=xxx&limit=30   -> result.songs[{id,name,ar:[{name}],al:{name,picUrl},dt}]
 *   GET {base}/song/url?id=xxx&br=320000      -> data[{url}]
 *   GET {base}/lyric?id=xxx                   -> lrc.lyric / tlyric.lyric
 *
 * 注意：在线曲库版权归原平台所有，本项目仅作技术学习演示。baseUrl 默认留空，
 * 请在 App 内"设置"填入你自己部署的 API 地址后再启用在线功能。
 */
object OnlineMusicRepository {

    /** 在线 API 基址，由用户在设置页填写；为空时在线功能自动降级为提示。 */
    var baseUrl: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(keyword: String): List<Song> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || keyword.isBlank()) return@withContext emptyList()
        val url = "$baseUrl/search?keywords=${URLEncoder.encode(keyword, "UTF-8")}&limit=30"
        val body = get(url) ?: return@withContext emptyList()
        parseSearch(body)
    }

    /** 解析在线歌曲的真实播放地址（部分接口返回为空时说明该曲不可播） */
    suspend fun resolveUrl(id: String): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext ""
        val url = "$baseUrl/song/url?id=$id&br=320000"
        val body = get(url) ?: return@withContext ""
        try {
            val root = JSONObject(body)
            root.optJSONArray("data")?.optJSONObject(0)?.optString("url") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun fetchLyric(id: String): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext ""
        val url = "$baseUrl/lyric?id=$id"
        val body = get(url) ?: return@withContext ""
        try {
            val root = JSONObject(body)
            val lrc = root.optJSONObject("lrc")?.optString("lyric") ?: ""
            val tlyric = root.optJSONObject("tlyric")?.optString("lyric") ?: ""
            if (tlyric.isBlank()) lrc else "$lrc\n$tlyric"
        } catch (e: Exception) {
            ""
        }
    }

    private fun get(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build())
            .execute().use { it.body?.string() }
    } catch (e: Exception) {
        null
    }

    private fun parseSearch(json: String): List<Song> = try {
        val root = JSONObject(json)
        val result = root.optJSONObject("result") ?: return emptyList()
        val arr = result.optJSONArray("songs") ?: return emptyList()
        val out = mutableListOf<Song>()
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            val ar = s.optJSONArray("ar") ?: s.optJSONArray("artists")
            val artist = ar?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() } ?: "未知"
            val al = s.optJSONObject("al") ?: s.optJSONObject("album")
            val album = al?.optString("name")?.takeIf { it.isNotBlank() } ?: "未知"
            val pic = al?.optString("picUrl")
            val id = s.optLong("id", 0L)
            out += Song(
                id = id.toString(),
                title = s.optString("name", "未知"),
                artist = artist,
                album = album,
                durationMs = s.optLong("dt", 0L),
                playUrl = "",
                artworkUri = pic,
                source = Source.ONLINE,
            )
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
