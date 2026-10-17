package com.folia.xiaomi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.folia.xiaomi.ui.player.ImmersiveLyricsScreen
import com.folia.xiaomi.ui.player.MiniPlayerBar
import com.folia.xiaomi.ui.playlist.PlaylistScreen

private const val DEST_PLAYLIST = 0
private const val DEST_PLAYER = 1

/** 主界面：曲库(本地/在线) 与 沉浸式歌词 两个目的地 */
@Composable
fun MainScreen(
    vm: MainViewModel,
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
) {
    var dest by rememberSaveable { mutableIntStateOf(DEST_PLAYLIST) }

    Box(Modifier.fillMaxSize()) {
        when (dest) {
            DEST_PLAYLIST -> Column(Modifier.fillMaxSize()) {
                PlaylistScreen(
                    vm = vm,
                    hasPermission = hasPermission,
                    onGrantPermission = onGrantPermission,
                    modifier = Modifier.weight(1f),
                )
                MiniPlayerBar(onOpenPlayer = { dest = DEST_PLAYER })
                NavigationBar(
                    Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = dest == DEST_PLAYLIST,
                        onClick = { dest = DEST_PLAYLIST },
                        icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "曲库") },
                        label = { Text("曲库") },
                    )
                    NavigationBarItem(
                        selected = dest == DEST_PLAYER,
                        onClick = { dest = DEST_PLAYER },
                        icon = { Icon(Icons.Filled.GraphicEq, contentDescription = "正在播放") },
                        label = { Text("正在播放") },
                    )
                }
            }
            else -> ImmersiveLyricsScreen(onExit = { dest = DEST_PLAYLIST })
        }
    }
}
