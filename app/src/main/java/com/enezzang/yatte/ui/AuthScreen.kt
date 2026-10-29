package com.enezzang.yatte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enezzang.yatte.ui.theme.EnezzangYatteTheme

/**
 * 로그인/회원가입. 계정은 subculture-ground 전 프로젝트 공용이라
 * 웹에서 쓰던 이메일·비밀번호가 그대로 들어온다.
 */
@Composable
fun AuthScreen(
    isAuthenticating: Boolean,
    errorMessage: String?,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 화면 회전에도 입력이 날아가지 않게 rememberSaveable.
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val submit = {
        keyboard?.hide()
        if (isRegister) onRegister(email, password, name) else onLogin(email, password)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EnezzangYatteTheme.gutter),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text("합주실 예약 AI", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "말로 물어보면 서울 합주실의 빈 시간을 찾아\n예약 링크까지 바로 받아요.",
            style = MaterialTheme.typography.bodyLarge,
            color = EnezzangYatteTheme.tokens.muted,
        )

        Spacer(Modifier.height(4.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isRegister,
                onClick = { isRegister = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("로그인", style = MaterialTheme.typography.labelLarge) }
            SegmentedButton(
                selected = isRegister,
                onClick = { isRegister = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("회원가입", style = MaterialTheme.typography.labelLarge) }
        }

        LabeledField(
            value = email,
            onValueChange = { email = it },
            label = "이메일",
            placeholder = "you@example.com",
            enabled = !isAuthenticating,
            keyboardType = KeyboardType.Email,
        )

        LabeledField(
            value = password,
            onValueChange = { password = it },
            label = "비밀번호",
            supporting = if (isRegister) "6자 이상" else null,
            isPassword = true,
            enabled = !isAuthenticating,
            imeAction = if (isRegister) ImeAction.Next else ImeAction.Done,
            onDone = { if (!isRegister) submit() },
        )

        if (isRegister) {
            LabeledField(
                value = name,
                onValueChange = { name = it },
                label = "이름",
                placeholder = "밴드에서 부르는 이름이면 돼요",
                enabled = !isAuthenticating,
                imeAction = ImeAction.Done,
                onDone = submit,
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }

        PrimaryButton(
            text = when {
                isAuthenticating -> if (isRegister) "가입 중…" else "로그인 중…"
                isRegister -> "가입하고 시작하기"
                else -> "로그인"
            },
            onClick = submit,
            loading = isAuthenticating,
        )

        Text(
            text = "웹(subculture-ground)과 같은 계정을 써요. 이미 가입했다면 그대로 로그인하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = EnezzangYatteTheme.tokens.muted,
        )
    }
}
