package com.kanenashitogeari.bandroom.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * subculture-ground 백엔드. 상태코드를 그대로 읽어야 하므로(401/400/409 분기) 전부 [Response]로 받는다.
 *
 * 암호화 규칙은 서버 컨트롤러를 따른다:
 * - auth 로그인/가입, 자격증명 저장(PUT)은 **암호화 본문**([EncryptedBody])
 * - 상태 조회(GET)·채팅(POST)은 **평문 + JWT** (채팅은 페이로드가 커서 XOR 오버헤드만 생기기 때문)
 *
 * 토큰은 인터셉터 대신 호출마다 명시한다 — 어떤 호출이 인증이 필요한지 시그니처만 보고 알 수 있게.
 */
interface BandRoomApi {

    @POST("auth/login")
    suspend fun login(@Body body: EncryptedBody): Response<EncryptedBody>

    @POST("auth/register")
    suspend fun register(@Body body: EncryptedBody): Response<EncryptedBody>

    @GET("notion-agent/credentials/status")
    suspend fun credentialStatus(
        @Header("Authorization") bearer: String,
    ): Response<CredentialStatus>

    /** 성공은 204 No Content — 바디가 없어 [Unit]으로 받는다. */
    @PUT("notion-agent/credentials")
    suspend fun saveCredentials(
        @Header("Authorization") bearer: String,
        @Body body: EncryptedBody,
    ): Response<Unit>

    @DELETE("notion-agent/credentials")
    suspend fun deleteCredentials(
        @Header("Authorization") bearer: String,
    ): Response<Unit>

    @POST("notion-agent/chat")
    suspend fun chat(
        @Header("Authorization") bearer: String,
        @Body body: ChatRequest,
    ): Response<ChatResponse>
}
