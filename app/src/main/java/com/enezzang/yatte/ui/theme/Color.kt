package com.enezzang.yatte.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 합주실 앱 팔레트. subculture-ground 관리 화면의 보라(#7c3aed) 계열을 뿌리로 하되,
 * 본문 글씨가 올라가는 자리는 한 단계 어두운 700(#6d28d9)을 기본으로 쓴다 —
 * 흰 글씨 대비가 원색은 4.2:1로 AA(4.5:1)에 아슬아슬하고 700은 6.2:1로 여유 있다.
 * 색을 바꾸려면 여기만 고친다. 화면 파일에 hex를 직접 쓰지 않는다.
 */

// — 라이트 지면 —
val Bg = Color(0xFFFBFAF8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDim = Color(0xFFF1EEF8)
val Ink = Color(0xFF1C1B22)
val MutedOnLight = Color(0xFF5E5A6B) // Bg 대비 5.9:1 — 보조 문구도 AA를 지킨다
val HairlineLight = Color(0xFFE5E1EF)

// — 보라 램프 —
val Violet100 = Color(0xFFF3EFFE)
val Violet200 = Color(0xFFEDE9FE)
val Violet300 = Color(0xFFC4B5FD)
val Violet400 = Color(0xFFA78BFA)
val Violet600 = Color(0xFF7C3AED)
val Violet700 = Color(0xFF6D28D9)
val Violet800 = Color(0xFF5B21B6)
val Violet900 = Color(0xFF4C1D95)
val Violet950 = Color(0xFF2E1065)

// — 다크 지면 —
val BgDark = Color(0xFF141218)
val SurfaceDark = Color(0xFF1E1B26)
val SurfaceDimDark = Color(0xFF262230)
val InkDark = Color(0xFFECEAF2)
val MutedOnDark = Color(0xFFA7A2B5)
val HairlineDark = Color(0xFF363040)

// — 상태 —
val ErrorText = Color(0xFFB3261E)
val ErrorTextDark = Color(0xFFF2B8B5)
val ErrorContainer = Color(0xFFF9DEDC)
val ErrorContainerDark = Color(0xFF4A2522)
val OkGreen = Color(0xFF1B7F4B)
val OkGreenDark = Color(0xFF7ED9A7)
