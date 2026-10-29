package com.enezzang.yatte

import com.enezzang.yatte.ui.InlineSpan
import com.enezzang.yatte.ui.LinkKind
import com.enezzang.yatte.ui.ReplyBlock
import com.enezzang.yatte.ui.extractLinkChips
import com.enezzang.yatte.ui.parseInline
import com.enezzang.yatte.ui.parseReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 에이전트 답변 파싱 규칙. 틀리면 사용자가 예약 링크를 못 누르거나 깨진 마크다운을 본다.
 */
class ChatTextTest {

    @Test
    fun `굵게-마크다운 링크-맨몸 URL을 가른다`() {
        val spans = parseInline("**그라운드 합정** A룸은 [여기](https://a.com)서, 자세한 건 https://b.com 참고")
        assertEquals(
            listOf(
                InlineSpan.Bold("그라운드 합정"),
                InlineSpan.Plain(" A룸은 "),
                InlineSpan.Link("여기", "https://a.com"),
                InlineSpan.Plain("서, 자세한 건 "),
                InlineSpan.Link("https://b.com", "https://b.com"),
                InlineSpan.Plain(" 참고"),
            ),
            spans,
        )
    }

    @Test
    fun `URL 끝의 구두점은 링크에 넣지 않는다`() {
        // "…주소." 의 마침표가 주소에 붙으면 404가 된다
        val spans = parseInline("예약은 https://m.booking.naver.com/booking/10/bizes/123.")
        val link = spans.filterIsInstance<InlineSpan.Link>().single()
        assertEquals("https://m.booking.naver.com/booking/10/bizes/123", link.url)
    }

    @Test
    fun `제목-글머리-문단 블록을 가르고 빈 줄은 버린다`() {
        val blocks = parseReply("## 가능한 룸\n\n- A룸 20:00\n* B룸 21:00\n일반 문단")
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is ReplyBlock.Heading)
        assertTrue(blocks[1] is ReplyBlock.Bullet)
        assertTrue(blocks[2] is ReplyBlock.Bullet)
        assertTrue(blocks[3] is ReplyBlock.Paragraph)
        assertEquals(listOf(InlineSpan.Plain("A룸 20:00")), (blocks[1] as ReplyBlock.Bullet).spans)
    }

    @Test
    fun `네이버 예약 링크는 전용 라벨 버튼이 된다`() {
        val url = "https://m.booking.naver.com/booking/10/bizes/482561/items/4598037?startDate=2026-11-01"
        val chips = extractLinkChips("A룸: $url 에서 예약하세요")
        assertEquals(1, chips.size)
        assertEquals(LinkKind.NaverBooking, chips[0].kind)
        assertEquals("네이버 예약 열기", chips[0].label)
        assertEquals(url, chips[0].url)
    }

    @Test
    fun `유튜브 링크는 유튜브 버튼, 같은 URL은 한 번만`() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val chips = extractLinkChips("영상: $url 그리고 다시 $url")
        assertEquals(1, chips.size)
        assertEquals(LinkKind.YouTube, chips[0].kind)
        assertEquals("YouTube에서 보기", chips[0].label)
    }

    @Test
    fun `일반 링크는 마크다운 라벨을 버튼 이름으로 쓴다`() {
        val chips = extractLinkChips("[노션 페이지](https://notion.so/abc) 를 만들었어요")
        assertEquals(LinkKind.Other, chips[0].kind)
        assertEquals("노션 페이지", chips[0].label)
    }
}
