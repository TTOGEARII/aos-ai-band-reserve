package com.enezzang.yatte.data

import com.enezzang.yatte.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 백엔드 ALLOWED_MODELS와 일치. 첫 항목이 기본값 — 허용 밖 값은 서버가 기본값으로 폴백한다. */
data class AgentModel(val id: String, val label: String)

val AGENT_MODELS = listOf(
    AgentModel("gemini-3-flash-preview", "Gemini 3 Flash (최신)"),
    AgentModel("gemini-2.5-flash", "Gemini 2.5 Flash (안정)"),
)

/** 서버 ChatRequestDto의 @MaxLength와 같다. 입력칸에서도 이 값으로 막는다. */
const val MAX_MESSAGE_LENGTH = 4000

object EnezzangYatteNetwork {

    /**
     * `explicitNulls = false`가 핵심이다. 자격증명 부분 수정([CredentialsPatch])에서
     * null 필드가 바디에서 빠져야 "보낸 것만 갱신"이 성립한다(서버는 undefined=유지로 본다).
     * `ignoreUnknownKeys`는 서버가 필드를 추가해도 앱이 깨지지 않게 하는 안전장치.
     */
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun createApi(): EnezzangYatteApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            /*
             * 읽기 타임아웃이 5분인 이유: 채팅은 에이전트가 노션·네이버 예약을 도구로
             * 여러 번 부르는 동안 응답이 열려 있다(웹도 300초를 준다). 상태 조회·저장 같은
             * 빠른 호출이 5분을 기다리는 일은 서버가 받아놓고 응답을 안 줄 때뿐이라 드물고,
             * 그걸 위해 클라이언트를 둘로 쪼개는 건 과하다.
             */
            .readTimeout(300, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EnezzangYatteApi::class.java)
    }
}

@Singleton
class EnezzangYatteRepository @Inject constructor(private val api: EnezzangYatteApi) {

    private val encryptionKey = BuildConfig.ENCRYPTION_KEY

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> =
        safeCall("로그인에 실패했어요.") {
            api.login(encryptedBody(LoginRequest(email, password)))
        }.decrypted()

    suspend fun register(email: String, password: String, name: String): ApiResult<AuthResponse> =
        safeCall("회원가입에 실패했어요.") {
            api.register(encryptedBody(RegisterRequest(email, password, name)))
        }.decrypted()

    /** 브라우저 인가로 받은 코드를 JWT로 교환. redirectUri는 인가 요청과 같은 값([KAKAO_REDIRECT_URI]). */
    suspend fun kakaoLogin(code: String): ApiResult<AuthResponse> =
        safeCall("카카오 로그인에 실패했어요.") {
            api.kakao(encryptedBody(KakaoLoginRequest(code, KAKAO_REDIRECT_URI)))
        }.decrypted()

    suspend fun credentialStatus(token: String): ApiResult<CredentialStatus> =
        safeCall("설정 상태를 불러오지 못했어요.") { api.credentialStatus(bearer(token)) }

    /** 보낸 필드만 갱신된다. null = 유지, 빈 문자열 = 지우기. */
    suspend fun saveCredentials(token: String, patch: CredentialsPatch): ApiResult<Unit> =
        safeUnitCall("저장에 실패했어요.") {
            api.saveCredentials(bearer(token), encryptedBody(patch))
        }

    suspend fun deleteCredentials(token: String): ApiResult<Unit> =
        safeUnitCall("삭제에 실패했어요.") { api.deleteCredentials(bearer(token)) }

    /**
     * 에이전트 호출. 도구 루프 동안 수십 초~수 분 걸릴 수 있다.
     * 취소는 호출한 코루틴 Job 취소로 한다 — OkHttp 연결이 끊기면 서버도 에이전트 루프를 멈춘다.
     */
    suspend fun chat(
        token: String,
        history: List<ChatTurnBody>,
        message: String,
        model: String,
    ): ApiResult<ChatResponse> =
        safeCall("응답을 받지 못했어요.") {
            api.chat(bearer(token), ChatRequest(message, history, model))
        }

    private fun bearer(token: String) = "Bearer $token"

    private inline fun <reified T> encryptedBody(payload: T): EncryptedBody =
        EncryptedBody(Crypto.encrypt(EnezzangYatteNetwork.json.encodeToString(payload), encryptionKey))

    /**
     * 암호화 응답({encrypted})을 풀어 원하는 타입으로. 여기서 깨지면 십중팔구
     * 앱과 서버의 ENCRYPTION_KEY가 달라 복호화 결과가 쓰레기값인 경우라, 그걸 문구로 알린다.
     */
    private inline fun <reified T> ApiResult<EncryptedBody>.decrypted(): ApiResult<T> =
        when (this) {
            is ApiResult.Success -> try {
                ApiResult.Success(
                    EnezzangYatteNetwork.json.decodeFromString<T>(
                        Crypto.decrypt(data.encrypted, encryptionKey),
                    ),
                )
            } catch (e: Exception) {
                ApiResult.NetworkError("응답을 해석하지 못했어요. 앱의 암호화 키가 서버와 같은지 확인해 주세요.")
            }

            is ApiResult.Failure -> this
        }

    /**
     * 모든 호출이 지나는 경계. 상태코드를 [ApiResult]로 좁히고,
     * 서버가 준 한국어 message가 있으면 그걸 우선 쓴다(웹과 문구를 맞추기 위해).
     */
    private suspend fun <T> safeCall(
        fallbackMessage: String,
        block: suspend () -> Response<T>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return@withContext ApiResult.ServerError(
                        response.code(),
                        "서버가 빈 응답을 보냈어요.",
                    )
                return@withContext ApiResult.Success(body)
            }
            failureOf(response, fallbackMessage)
        } catch (e: CancellationException) {
            throw e // 사용자가 취소한 것 — 삼키면 코루틴 취소가 전파되지 않는다.
        } catch (e: IOException) {
            ApiResult.NetworkError("인터넷 연결을 확인해 주세요.")
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message?.takeIf { it.isNotBlank() } ?: fallbackMessage)
        }
    }

    /** 204 No Content 응답용 — 바디를 요구하지 않는 것만 [safeCall]과 다르다. */
    private suspend fun safeUnitCall(
        fallbackMessage: String,
        block: suspend () -> Response<Unit>,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) return@withContext ApiResult.Success(Unit)
            failureOf(response, fallbackMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ApiResult.NetworkError("인터넷 연결을 확인해 주세요.")
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message?.takeIf { it.isNotBlank() } ?: fallbackMessage)
        }
    }

    private fun failureOf(response: Response<*>, fallbackMessage: String): ApiResult.Failure {
        val serverMessage = response.errorBody()?.let { errorBody ->
            runCatching {
                EnezzangYatteNetwork.json.decodeFromString<ApiErrorBody>(errorBody.string()).text
            }.getOrNull()
        }

        return when (response.code()) {
            401 -> ApiResult.Unauthorized(serverMessage ?: "로그인이 필요해요.")
            400 -> ApiResult.BadRequest(serverMessage ?: fallbackMessage)
            409 -> ApiResult.Conflict(serverMessage ?: "이미 사용 중인 이메일입니다.")
            else -> ApiResult.ServerError(response.code(), serverMessage ?: fallbackMessage)
        }
    }
}
