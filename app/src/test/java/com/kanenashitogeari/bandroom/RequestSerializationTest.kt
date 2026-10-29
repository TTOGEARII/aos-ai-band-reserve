package com.kanenashitogeari.bandroom

import com.kanenashitogeari.bandroom.data.AuthResponse
import com.kanenashitogeari.bandroom.data.BandRoomNetwork
import com.kanenashitogeari.bandroom.data.ChatRequest
import com.kanenashitogeari.bandroom.data.ChatResponse
import com.kanenashitogeari.bandroom.data.ChatTurnBody
import com.kanenashitogeari.bandroom.data.CredentialsPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버 계약을 요청/응답 직렬화 수준에서 못 박는다.
 * - 자격증명 부분 수정: **안 보낸 필드 = 유지, 빈 문자열 = 지우기.**
 *   null 필드가 바디에서 빠지는 건 explicitNulls=false 덕분이고, 이걸 끄면
 *   "Gemini 키만 저장"이 노션 토큰까지 null로 덮어써 지워 버린다.
 * - 서버가 필드를 더 내려줘도(ignoreUnknownKeys) 앱이 깨지지 않는다.
 */
class RequestSerializationTest {

    private val json = BandRoomNetwork.json

    @Test
    fun `자격증명 패치 - 입력한 필드만 바디에 실린다`() {
        val body = json.encodeToString(CredentialsPatch(geminiKey = "AIza-new"))
        assertEquals("""{"geminiKey":"AIza-new"}""", body)
        assertFalse(body.contains("notionToken"))
        assertFalse(body.contains("youtubeKey"))
    }

    @Test
    fun `자격증명 패치 - 빈 문자열은 지우기라서 바디에 남아야 한다`() {
        val body = json.encodeToString(CredentialsPatch(notionToken = ""))
        assertEquals("""{"notionToken":""}""", body)
    }

    @Test
    fun `채팅 요청 - role 표기와 필드 구성`() {
        val body = json.encodeToString(
            ChatRequest(
                message = "홍대 합주실 찾아줘",
                history = listOf(
                    ChatTurnBody("user", "안녕"),
                    ChatTurnBody("assistant", "안녕하세요!"),
                ),
                model = "gemini-3-flash-preview",
            ),
        )
        assertEquals(
            """{"message":"홍대 합주실 찾아줘","history":[{"role":"user","content":"안녕"},""" +
                """{"role":"assistant","content":"안녕하세요!"}],"model":"gemini-3-flash-preview"}""",
            body,
        )
    }

    @Test
    fun `채팅 응답 - 선언 안 한 toolCalls input 필드가 있어도 깨지지 않는다`() {
        val response = json.decodeFromString<ChatResponse>(
            """{"reply":"A룸이 비어 있어요","toolCalls":[""" +
                """{"tool":"get_available_slots","input":{"studio":"그라운드","date":"2026-11-01"},"ok":true}]}""",
        )
        assertEquals("A룸이 비어 있어요", response.reply)
        assertEquals(1, response.toolCalls.size)
        assertEquals("get_available_slots", response.toolCalls[0].tool)
        assertTrue(response.toolCalls[0].ok)
    }

    @Test
    fun `인증 응답 - 서버가 더 주는 user 필드는 무시된다`() {
        val response = json.decodeFromString<AuthResponse>(
            """{"accessToken":"jwt.token.here","user":{"idx":7,"email":"a@b.c","name":"드러머",""" +
                """"phone":null,"birthDate":null,"status":1,"emailVerifiedAt":null}}""",
        )
        assertEquals("jwt.token.here", response.accessToken)
        assertEquals(7, response.user.idx)
        assertEquals("드러머", response.user.name)
    }
}
