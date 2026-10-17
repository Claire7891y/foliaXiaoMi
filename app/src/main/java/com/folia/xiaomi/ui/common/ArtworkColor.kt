package com.folia.xiaomi.ui.common

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 从专辑封面提取"主色调"，供沉浸式歌词背景使用。
 * 本地走 ContentResolver，在线走 HTTP，均缩略后取平均色；失败回退主题色。
 */
object ArtworkColor {

    private val cache = mutableMapOf<String, Color>()
    private val Fallback = Color(0xFF1B9E55)

    suspend fun forSong(song: Song?, resolver: ContentResolver): Color =
        withContext(Dispatchers.IO) {
            if (song == null || song.artworkUri.isNullOrBlank()) return@withContext Fallback
            cache[song.id]?.let { return@withContext it }
            val bmp = loadBitmap(song, resolver)
            if (bmp == null) return@withContext Fallback
            val color = averageColor(bmp)
            cache[song.id] = color
            color
        }

    private fun loadBitmap(song: Song, resolver: ContentResolver): Bitmap? = try {
        val stream = when (song.source) {
            Source.LOCAL -> resolver.openInputStream(Uri.parse(song.artworkUri))
            Source.ONLINE -> {
                val conn = URL(song.artworkUri).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.inputStream
            }
        } ?: return null

        stream.use { s ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeStream(s, null, opts)
        }
    } catch (_: Exception) {
        null
    }

    private fun averageColor(bmp: Bitmap): Color {
        val small = Bitmap.createScaledBitmap(bmp, 1, 1, false)
        val px = small.getPixel(0, 0)
        small.recycle()
        return Color(
            red = AndroidColor.red(px) / 255f,
            green = AndroidColor.green(px) / 255f,
            blue = AndroidColor.blue(px) / 255f,
        )
    }
}
