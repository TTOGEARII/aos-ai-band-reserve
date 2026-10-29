package com.enezzang.yatte.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enezzang.yatte.data.AGENT_MODELS
import com.enezzang.yatte.data.ApiResult
import com.enezzang.yatte.data.AuthSession
import com.enezzang.yatte.data.EnezzangYatteRepository
import com.enezzang.yatte.data.ChatStore
import com.enezzang.yatte.data.ChatTurnBody
import com.enezzang.yatte.data.CredentialStatus
import com.enezzang.yatte.data.CredentialsPatch
import com.enezzang.yatte.data.MAX_MESSAGE_LENGTH
import com.enezzang.yatte.data.SessionStore
import com.enezzang.yatte.data.StoredMessage
import com.enezzang.yatte.data.ToolCallTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Role { User, Assistant }

/** 화면용 채팅 한 턴. 저장 모델([StoredMessage])과는 role 표현만 다르다. */
data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCallTrace> = emptyList(),
)

private fun StoredMessage.toChatMessage() = ChatMessage(
    role = if (role == "user") Role.User else Role.Assistant,
    content = content,
    toolCalls = toolCalls,
)

private fun ChatMessage.toStored() = StoredMessage(
    role = if (role == Role.User) "user" else "assistant",
    content = content,
    toolCalls = toolCalls,
)

data class EnezzangYatteUiState(
    val isBooting: Boolean = true,
    val session: AuthSession? = null,
    val showSettings: Boolean = false,

    // 인증
    val isAuthenticating: Boolean = false,
    /** 로그인 화면 안에서만 쓰는 오류(검증 실패, 비밀번호 틀림, 세션 만료 안내). */
    val authError: String? = null,

    // API 키 상태
    val credentialStatus: CredentialStatus? = null,
    val isSavingCredentials: Boolean = false,
    /** 설정 화면 안에서만 쓰는 오류. */
    val settingsError: String? = null,

    // 채팅
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val model: String = AGENT_MODELS.first().id,
    /** 채팅 상단 배너에 떠 있는 오류. 재시도(마지막 질문 다시 보내기)와 닫기를 제공한다. */
    val chatError: String? = null,
    /** 스낵바로 한 번 보여주고 지우는 안내. */
    val message: String? = null,
) {
    /**
     * Gemini 키가 없으면 에이전트가 아예 안 돈다. 상태를 **아직 모를 때(null)는 막지 않는다** —
     * 상태 조회가 느리거나 실패했다고 채팅까지 잠그면 더 불편하다(서버가 어차피 400으로 알려준다).
     */
    val needsGeminiKey: Boolean
        get() = credentialStatus?.hasGeminiKey == false

    /** 마지막 턴이 답을 못 받은 질문인가 — 재시도 버튼의 노출 근거. */
    val canRetry: Boolean
        get() = !isSending && messages.lastOrNull()?.role == Role.User
}

@HiltViewModel
class EnezzangYatteViewModel @Inject constructor(
    private val repository: EnezzangYatteRepository,
    private val sessionStore: SessionStore,
    private val chatStore: ChatStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnezzangYatteUiState())
    val uiState: StateFlow<EnezzangYatteUiState> = _uiState.asStateFlow()

    private var sendJob: Job? = null

    init {
        boot()
    }

    /** 저장해 둔 세션·대화·모델을 되살린다. 세션이 있으면 키 상태를 바로 확인한다(401이면 여기서 로그아웃). */
    private fun boot() {
        viewModelScope.launch {
            val saved = sessionStore.session.first()
            val storedMessages = chatStore.messages.first().map { it.toChatMessage() }
            val storedModel = chatStore.model.first()
                ?.takeIf { id -> AGENT_MODELS.any { it.id == id } }

            _uiState.update {
                it.copy(
                    isBooting = false,
                    session = saved,
                    messages = storedMessages,
                    model = storedModel ?: AGENT_MODELS.first().id,
                )
            }
            if (saved != null) refreshStatus()
        }
    }

    // ─── 인증 ───

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@")) {
            _uiState.update { it.copy(authError = "올바른 이메일 형식이 아닙니다.") }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(authError = "비밀번호를 입력해주세요.") }
            return
        }
        authenticate("로그인") { repository.login(trimmedEmail, password) }
    }

    fun register(email: String, password: String, passwordConfirm: String, name: String) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (!trimmedEmail.contains("@")) {
            _uiState.update { it.copy(authError = "올바른 이메일 형식이 아닙니다.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(authError = "비밀번호는 6자 이상이어야 합니다.") }
            return
        }
        // 비밀번호는 가려진 채 입력되므로 오타를 서버가 아니라 여기서 잡는다 — 재입력 확인.
        if (password != passwordConfirm) {
            _uiState.update { it.copy(authError = "비밀번호가 서로 달라요. 같은 값을 두 번 입력해 주세요.") }
            return
        }
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(authError = "이름을 입력해주세요.") }
            return
        }
        authenticate("회원가입") { repository.register(trimmedEmail, password, trimmedName) }
    }

    /** 카카오 딥링크(yatte://kakao)로 돌아온 인가 코드 처리. 이미 로그인돼 있으면 무시한다. */
    fun kakaoLogin(code: String) {
        if (_uiState.value.session != null) return
        authenticate("카카오 로그인") { repository.kakaoLogin(code) }
    }

    /** 카카오 인가 취소/실패 딥링크. 로그인 화면의 오류 자리에 안내만 띄운다. */
    fun kakaoFailed(reason: String?) {
        if (_uiState.value.session != null) return
        _uiState.update {
            it.copy(authError = reason ?: "카카오 인증 응답이 올바르지 않아요. 다시 시도해 주세요.")
        }
    }

    private fun authenticate(
        label: String,
        call: suspend () -> ApiResult<com.enezzang.yatte.data.AuthResponse>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }

            when (val result = call()) {
                is ApiResult.Success -> {
                    val session = AuthSession(
                        accessToken = result.data.accessToken,
                        email = result.data.user.email,
                        name = result.data.user.name,
                    )
                    sessionStore.save(session)
                    _uiState.update {
                        it.copy(session = session, isAuthenticating = false, authError = null)
                    }
                    refreshStatus()
                }

                is ApiResult.Failure -> _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = result.message.ifBlank { "${label}에 실패했어요." },
                    )
                }
            }
        }
    }

    fun signOut() {
        sendJob?.cancel()
        viewModelScope.launch {
            sessionStore.clear()
            chatStore.clearMessages()
            _uiState.update {
                EnezzangYatteUiState(
                    isBooting = false,
                    model = it.model, // 모델 선택은 계정과 무관한 기기 설정이라 남긴다
                )
            }
        }
    }

    /** 아무 호출에서나 401이 오면 토큰이 만료된 것. 지우고 로그인으로 보낸다. */
    private suspend fun sessionExpired() {
        sendJob?.cancel()
        sessionStore.clear()
        _uiState.update {
            it.copy(
                session = null,
                showSettings = false,
                isSending = false,
                isAuthenticating = false,
                authError = "로그인이 만료됐어요. 다시 로그인해 주세요.",
            )
        }
    }

    // ─── API 키 ───

    fun refreshStatus() {
        val token = _uiState.value.session?.accessToken ?: return
        viewModelScope.launch {
            when (val result = repository.credentialStatus(token)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(credentialStatus = result.data, settingsError = null)
                }

                is ApiResult.Unauthorized -> sessionExpired()

                // 상태를 모른다고 채팅을 잠그지는 않는다. 설정 화면에서만 알린다.
                is ApiResult.Failure -> _uiState.update {
                    it.copy(settingsError = result.message)
                }
            }
        }
    }

    /** null = 바꾸지 않음, 빈 문자열 = 지우기. 보낸 필드만 서버가 갱신한다. */
    fun saveCredentials(notionToken: String?, geminiKey: String?, youtubeKey: String?) {
        val token = _uiState.value.session?.accessToken ?: return
        if (notionToken == null && geminiKey == null && youtubeKey == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCredentials = true, settingsError = null) }

            val patch = CredentialsPatch(notionToken, geminiKey, youtubeKey)
            when (val result = repository.saveCredentials(token, patch)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isSavingCredentials = false, message = "저장했어요.")
                    }
                    refreshStatus()
                }

                is ApiResult.Unauthorized -> sessionExpired()

                is ApiResult.Failure -> _uiState.update {
                    it.copy(isSavingCredentials = false, settingsError = result.message)
                }
            }
        }
    }

    fun deleteCredentials() {
        val token = _uiState.value.session?.accessToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCredentials = true, settingsError = null) }

            when (val result = repository.deleteCredentials(token)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingCredentials = false,
                            credentialStatus = CredentialStatus(),
                            message = "모든 키를 삭제했어요.",
                        )
                    }
                }

                is ApiResult.Unauthorized -> sessionExpired()

                is ApiResult.Failure -> _uiState.update {
                    it.copy(isSavingCredentials = false, settingsError = result.message)
                }
            }
        }
    }

    // ─── 채팅 ───

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return
        if (trimmed.length > MAX_MESSAGE_LENGTH) {
            _uiState.update { it.copy(message = "메시지는 ${MAX_MESSAGE_LENGTH}자 이하여야 해요.") }
            return
        }
        // 답을 못 받은 질문이 쌓여 있어도 새 질문을 막지 않는다 — 이전 턴은 그냥 기록으로 남는다.
        val messages = _uiState.value.messages + ChatMessage(Role.User, trimmed)
        _uiState.update { it.copy(messages = messages, chatError = null) }
        persistMessages(messages)
        dispatchChat()
    }

    /** 실패/취소한 마지막 질문을 다시 보낸다. 질문을 중복 추가하지 않는다. */
    fun retryLast() {
        if (!_uiState.value.canRetry) return
        _uiState.update { it.copy(chatError = null) }
        dispatchChat()
    }

    /**
     * 마지막 user 턴을 서버로 보낸다. history는 그 이전까지 —
     * 백엔드가 message(새 질문)와 history(이전 대화)를 분리해서 받는다.
     */
    private fun dispatchChat() {
        val state = _uiState.value
        val token = state.session?.accessToken ?: return
        val last = state.messages.lastOrNull()?.takeIf { it.role == Role.User } ?: return

        sendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, chatError = null) }

            val history = state.messages.dropLast(1).map {
                ChatTurnBody(if (it.role == Role.User) "user" else "assistant", it.content)
            }

            when (val result = repository.chat(token, history, last.content, state.model)) {
                is ApiResult.Success -> {
                    val messages = _uiState.value.messages +
                        ChatMessage(Role.Assistant, result.data.reply, result.data.toolCalls)
                    _uiState.update { it.copy(messages = messages, isSending = false) }
                    persistMessages(messages)
                }

                is ApiResult.Unauthorized -> sessionExpired()

                is ApiResult.Failure -> _uiState.update {
                    it.copy(isSending = false, chatError = result.message)
                }
            }
        }
    }

    /** 검색 도중 그만두기. 연결이 끊기면 서버도 에이전트 루프를 멈춰 Gemini 호출 낭비가 없다. */
    fun cancelSend() {
        if (!_uiState.value.isSending) return
        sendJob?.cancel()
        sendJob = null
        _uiState.update {
            it.copy(isSending = false, chatError = "요청을 취소했어요. 재시도를 누르면 다시 물어봐요.")
        }
    }

    fun newChat() {
        if (_uiState.value.isSending) cancelSend()
        viewModelScope.launch {
            chatStore.clearMessages()
            _uiState.update { it.copy(messages = emptyList(), chatError = null) }
        }
    }

    fun selectModel(id: String) {
        if (AGENT_MODELS.none { it.id == id }) return
        _uiState.update { it.copy(model = id) }
        viewModelScope.launch { chatStore.saveModel(id) }
    }

    private fun persistMessages(messages: List<ChatMessage>) {
        viewModelScope.launch { chatStore.saveMessages(messages.map { it.toStored() }) }
    }

    // ─── 화면 전환·일회성 메시지 ───

    fun openSettings() = _uiState.update { it.copy(showSettings = true, settingsError = null) }

    fun closeSettings() = _uiState.update { it.copy(showSettings = false) }

    fun dismissChatError() = _uiState.update { it.copy(chatError = null) }

    fun dismissAuthError() = _uiState.update { it.copy(authError = null) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
