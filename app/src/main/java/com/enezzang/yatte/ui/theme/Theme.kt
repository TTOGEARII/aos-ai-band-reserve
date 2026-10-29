package com.enezzang.yatte.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Cyan800,
    onPrimary = Color.White,
    primaryContainer = Cyan100,
    onPrimaryContainer = Cyan900,
    secondary = Cyan900,
    onSecondary = Color.White,
    // 세그먼트 선택·FilledTonalButton이 이 슬롯을 쓴다. 안 주면 M3 기본 연보라가 샌다.
    secondaryContainer = Cyan200,
    onSecondaryContainer = Cyan900,
    background = Bg,
    onBackground = Ink,
    surface = Bg,
    onSurface = Ink,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = MutedOnLight,
    // 드롭다운·다이얼로그가 색을 가져가는 계열. 비워 두면 M3가 primary에서 파생한
    // 푸른끼 도는 회색을 제멋대로 깔아 지면이 탁해진다.
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceDim,
    surfaceContainerHighest = SurfaceDim,
    surfaceBright = Bg,
    surfaceDim = SurfaceDim,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = ErrorText,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Cyan400,
    onPrimary = Navy950,
    primaryContainer = Cyan900,
    onPrimaryContainer = Cyan100,
    secondary = Cyan300,
    onSecondary = Navy950,
    secondaryContainer = Color(0xFF1B4A66),
    onSecondaryContainer = Cyan100,
    background = BgDark,
    onBackground = InkDark,
    surface = BgDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceDimDark,
    onSurfaceVariant = MutedOnDark,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDimDark,
    surfaceContainerHighest = SurfaceDimDark,
    surfaceBright = SurfaceDimDark,
    surfaceDim = BgDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = ErrorTextDark,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * M3 스킴에 슬롯이 없는 이 앱 고유 값들. 화면 파일이 hex를 직접 들고 있지 않도록 여기로 모은다.
 * 말풍선 색이 이 앱 얼굴이라 색만이 아니라 역할로 토큰을 둔다.
 */
data class EnezzangYatteTokens(
    val hairline: Color,
    val muted: Color,
    /** 내(사용자) 말풍선 — 에네 블루 지면. */
    val bubbleUser: Color,
    val onBubbleUser: Color,
    /** 에이전트 말풍선 — 카드 지면. */
    val bubbleAssistant: Color,
    val onBubbleAssistant: Color,
    /** 도구 트레이스 성공/실패 표시. */
    val traceOk: Color,
    val traceFail: Color,
    /** 입력바 지면. */
    val inputBg: Color,
)

private val LightTokens = EnezzangYatteTokens(
    hairline = HairlineLight,
    muted = MutedOnLight,
    bubbleUser = Cyan800,
    onBubbleUser = Color.White,
    bubbleAssistant = SurfaceLight,
    onBubbleAssistant = Ink,
    traceOk = OkGreen,
    traceFail = ErrorText,
    inputBg = SurfaceLight,
)

private val DarkTokens = EnezzangYatteTokens(
    hairline = HairlineDark,
    muted = MutedOnDark,
    bubbleUser = BubbleUserDark,
    onBubbleUser = OnBubbleUserDark,
    bubbleAssistant = SurfaceDark,
    onBubbleAssistant = InkDark,
    traceOk = OkGreenDark,
    traceFail = ErrorTextDark,
    inputBg = SurfaceDark,
)

private val LocalEnezzangYatte = staticCompositionLocalOf { LightTokens }

object EnezzangYatteTheme {
    val tokens: EnezzangYatteTokens
        @Composable @ReadOnlyComposable get() = LocalEnezzangYatte.current

    /** 화면 좌우 여백. */
    val gutter = 16.dp
}

/**
 * 다이내믹 컬러는 쓰지 않는다 — 기기 배경화면에서 색을 뽑으면
 * 에네 시안 단일 액센트 위에 세운 말풍선 대비가 무너진다.
 */
@Composable
fun Enezzang_yatteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalEnezzangYatte provides if (darkTheme) DarkTokens else LightTokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content,
        )
    }
}
