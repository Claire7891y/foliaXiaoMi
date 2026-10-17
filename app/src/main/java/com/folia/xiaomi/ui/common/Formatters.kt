package com.folia.xiaomi.ui.common

import java.util.Locale

/** 毫秒 -> mm:ss */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}
