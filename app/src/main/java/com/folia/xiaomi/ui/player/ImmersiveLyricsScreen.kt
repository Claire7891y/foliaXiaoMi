package com.folia.xiaomi.ui.player

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folia.xiaomi.adapt.XiaomiAdapt
import com.folia.xiaomi.data.lyrics.LyricLine
import com.folia.xiaomi.data.repository.LyricsRepository
import com.folia.xiaomi.player.PlayerManager
import com.folia.xiaomi.ui.common.ArtworkColor
import com.folia.xiaomi.ui.common.formatDuration
import kotlin.math.abs

/** 歌词动画模式 */
private enum class LyricMode(val label: String) {
    FLOW("浮动"), KARAOKE("逐字"), ZEN("倾诉"),
}

/**
 * 沉浸式全屏歌词页 —— Folia 的灵魂。
 * 以歌曲主色调渲染渐变背景，全屏歌词随节拍滚动并做逐字高亮（卡拉 OK），
 * 三种模式：浮动 / 逐字 / 倾诉。进入时隐藏系统栏，全屏沉浸。
 */
@Composable
fun ImmersiveLyricsScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = (context as? Activity)

    val queue by PlayerManager.queue.collectAsState()
    val index by PlayerManager.currentIndex.collectAsState()
    val playing by PlayerManager.isPlaying.collectAsState()
    val position by PlayerManager.positionMs.collectAsState()
    val duration = PlayerManager.durationMs

    val song = queue.getOrNull(index)

    // 进入/退出沉浸式系统栏
    LaunchedEffect(Unit) {
        activity?.let { XiaomiAdapt.enterImmersive(it) }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { XiaomiAdapt.exitImmersive(it, !isSystemInDarkTheme()) }
        }
    }

    // 歌词加载
    var lyrics by remember(song?.id) { mutableStateOf(emptyList<LyricLine>()) }
    LaunchedEffect(song?.id) {
        if (song != null) lyrics = LyricsRepository.getLyric(context, song)
    }

    // 背景主色调（取自专辑封面）
    var bgColor by remember(song?.id) { mutableStateOf(Color(0xFF1B9E55)) }
    LaunchedEffect(song?.id) {
        if (song != null) bgColor = ArtworkColor.forSong(song, context.contentResolver)
    }

    if (song == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("暂无播放", color = Color.White)
        }
        return
    }

    val onColor = if (bgColor.luminance() > 0.5f) Color(0xFF10241A) else Color(0xFFF2FFF6)
    val bgGradient = listOf(
        bgColor,
        lerp(bgColor, Color.Black, 0.55f),
        lerp(bgColor, Color.Black, 0.78f),
    )

    var mode by remember { mutableIntStateOf(0) }
    val currentMode = LyricMode.entries[mode]

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(bgGradient))) {
        // 顶部信息栏
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = onColor)
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, color = onColor, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("${song.artist} · ${song.album}", color = onColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            // 模式切换：左切回上一模式，右切到下一模式
            IconButton(onClick = { mode = (mode - 1 + LyricMode.entries.size) % LyricMode.entries.size }) {
                Icon(Icons.Outlined.GraphicEq, contentDescription = "上一模式：${currentMode.label}", tint = onColor)
            }
            IconButton(onClick = { mode = (mode + 1) % LyricMode.entries.size }) {
                Icon(
                    if (currentMode == LyricMode.KARAOKE) Icons.Filled.Lyrics else Icons.Outlined.Lyrics,
                    contentDescription = "当前模式：${currentMode.label}，点击切换",
                    tint = onColor,
                )
            }
        }

        // 歌词主体
        LyricsVisualizer(
            lines = lyrics,
            positionMs = position,
            mode = currentMode,
            activeColor = onColor,
            inactiveColor = onColor.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxSize(),
        )

        // 底部控制条
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.32f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(position), color = onColor, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = position.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
                    onValueChange = { PlayerManager.seekTo(it.toLong()) },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    enabled = duration > 0,
                )
                Text(formatDuration(duration), color = onColor, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { PlayerManager.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", tint = onColor, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(24.dp))
                IconButton(onClick = { PlayerManager.togglePlayPause() }) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "暂停" else "播放",
                        tint = onColor,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.width(24.dp))
                IconButton(onClick = { PlayerManager.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = onColor, modifier = Modifier.size(40.dp))
                }
            }
        }

        // 模式角标
        Text(
            currentMode.label,
            color = onColor.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 44.dp),
        )
    }
}

@Composable
private fun LyricsVisualizer(
    lines: List<LyricLine>,
    positionMs: Long,
    mode: LyricMode,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("加载歌词中…", color = inactiveColor, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val currentIndex = remember(lines, positionMs) { resolveCurrentLine(lines, positionMs) }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (mode != LyricMode.ZEN) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 4))
        }
    }

    if (mode == LyricMode.ZEN) {
        // 倾诉模式：只展示当前一行，居中大字号，切换有过渡动画
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            },
            modifier = modifier,
        ) { idx ->
            val line = lines[idx]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    line.text,
                    color = activeColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }
    } else {
        // 浮动 / 逐字模式：整页歌词滚动，当前行突出
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 230.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(count = lines.size, key = { it }) { i ->
                val line = lines[i]
                val isCurrent = i == currentIndex
                val distance = abs(i - currentIndex)
                val alpha = if (isCurrent) 1f else (0.4f / (1 + distance * 0.4f))
                val scale = if (isCurrent) 1.12f else 1f

                Box(
                    Modifier
                        .padding(vertical = 10.dp)
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    if (isCurrent && mode == LyricMode.KARAOKE) {
                        KaraokeText(line, positionMs, activeColor, inactiveColor)
                    } else {
                        Text(
                            line.text,
                            color = if (isCurrent) activeColor else inactiveColor,
                            fontSize = if (isCurrent) 26.sp else 17.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 40.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KaraokeText(line: LyricLine, positionMs: Long, active: Color, inactive: Color) {
    val annotated = buildAnnotatedString {
        if (line.words.isEmpty()) {
            withStyle(SpanStyle(color = active)) { append(line.text) }
        } else {
            for (word in line.words) {
                val color = if (word.startMs <= positionMs) active else inactive
                withStyle(SpanStyle(color = color)) { append(word.text) }
            }
        }
    }
    Text(
        annotated,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        lineHeight = 34.sp,
        modifier = Modifier.padding(horizontal = 40.dp),
    )
}

/** 定位当前歌词行 */
private fun resolveCurrentLine(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return 0
    val hit = lines.indexOfLast { positionMs >= it.startMs }
    return if (hit >= 0) hit else 0
}
