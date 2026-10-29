package com.enezzang.yatte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enezzang.yatte.data.AGENT_MODELS
import com.enezzang.yatte.data.AuthSession
import com.enezzang.yatte.data.CredentialStatus
import com.enezzang.yatte.ui.theme.EnezzangYatteTheme
import java.net.URLEncoder

/**
 * 부적절한 AI 응답 신고 메일 주소. Google Play 생성형 AI 정책이 인앱 신고 장치를 요구한다 —
 * 이 앱 규모에선 메일 신고가 그 장치다. 주소를 바꾸면 개인정보처리방침의 연락처와 같이 바꿀 것.
 */
private const val REPORT_EMAIL = "cagameku3842@gmail.com"

/**
 * 설정: 계정 · AI 모델 · API 키.
 * 키 값은 서버가 되돌려주지 않는다(암호화 저장) — 그래서 입력칸은 항상 비어 있고,
 * 저장 여부만 상태 줄로 보여준다. 입력한 필드만 서버로 보낸다(null=유지).
 */
@Composable
fun SettingsScreen(
    session: AuthSession,
    status: CredentialStatus?,
    model: String,
    isSaving: Boolean,
    errorMessage: String?,
    onSelectModel: (String) -> Unit,
    onSaveCredentials: (notion: String?, gemini: String?, youtube: String?) -> Unit,
    onDeleteCredentials: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var notionInput by rememberSaveable { mutableStateOf("") }
    var geminiInput by rememberSaveable { mutableStateOf("") }
    var youtubeInput by rememberSaveable { mutableStateOf("") }
    var confirmClear by rememberSaveable { mutableStateOf<String?>(null) } // "notion"|"gemini"|"youtube"
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    val tokens = EnezzangYatteTheme.tokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnezzangYatteTheme.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // ─── 계정 ───
        SectionTitle("계정")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (session.name.isNotBlank()) {
                    Text(session.name, style = MaterialTheme.typography.bodyLarge)
                }
                Text(session.email, style = MaterialTheme.typography.bodyMedium, color = tokens.muted)
            }
            TextButton(onClick = { confirmSignOut = true }) {
                Text("로그아웃", style = MaterialTheme.typography.labelLarge)
            }
        }
        Hairline()

        // ─── AI 모델 ───
        SectionTitle("AI 모델")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AGENT_MODELS.forEachIndexed { index, agentModel ->
                SegmentedButton(
                    selected = model == agentModel.id,
                    onClick = { onSelectModel(agentModel.id) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AGENT_MODELS.size),
                ) {
                    Text(agentModel.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        Hairline()

        // ─── API 키 ───
        SectionTitle("API 키")
        Text(
            text = "키는 서버에 암호화되어 저장되고 다시 보여주지 않아요. 바꾸려면 새 값을 입력하고 저장하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
        )
        if (status == null) {
            Text("상태 확인 중…", style = MaterialTheme.typography.bodyMedium, color = tokens.muted)
        }

        KeyField(
            label = "Gemini API 키",
            required = true,
            isSet = status?.hasGeminiKey == true,
            statusDetail = null,
            help = "필수 — aistudio.google.com에서 무료 발급",
            value = geminiInput,
            onValueChange = { geminiInput = it },
            enabled = !isSaving,
            onClear = { confirmClear = "gemini" },
        )
        KeyField(
            label = "노션 Integration 토큰",
            required = false,
            isSet = status?.hasNotionToken == true,
            statusDetail = status?.notionWorkspace?.let { "연결됨 · $it" },
            help = "선택 — 합주 일정을 노션 캘린더에 기록할 때",
            value = notionInput,
            onValueChange = { notionInput = it },
            enabled = !isSaving,
            onClear = { confirmClear = "notion" },
        )
        KeyField(
            label = "YouTube API 키",
            required = false,
            isSet = status?.hasYoutubeKey == true,
            statusDetail = null,
            help = "선택 — 곡·커버 영상 검색용",
            value = youtubeInput,
            onValueChange = { youtubeInput = it },
            enabled = !isSaving,
            onClear = { confirmClear = "youtube" },
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }

        PrimaryButton(
            text = if (isSaving) "저장 중…" else "입력한 키 저장",
            onClick = {
                // 입력한 필드만 보낸다. 실패해도 재입력 부담이 적은 값(붙여넣기)이라 칸은 바로 비운다.
                onSaveCredentials(
                    notionInput.trim().ifEmpty { null },
                    geminiInput.trim().ifEmpty { null },
                    youtubeInput.trim().ifEmpty { null },
                )
                notionInput = ""
                geminiInput = ""
                youtubeInput = ""
            },
            enabled = listOf(notionInput, geminiInput, youtubeInput).any { it.isNotBlank() },
            loading = isSaving,
        )

        Hairline()

        // ─── 지원 ───
        SectionTitle("지원")
        val uriHandler = LocalUriHandler.current
        TextButton(onClick = {
            // mailto는 공백을 %20으로 받아야 해서 URLEncoder의 '+'를 바꿔 준다.
            val subject = URLEncoder.encode("[합주실 예약 AI] 부적절한 응답 신고", "UTF-8")
                .replace("+", "%20")
            uriHandler.openUri("mailto:$REPORT_EMAIL?subject=$subject")
        }) {
            Text("부적절한 AI 응답 신고", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = "에이전트의 답변이 부적절하거나 유해하다면 알려주세요. 메일 앱이 열려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
        )

        Hairline()
        TextButton(onClick = { confirmDeleteAll = true }, enabled = !isSaving) {
            Text(
                "저장된 키 모두 삭제",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    // ─── 확인 다이얼로그 ───
    confirmClear?.let { field ->
        val label = when (field) {
            "gemini" -> "Gemini API 키"
            "notion" -> "노션 토큰"
            else -> "YouTube API 키"
        }
        AlertDialog(
            onDismissRequest = { confirmClear = null },
            title = { Text("$label 지우기") },
            text = { Text("서버에 저장된 ${label}를 지워요. 관련 기능은 키를 다시 넣을 때까지 쓸 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = null
                    // 빈 문자열 = 그 키만 지우기 (서버 계약)
                    onSaveCredentials(
                        if (field == "notion") "" else null,
                        if (field == "gemini") "" else null,
                        if (field == "youtube") "" else null,
                    )
                }) { Text("지우기", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = null }) { Text("취소") }
            },
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("저장된 키 모두 삭제") },
            text = { Text("Gemini·노션·YouTube 키를 전부 지워요. 에이전트를 다시 쓰려면 Gemini 키부터 다시 넣어야 해요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteCredentials()
                }) { Text("모두 삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("취소") }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("로그아웃") },
            text = { Text("이 기기의 대화 기록도 함께 지워져요. 서버에 저장한 키는 그대로 남아요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("취소") }
            },
        )
    }
}

/** 키 한 개 분량: 상태 줄(저장됨/미설정) + 새 값 입력칸 + (저장돼 있으면) 지우기. */
@Composable
private fun KeyField(
    label: String,
    required: Boolean,
    isSet: Boolean,
    statusDetail: String?,
    help: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onClear: () -> Unit,
) {
    val tokens = EnezzangYatteTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = statusDetail ?: if (isSet) "저장됨" else if (required) "미설정 — 필수" else "미설정",
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isSet -> tokens.traceOk
                    required -> MaterialTheme.colorScheme.error
                    else -> tokens.muted
                },
                modifier = Modifier.weight(1f),
            )
            if (isSet) {
                TextButton(onClick = onClear, enabled = enabled) {
                    Text("지우기", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        LabeledField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = if (isSet) "바꾸려면 새 키 입력" else "키 붙여넣기",
            supporting = help,
            enabled = enabled,
        )
    }
}
