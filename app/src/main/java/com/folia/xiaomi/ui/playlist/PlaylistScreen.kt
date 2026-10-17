package com.folia.xiaomi.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.player.PlayerManager
import com.folia.xiaomi.ui.MainViewModel
import com.folia.xiaomi.ui.common.formatDuration

@Composable
fun PlaylistScreen(
    vm: MainViewModel,
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("本地") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("在线") })
        }
        when (tab) {
            0 -> LocalTab(vm, hasPermission, onGrantPermission)
            else -> OnlineTab(vm)
        }
    }
}

@Composable
private fun LocalTab(vm: MainViewModel, hasPermission: Boolean, onGrantPermission: () -> Unit) {
    val songs by vm.localSongs
    if (!hasPermission) {
        EmptyState(text = "请授予音乐读取权限以扫描本地歌曲", buttonText = "授予权限", onClick = onGrantPermission)
    } else if (songs.isEmpty()) {
        EmptyState(text = "没有扫描到本地音乐", buttonText = null, onClick = null)
    } else {
        LazyColumn {
            items(songs, key = { it.id }) { song ->
                SongRow(song = song, onClick = { PlayerManager.playQueue(songs, songs.indexOf(song)) })
            }
        }
    }
}

@Composable
private fun OnlineTab(vm: MainViewModel) {
    val songs by vm.onlineSongs
    val searching by vm.searching
    val onlineReady by vm.onlineReady
    var query by remember { mutableStateOf("") }
    var showConfig by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("搜索歌曲 / 歌手 / 专辑") },
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { if (query.isNotBlank()) vm.search(query) }) { Text("搜索") }
            TextButton(onClick = { showConfig = true }) { Text("音源") }
        }

        if (!onlineReady) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = { showConfig = true },
            ) {
                Text(
                    "未配置在线音源 API，点击设置后即可在线搜索（仅供学习演示）",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        when {
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            songs.isEmpty() -> EmptyState(text = "输入关键词搜索在线歌曲", buttonText = null, onClick = null)
            else -> LazyColumn {
                items(songs, key = { it.id }) { song ->
                    SongRow(song = song, onClick = { PlayerManager.playQueue(songs, songs.indexOf(song)) })
                }
            }
        }
    }

    if (showConfig) {
        ApiConfigDialog(
            current = com.folia.xiaomi.data.repository.OnlineMusicRepository.baseUrl,
            onSave = { url -> vm.setOnlineBaseUrl(url); showConfig = false },
            onDismiss = { showConfig = false },
        )
    }
}

@Composable
private fun ApiConfigDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("在线音源 API 地址") },
        text = {
            Column {
                Text("填写你自行部署的 NeteaseCloudMusicApiEnhanced 地址（如 https://your-api.example.com）。留空则禁用在线搜索。", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("https://...") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EmptyState(text: String, buttonText: String?, onClick: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (buttonText != null && onClick != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onClick) { Text(buttonText) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artworkUri ?: "")
                .crossfade(true)
                .build(),
            contentDescription = song.album,
            modifier = Modifier.size(52.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (song.durationMs > 0) {
            Text(
                formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
