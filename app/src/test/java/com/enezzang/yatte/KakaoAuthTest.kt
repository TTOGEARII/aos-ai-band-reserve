package com.enezzang.yatte

import com.enezzang.yatte.data.KAKAO_REDIRECT_URI
import com.enezzang.yatte.data.KakaoCallback
import com.enezzang.yatte.data.kakaoAuthorizeUrl
import com.enezzang.yatte.data.parseKakaoCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 카카오 인가 URL과 복귀 딥링크 파싱. 여기가 틀리면 로그인 버튼이 엉뚱한 곳으로 가거나
 * 돌아온 코드를 못 읽어 로그인이 조용히 실패한다.
 */
class KakaoAuthTest {

    @Test
    fun `인가 URL - 필수 파라미터와 인코딩된 리다이렉트`() {
        val url = kakaoAuthorizeUrl("test-rest-key")
        assertTrue(url.startsWith("https://kauth.kakao.com/oauth/authorize?"))
        assertTrue(url.contains("client_id=test-rest-key"))
        assertTrue(url.contains("response_type=code"))
        // redirect_uri는 반드시 URL 인코딩 — 안 하면 카카오가 등록된 URI와 다르다고 거부한다
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fnuxt.kanenashitogeari.com%2Fyatte-kakao-bridge.html"))
        assertTrue(url.contains("scope=profile_nickname%2Cprofile_image"))
    }

    @Test
    fun `리다이렉트 상수는 브리지 페이지를 가리킨다`() {
        // 카카오 콘솔 등록값·브리지 파일명과 삼자일치해야 하는 값이라 상수 자체를 못 박는다
        assertEquals("https://nuxt.kanenashitogeari.com/yatte-kakao-bridge.html", KAKAO_REDIRECT_URI)
    }

    @Test
    fun `딥링크 파싱 - 정상 코드`() {
        assertEquals(
            KakaoCallback.Code("abc123XYZ"),
            parseKakaoCallback("code=abc123XYZ&state="),
        )
    }

    @Test
    fun `딥링크 파싱 - 사용자가 취소하면 에러 설명을 전달`() {
        val failed = parseKakaoCallback(
            "error=access_denied&error_description=User%20denied%20access",
        ) as KakaoCallback.Failed
        assertEquals("User denied access", failed.reason)
    }

    @Test
    fun `딥링크 파싱 - 설명 없는 에러는 기본 안내 문구`() {
        val failed = parseKakaoCallback("error=access_denied") as KakaoCallback.Failed
        assertEquals("카카오 인증이 취소됐어요.", failed.reason)
    }

    @Test
    fun `딥링크 파싱 - 코드도 에러도 없으면 reason null`() {
        assertEquals(KakaoCallback.Failed(null), parseKakaoCallback(null))
        assertEquals(KakaoCallback.Failed(null), parseKakaoCallback(""))
        assertEquals(KakaoCallback.Failed(null), parseKakaoCallback("foo=bar"))
    }
}
