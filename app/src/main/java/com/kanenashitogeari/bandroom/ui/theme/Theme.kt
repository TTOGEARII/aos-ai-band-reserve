package com.kanenashitogeari.bandroom.ui.theme

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
    primary = Violet700,
    onPrimary = Color.White,
    primaryContainer = Violet200,
    onPrimaryContainer = Violet900,
    secondary = Violet800,
    onSecondary = Color.White,
    background = Bg,
    onBackground = Ink,
    surface = Bg,
    onSurface = Ink,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = MutedOnLight,
    // 드롭다운·다이얼로그가 색을 가져가는 계열. 비워 두면 M3가 primary에서 파생한
    // 보라끼 도는 회색을 깔아 지면이 탁해진다.
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
    primary = Violet300,
    onPrimary = Violet950,
    primaryContainer = Violet900,
    onPrimaryContainer = Violet200,
    secondary = Violet400,
    onSecondary = Violet950,
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
data class BandRoomTokens(
    val hairline: Color,
    val muted: Color,
    /** 내(사용자) 말풍선 — 액센트 지면. */
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

private val LightTokens = BandRoomTokens(
    hairline = HairlineLight,
    muted = MutedOnLight,
    bubbleUser = Violet700,
    onBubbleUser = Color.White,
    bubbleAssistant = SurfaceLight,
    onBubbleAssistant = Ink,
    traceOk = OkGreen,
    traceFail = ErrorText,
    inputBg = SurfaceLight,
)

private val DarkTokens = BandRoomTokens(
    hairline = HairlineDark,
    muted = MutedOnDark,
    bubbleUser = Violet800,
    onBubbleUser = Violet100,
    bubbleAssistant = SurfaceDark,
    onBubbleAssistant = InkDark,
    traceOk = OkGreenDark,
    traceFail = ErrorTextDark,
    inputBg = SurfaceDark,
)

private val LocalBandRoom = staticCompositionLocalOf { LightTokens }

object BandRoomTheme {
    val tokens: BandRoomTokens
        @Composable @ReadOnlyComposable get() = LocalBandRoom.current

    /** 화면 좌우 여백. */
    val gutter = 16.dp
}

/**
 * 다이내믹 컬러는 쓰지 않는다 — 기기 배경화면에서 색을 뽑으면
 * 보라 단일 액센트 위에 세운 말풍선 대비가 무너진다.
 */
@Composable
fun Band_roomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBandRoom provides if (darkTheme) DarkTokens else LightTokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content,
        )
    }
}
