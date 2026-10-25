package com.tasky.app.core.markdown

/**
 * Minimal markdown block/inline parser used for rendering task notes in Compose.
 * Supports: headings (#..###), unordered lists (- item), bold **x**, italic *x*,
 * inline code `x`, and checkbox lines "- [ ]" / "- [x]".
 */
object MarkdownParser {

    sealed interface Block {
        data class Heading(val level: Int, val spans: List<Span>) : Block
        data class Paragraph(val spans: List<Span>) : Block
        data class ListItem(val spans: List<Span>, val checkbox: Boolean?, val checked: Boolean) : Block
        data class Divider(val unused: Int = 0) : Block
    }

    sealed interface Span {
        data class Text(val text: String) : Span
        data class Bold(val inner: String) : Span
        data class Italic(val inner: String) : Span
        data class Code(val inner: String) : Span
    }

    private val HEADING = Regex("^(#{1,3})\\s+(.*)$")
    private val LIST = Regex("^[-*]\\s+(.*)$")
    private val CHECK = Regex("^\\[( |x|X)]\\s+(.*)$")
    private val DIVIDER = Regex("^[-*_]{3,}$")

    fun parseBlocks(text: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotBlank()) {
                blocks.add(Block.Paragraph(parseSpans(paragraph.toString().trim())))
            }
            paragraph.clear()
        }
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> flush()
                HEADING.matchEntire(line) != null -> {
                    flush()
                    val m = HEADING.matchEntire(line)
                    blocks.add(Block.Heading(m!!.groupValues[1].length, parseSpans(m.groupValues[2])))
                }
                DIVIDER.matchEntire(line) != null -> {
                    flush()
                    blocks.add(Block.Divider())
                }
                LIST.matchEntire(line) != null -> {
                    flush()
                    val m = LIST.matchEntire(line)!!
                    val inner = m.groupValues[1]
                    val cm = CHECK.matchEntire(inner)
                    if (cm != null) {
                        blocks.add(Block.ListItem(parseSpans(cm.groupValues[2]), true, cm.groupValues[1].lowercase() == "x"))
                    } else {
                        blocks.add(Block.ListItem(parseSpans(inner), false, false))
                    }
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append('\n')
                    paragraph.append(line)
                }
            }
        }
        flush()
        return blocks
    }

    fun parseSpans(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var i = 0
        val buf = StringBuilder()
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '`') {
                val close = text.indexOf('`', i + 1)
                if (close > i) {
                    flushBuf(buf, spans)
                    spans.add(Span.Code(text.substring(i + 1, close)))
                    i = close + 1
                    continue
                }
            }
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val close = text.indexOf("**", i + 2)
                if (close > i + 1) {
                    flushBuf(buf, spans)
                    spans.add(Span.Bold(text.substring(i + 2, close)))
                    i = close + 2
                    continue
                }
            }
            if (text[i] == '*') {
                val close = text.indexOf('*', i + 1)
                if (close > i) {
                    flushBuf(buf, spans)
                    spans.add(Span.Italic(text.substring(i + 1, close)))
                    i = close + 1
                    continue
                }
            }
            buf.append(text[i])
            i++
        }
        flushBuf(buf, spans)
        return spans
    }

    private fun flushBuf(buf: StringBuilder, spans: MutableList<Span>) {
        if (buf.isNotEmpty()) {
            spans.add(Span.Text(buf.toString()))
            buf.clear()
        }
    }
}
