package com.kanenashitogeari.bandroom

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 의존성 조립은 Hilt가 한다 — 각 클래스의 @Inject 생성자와 di/NetworkModule 참고.
 * Application이 직접 들고 있던 저장소/리포지토리 프로퍼티는 전부 제거했다.
 */
@HiltAndroidApp
class BandRoomApplication : Application()
