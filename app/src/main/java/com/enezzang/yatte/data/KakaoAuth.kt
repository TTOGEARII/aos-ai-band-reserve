package com.enezzang.yatte.data

import java.net.URLDecoder
import java.net.URLEncoder

/*
 * 카카오 로그인 — 브라우저 인가 → 딥링크 복귀 방식.
 *
 * 앱 → 브라우저(kauth 인가) → 카카오가 브리지 페이지([KAKAO_REDIRECT_URI])로 리다이렉트
 * → 브리지가 yatte://kakao?code=... 로 앱 복귀 → 앱이 코드를 서버(POST /auth/kakao)로 보내 JWT 수령.
 *
 * 이 구조인 이유: 백엔드가 SDK 토큰이 아니라 **인가 코드 + redirectUri**를 받는 REST 플로우라서
 * (auth.service.kakaoLogin이 코드→토큰 교환을 직접 한다), 카카오 SDK 없이 서버 무수정으로 붙는다.
 * ⚠️ redirectUri는 카카오 개발자 콘솔(카카오 로그인 > Redirect URI)에 **정확히 등록돼 있어야** 하고,
 * 인가 요청과 토큰 교환에 한 글자도 다르지 않게 같은 값이 쓰여야 한다.
 */

/** 카카오가 인가 코드를 전달할 브리지 페이지(subculture-ground-front public 정적 파일). */
const val KAKAO_REDIRECT_URI = "https://nuxt.kanenashitogeari.com/yatte-kakao-bridge.html"

/** 브리지가 앱으로 복귀할 때 쓰는 딥링크. AndroidManifest의 intent-filter와 같아야 한다. */
const val KAKAO_DEEPLINK_SCHEME = "yatte"
const val KAKAO_DEEPLINK_HOST = "kakao"

/** 웹과 같은 동의 항목. 이메일은 카카오 쪽 설정이 있을 때만 내려온다(서버가 알아서 처리). */
private const val KAKAO_SCOPE = "profile_nickname,profile_image"

fun kakaoAuthorizeUrl(restKey: String): String {
    val redirect = URLEncoder.encode(KAKAO_REDIRECT_URI, "UTF-8")
    val scope = URLEncoder.encode(KAKAO_SCOPE, "UTF-8")
    return "https://kauth.kakao.com/oauth/authorize" +
        "?client_id=$restKey&redirect_uri=$redirect&response_type=code&scope=$scope"
}

sealed interface KakaoCallback {
    data class Code(val code: String) : KakaoCallback

    /** 사용자가 취소했거나 카카오가 error를 보냄. reason이 null이면 코드 자체가 없는 이상 호출. */
    data class Failed(val reason: String?) : KakaoCallback
}

/**
 * yatte://kakao 딥링크의 쿼리에서 인가 코드를 꺼낸다.
 * android.net.Uri에 묶지 않고 쿼리 문자열만 받는 순수 함수 — [KakaoAuthTest]가 지킨다.
 */
fun parseKakaoCallback(query: String?): KakaoCallback {
    val params = (query ?: "")
        .split('&')
        .mapNotNull { pair ->
            if (pair.isEmpty()) return@mapNotNull null
            val idx = pair.indexOf('=')
            if (idx < 0) return@mapNotNull pair to ""
            pair.substring(0, idx) to runCatching {
                URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }.getOrDefault("")
        }
        .toMap()

    val code = params["code"]
    if (!code.isNullOrBlank()) return KakaoCallback.Code(code)

    val error = params["error"]
    return if (error != null) {
        KakaoCallback.Failed(
            params["error_description"]?.takeIf { it.isNotBlank() }
                ?: "카카오 인증이 취소됐어요.",
        )
    } else {
        KakaoCallback.Failed(null)
    }
}
