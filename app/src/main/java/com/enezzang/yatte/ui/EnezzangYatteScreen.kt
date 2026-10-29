package com.enezzang.yatte.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enezzang.yatte.data.AGENT_MODELS
import com.enezzang.yatte.ui.theme.EnezzangYatteTheme

/**
 * 앱의 화면 트리. 세션이 없으면 로그인, 있으면 채팅이 메인이고 설정이 그 위에 얹힌다.
 * 탭이 없는 단일 흐름 — 이 앱이 하는 일이 "에이전트와 대화" 하나이기 때문.
 */
@Composable
fun EnezzangYatteScreen(viewModel: EnezzangYatteViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmNewChat by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    // 설정 화면에서 뒤로가기 = 채팅으로.
    BackHandler(enabled = state.showSettings) { viewModel.closeSettings() }

    val session = state.session

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        /*
         * 헤더와 오류 배너는 붙박이다. 채팅을 아래로 내려 본 상태에서 전송이 실패해도
         * 배너가 화면 밖에 있으면 아무 일도 안 일어난 것처럼 보이기 때문.
         */
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                when {
                    session == null -> {} // 로그인 화면은 자체 타이틀이 있다

                    state.showSettings -> SettingsHeader(onBack = viewModel::closeSettings)

                    else -> ChatHeader(
                        modelLabel = AGENT_MODELS.firstOrNull { it.id == state.model }?.label ?: state.model,
                        hasMessages = state.messages.isNotEmpty(),
                        onNewChat = { confirmNewChat = true },
                        onOpenSettings = viewModel::openSettings,
                    )
                }
                if (session != null) Hairline()

                if (session != null && !state.showSettings) {
                    state.chatError?.let { message ->
                        ErrorBanner(
                            message = message,
                            onDismiss = viewModel::dismissChatError,
                            onRetry = if (state.canRetry) viewModel::retryLast else null,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // 키보드가 올라오면 본문 높이를 줄인다 — 채팅 입력바가 키보드 위로 따라온다.
                .imePadding(),
        ) {
            when {
                state.isBooting -> LoadingBlock()

                session == null -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    AuthScreen(
                        isAuthenticating = state.isAuthenticating,
                        errorMessage = state.authError,
                        onLogin = viewModel::login,
                        onRegister = viewModel::register,
                    )
                }

                state.showSettings -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    SettingsScreen(
                        session = session,
                        status = state.credentialStatus,
                        model = state.model,
                        isSaving = state.isSavingCredentials,
                        errorMessage = state.settingsError,
                        onSelectModel = viewModel::selectModel,
                        onSaveCredentials = viewModel::saveCredentials,
                        onDeleteCredentials = viewModel::deleteCredentials,
                        onSignOut = viewModel::signOut,
                    )
                }

                else -> ChatScreen(
                    messages = state.messages,
                    isSending = state.isSending,
                    needsGeminiKey = state.needsGeminiKey,
                    onSend = viewModel::sendMessage,
                    onCancel = viewModel::cancelSend,
                    onOpenSettings = viewModel::openSettings,
                )
            }
        }
    }

    if (confirmNewChat) {
        AlertDialog(
            onDismissRequest = { confirmNewChat = false },
            title = { Text("새 대화") },
            text = { Text("지금까지의 대화를 지우고 새로 시작해요. 에이전트는 이전 대화를 더 이상 기억하지 않아요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewChat = false
                    viewModel.newChat()
                }) { Text("새로 시작") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewChat = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun ChatHeader(
    modelLabel: String,
    hasMessages: Boolean,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = EnezzangYatteTheme.gutter, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("합주실 예약 AI", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = modelLabel,
                style = MaterialTheme.typography.labelMedium,
                color = EnezzangYatteTheme.tokens.muted,
            )
        }
        if (hasMessages) {
            TextButton(onClick = onNewChat) {
                Text("새 대화", style = MaterialTheme.typography.labelLarge)
            }
        }
        TextButton(onClick = onOpenSettings) {
            Text("설정", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = EnezzangYatteTheme.gutter, top = 10.dp, bottom = 10.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("← 채팅", style = MaterialTheme.typography.labelLarge)
        }
        Text("설정", style = MaterialTheme.typography.headlineMedium)
    }
}
