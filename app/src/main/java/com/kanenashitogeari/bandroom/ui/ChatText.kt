package com.kanenashitogeari.bandroom.ui

/**
 * 에이전트 답변(마크다운 섞인 평문)을 화면 블록으로 푸는 순수 함수들.
 * 웹(index.vue의 INLINE_RE/YT_RE)과 같은 문법만 다룬다: **굵게**, [라벨](url), 맨몸 URL,
 * 줄 단위의 제목(#)과 글머리(- 또는 별표).
 *
 * Compose에 안 묶여 있어 [ChatTextTest]가 파싱 규칙을 못 박는다 —
 * 여기가 틀리면 사용자가 예약 링크를 못 누른다.
 */

sealed interface InlineSpan {
    data class Plain(val text: String) : InlineSpan
    data class Bold(val text: String) : InlineSpan
    data class Link(val text: String, val url: String) : InlineSpan
}

sealed interface ReplyBlock {
    data class Heading(val spans: List<InlineSpan>) : ReplyBlock
    data class Bullet(val spans: List<InlineSpan>) : ReplyBlock
    data class Paragraph(val spans: List<InlineSpan>) : ReplyBlock
}

/** 답변 아래 버튼으로 띄울 링크. 같은 URL은 한 번만. */
data class LinkChip(val label: String, val url: String, val kind: LinkKind)

enum class LinkKind { NaverBooking, YouTube, Other }

/**
 * 웹과 같은 순서의 인라인 문법: 굵게 → 마크다운 링크 → 맨몸 URL.
 * URL 끝의 구두점(.,!?)은 링크에 넣지 않는다 — "링크입니다." 의 마침표가 주소에 붙으면 404가 된다.
 */
private val INLINE_RE = Regex(
    """\*\*([^*]+)\*\*|\[([^\]]+)]\(([^)]+)\)|(https?://[^\s<>()]+[^\s<>().,!?])""",
)

private val YOUTUBE_RE = Regex(
    """(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/)([A-Za-z0-9_-]{11})""",
)

fun parseInline(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    var cursor = 0
    for (match in INLINE_RE.findAll(text)) {
        if (match.range.first > cursor) {
            spans += InlineSpan.Plain(text.substring(cursor, match.range.first))
        }
        val (bold, mdLabel, mdUrl, bareUrl) = match.destructured
        spans += when {
            bold.isNotEmpty() -> InlineSpan.Bold(bold)
            mdLabel.isNotEmpty() -> InlineSpan.Link(mdLabel, mdUrl)
            else -> InlineSpan.Link(bareUrl, bareUrl)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) spans += InlineSpan.Plain(text.substring(cursor))
    return spans
}

/**
 * 줄 단위 블록 파싱. 빈 줄은 건너뛴다(블록 간격은 렌더러가 준다).
 * 번호 목록(1. …)은 그대로 문단으로 둔다 — 번호가 글에 남아 있어 읽는 데 지장이 없다.
 */
fun parseReply(text: String): List<ReplyBlock> =
    text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> null
            trimmed.startsWith("#") -> {
                val content = trimmed.trimStart('#').trim()
                if (content.isEmpty()) null else ReplyBlock.Heading(parseInline(content))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                ReplyBlock.Bullet(parseInline(trimmed.substring(2).trim()))

            else -> ReplyBlock.Paragraph(parseInline(trimmed))
        }
    }

/**
 * 본문에 나온 링크를 종류별 라벨을 단 버튼 목록으로. 네이버 예약 링크가 이 앱의 목적지라
 * 눌러야 할 것이 본문 틈이 아니라 **버튼으로** 보이게 한다.
 */
fun extractLinkChips(text: String): List<LinkChip> {
    val chips = mutableListOf<LinkChip>()
    for (span in parseInline(text)) {
        if (span !is InlineSpan.Link) continue
        if (chips.any { it.url == span.url }) continue
        chips += when {
            span.url.contains("booking.naver.com") ->
                LinkChip("네이버 예약 열기", span.url, LinkKind.NaverBooking)

            YOUTUBE_RE.containsMatchIn(span.url) ->
                LinkChip("YouTube에서 보기", span.url, LinkKind.YouTube)

            else -> {
                val label = span.text.takeIf { it != span.url && it.isNotBlank() }
                    ?: span.url.removePrefix("https://").removePrefix("http://").take(40)
                LinkChip(label, span.url, LinkKind.Other)
            }
        }
    }
    return chips
}
