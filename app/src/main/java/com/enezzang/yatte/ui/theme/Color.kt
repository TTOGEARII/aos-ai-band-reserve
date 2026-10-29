package com.enezzang.yatte.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 에네(Ene) 콘셉트 팔레트 — 밝은 시안(하늘빛 머리칼)과 짙은 네이비(화면 속 디지털 세계).
 * 캐릭터 연상은 **색과 분위기로만** 한다(아트·명칭 사용 금지 — 스토어 IP 정책).
 *
 * 다크가 이 앱의 본진이다("화면 안에 사는" 콘셉트) — 라이트도 지원하되 쿨톤으로 맞춘다.
 * 본문 글씨가 올라가는 자리는 대비를 재서 고른다: 라이트 primary는 원색 시안이 아니라
 * 한 단계 어두운 #0277BD(흰 글씨 대비 5.0:1, AA 통과)를 쓴다.
 * 색을 바꾸려면 여기만 고친다. 화면 파일에 hex를 직접 쓰지 않는다.
 */

// — 라이트 지면 (쿨톤) —
val Bg = Color(0xFFF4F9FD)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDim = Color(0xFFE2EEF8)
val Ink = Color(0xFF0F1B28)
val MutedOnLight = Color(0xFF4A6378) // Bg 대비 ≈5.8:1 — 보조 문구도 AA
val HairlineLight = Color(0xFFD3E4F1)

// — 에네 시안 램프 —
val Cyan100 = Color(0xFFE1F5FE)
val Cyan200 = Color(0xFFB7E7FC)
val Cyan300 = Color(0xFF7FD4F9)
val Cyan400 = Color(0xFF53D1FF) // 에네 하이라이트 — 다크 primary
val Cyan600 = Color(0xFF0A9BDD)
val Cyan700 = Color(0xFF0288D1)
val Cyan800 = Color(0xFF0277BD) // 라이트 primary (흰 글씨 AA)
val Cyan900 = Color(0xFF015C96)
val Navy950 = Color(0xFF002B44) // 밝은 시안 위에 올리는 글씨

// — 다크 지면 (디지털 네이비) —
val BgDark = Color(0xFF0A141F)
val SurfaceDark = Color(0xFF101F2E)
val SurfaceDimDark = Color(0xFF17293C)
val InkDark = Color(0xFFE4F2FB)
val MutedOnDark = Color(0xFF8FB0C6)
val HairlineDark = Color(0xFF234258)

// — 말풍선 전용 —
/** 다크에서 내 말풍선 — 네이비 속에서 빛나는 시안 블루 카드. */
val BubbleUserDark = Color(0xFF0E5B88)
val OnBubbleUserDark = Color(0xFFDFF4FB)

// — 상태 —
val ErrorText = Color(0xFFB3261E)
val ErrorTextDark = Color(0xFFF2B8B5)
val ErrorContainer = Color(0xFFF9DEDC)
val ErrorContainerDark = Color(0xFF4A2522)
val OkGreen = Color(0xFF1B7F4B)
val OkGreenDark = Color(0xFF7ED9A7)
