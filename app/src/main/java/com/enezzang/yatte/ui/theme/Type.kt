package com.enezzang.yatte.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 채팅이 주인공인 앱이라 커스텀 서체 없이 시스템 서체(Roboto/한글 폴백)를 쓴다.
 * water_fee가 Archivo를 쓴 건 큰 금액 숫자 때문이었고, 여기엔 그런 자리가 없다.
 * 본문 16sp 하한 — 대화 글이 작으면 앱 전체가 피곤해진다.
 */
val Typography = Typography(
    // 로그인·빈 채팅 히어로의 큰 제목.
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    // 화면 상단 타이틀.
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    // 섹션 제목(설정 화면 등).
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    // 채팅 본문.
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // 보조 문구·설명.
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 버튼.
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    // 칩·도구 트레이스 같은 작은 라벨.
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
