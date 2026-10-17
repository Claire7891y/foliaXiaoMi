package com.folia.xiaomi.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.model.Source

/**
 * 本地音乐库：通过 MediaStore 扫描设备内音频，读取系统播放记录中的标签信息。
 * 兼容 MIUI/HyperOS 的媒体扫描与权限模型。
 */
object LocalMusicRepository {

    private val albumArtUri = Uri.parse("content://media/external/audio/albumart")

    fun scan(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (c.moveToNext()) {
                val mediaId = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                songs += Song(
                    id = mediaId.toString(),
                    title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "未知",
                    artist = c.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "未知",
                    album = c.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "未知",
                    durationMs = c.getLong(durCol),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId
                    ).toString(),
                    artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtUri, albumId).toString()
                    } else null,
                    source = Source.LOCAL,
                )
            }
        }
        return songs
    }
}
