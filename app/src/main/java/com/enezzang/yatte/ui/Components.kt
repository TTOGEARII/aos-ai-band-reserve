package com.enezzang.yatte.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.enezzang.yatte.ui.theme.EnezzangYatteTheme

/** 목록·섹션 사이의 1px 선. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(EnezzangYatteTheme.tokens.hairline),
    )
}

/**
 * 주요 동작 버튼. 터치 타깃 56dp — 리허설룸에서 폰을 대충 쥐고도 눌리는 크기.
 * loading이면 스피너로 바뀌고 비활성화된다(이중 제출 방지).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.height(22.dp).padding(end = 2.dp),
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * 라벨 붙은 입력칸. label을 그대로 접근성 이름으로 써서 TalkBack이 "편집 상자"라고만 읽는 일을 막는다.
 */
@Composable
fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supporting: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = placeholder?.let {
            { Text(it, style = MaterialTheme.typography.bodyLarge, color = EnezzangYatteTheme.tokens.muted) }
        },
        supportingText = supporting?.let {
            { Text(it, style = MaterialTheme.typography.bodyMedium) }
        },
        singleLine = true,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = EnezzangYatteTheme.tokens.hairline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
    )
}

/**
 * 화면 위에 고정해 쓰는 오류 배너. 스크롤 아래에서 실패하면 "아무 일도 안 일어난 것처럼"
 * 보이는 걸 막는다. 재시도가 가능한 문맥이면 [onRetry]를 준다.
 */
@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "재시도",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = EnezzangYatteTheme.gutter, vertical = 10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(retryLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
            TextButton(onClick = onDismiss) {
                Text("닫기", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** 부팅·전환 중 로딩. 화면을 통째로 비우는 대신 중앙 스피너 하나. */
@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** 설정 화면 등의 섹션 제목. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}
