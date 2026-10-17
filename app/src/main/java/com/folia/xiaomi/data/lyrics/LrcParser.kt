package com.folia.xiaomi.data.lyrics

/**
 * 一个"词"单元：卡拉 OK 逐字高亮的最小粒度。
 * text 可能为单个字符或一段短语；startMs/endMs 为绝对时间轴（毫秒）。
 */
data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/** 一行歌词：startMs 起始，endMs 结束，words 支持逐字时间轴 */
data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val words: List<LyricWord>,
    val text: String,
)

/**
 * LRC 解析器。
 * 兼容标准行级 LRC `[mm:ss.xx]文本`，以及 LDDC 等工具生成的逐字增强格式：
 * `[00:12.34][00:12.50]这[00:12.80]是[00:13.00]逐[00:13.30]字`
 */
object LrcParser {

    private val timeTag = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

    fun parse(raw: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (line in raw.lineSequence()) {
            if (line.isBlank()) continue
            val tags = timeTag.findAll(line).map { toMs(it) }.toList()
            if (tags.isEmpty()) continue

            val textStart = line.indexOf(']') + 1
            val text = line.substring(textStart)
            if (text.isBlank()) continue

            if (tags.size == 1) {
                val end = tags[0] + 5000
                lines += LyricLine(tags[0], end, listOf(LyricWord(text, tags[0], end)), text)
            } else {
                // 逐字时间轴：按字符切分文本，逐字符分配起止时间
                val chars = text.map { it.toString() }
                val words = chars.mapIndexedNotNull { i, ch ->
                    val start = tags.getOrElse(i) { tags.last() }
                    val end = tags.getOrElse(i + 1) { tags.last() + 300 }
                    LyricWord(ch, start, end)
                }
                val first = words.firstOrNull()?.startMs ?: tags.first()
                val lastEnd = words.lastOrNull()?.endMs ?: (first + 4000)
                lines += LyricLine(first, lastEnd, words, text.trim())
            }
        }
        lines.sortBy { it.startMs }
        return lines
    }

    private fun toMs(m: MatchResult): Long {
        val min = m.groupValues[1].toLongOrNull() ?: 0L
        val sec = m.groupValues[2].toLongOrNull() ?: 0L
        val frac = m.groupValues[3]
        val ms = when (frac.length) {
            2 -> frac.toLongOrNull()?.times(10) ?: 0L
            3 -> frac.toLongOrNull() ?: 0L
            1 -> frac.toLongOrNull()?.times(100) ?: 0L
            else -> 0L
        }
        return min * 60_000L + sec * 1_000L + ms
    }
}
