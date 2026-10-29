package com.enezzang.yatte.data

/**
 * 네트워크 호출 결과. 에러를 삼키지 않도록 실패를 타입으로 남긴다.
 *
 * water_fee와 달리 이 백엔드는 JWT를 쓰므로 **401이 실제로 온다** —
 * 로그인 화면에서는 "이메일/비밀번호 틀림"이고, 그 외에는 세션 만료라 로그아웃시킨다.
 * 어느 쪽인지는 호출한 쪽(ViewModel)이 문맥으로 가른다.
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>

    sealed interface Failure : ApiResult<Nothing> {
        /** 화면에 그대로 띄울 수 있는 한국어 문구. 서버 message가 있으면 그 값이다. */
        val message: String
    }

    /** 401 — 로그인 실패 또는 토큰 만료. */
    data class Unauthorized(override val message: String) : Failure

    /** 400 — 서버가 설명을 준 요청 오류. 예: "Gemini API 키가 설정되지 않았습니다." */
    data class BadRequest(override val message: String) : Failure

    /** 409 — 회원가입 이메일 중복. */
    data class Conflict(override val message: String) : Failure

    /** 그 외 서버 응답 오류(4xx/5xx). */
    data class ServerError(val code: Int, override val message: String) : Failure

    /** 연결 자체가 안 됨 — 오프라인/타임아웃/응답 파싱 실패. */
    data class NetworkError(override val message: String) : Failure
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}
