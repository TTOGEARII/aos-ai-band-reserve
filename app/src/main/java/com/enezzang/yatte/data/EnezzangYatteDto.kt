package com.enezzang.yatte.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/*
 * 필드명은 전부 서버 계약 그대로다 (subculture-ground-back의 DTO/서비스 응답).
 * 서버가 필드를 더 내려줘도 안 깨지도록 Json은 ignoreUnknownKeys로 받는다.
 */

/** 암호화 본문 래퍼. auth 요청/응답과 자격증명 저장(PUT)이 이 모양으로 오간다. */
@Serializable
data class EncryptedBody(val encrypted: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

/** phone·birthDate는 서버에서 선택이라 앱은 안 보낸다 — 가입 문턱을 낮게 유지. */
@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String)

/** 카카오 인가 코드 교환. redirectUri는 인가 요청과 정확히 같아야 한다(서버가 토큰 교환에 씀). */
@Serializable
data class KakaoLoginRequest(val code: String, val redirectUri: String)

@Serializable
data class AuthUser(val idx: Int = 0, val email: String = "", val name: String = "")

@Serializable
data class AuthResponse(val accessToken: String, val user: AuthUser)

@Serializable
data class CredentialStatus(
    val hasNotionToken: Boolean = false,
    val hasGeminiKey: Boolean = false,
    val hasYoutubeKey: Boolean = false,
    /** 노션 토큰이 유효하면 워크스페이스명. 토큰이 없거나 확인 실패면 null. */
    val notionWorkspace: String? = null,
)

/**
 * 자격증명 부분 수정. 서버는 **바디에 없는 필드 = 유지, 빈 문자열 = 지우기**로 본다.
 * null 필드가 바디에서 빠지는 근거는 EnezzangYatteNetwork.json의 `explicitNulls = false`다.
 * [RequestSerializationTest]가 지킨다.
 */
@Serializable
data class CredentialsPatch(
    val notionToken: String? = null,
    val geminiKey: String? = null,
    val youtubeKey: String? = null,
)

@Serializable
data class ChatTurnBody(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val message: String,
    val history: List<ChatTurnBody>,
    val model: String,
)

/** 서버는 input(도구 인자)도 내려주지만 UI는 도구 이름·성패만 보여서 선언하지 않는다. */
@Serializable
data class ToolCallTrace(val tool: String, val ok: Boolean = true)

@Serializable
data class ChatResponse(
    val reply: String,
    val toolCalls: List<ToolCallTrace> = emptyList(),
)

/** 기기에 저장하는 채팅 한 턴. 웹의 localStorage("notion-agent:chat")와 같은 역할. */
@Serializable
data class StoredMessage(
    val role: String, // "user" | "assistant"
    val content: String,
    val toolCalls: List<ToolCallTrace> = emptyList(),
)

/** 로그인 결과 중 기기에 남기는 것. 권한 판단은 매 요청 서버(JWT)가 한다. */
data class AuthSession(
    val accessToken: String,
    val email: String,
    val name: String,
)

/**
 * NestJS 에러 바디. message는 문자열로도, ValidationPipe를 거치면 문자열 배열로도 온다. 둘 다 받는다.
 */
@Serializable
data class ApiErrorBody(
    val message: JsonElement? = null,
) {
    val text: String?
        get() = when (val raw = message) {
            is JsonPrimitive -> raw.contentOrNull
            is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .joinToString(" ")
                .ifBlank { null }

            else -> null
        }
}
