package com.enezzang.yatte

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.enezzang.yatte.data.KAKAO_DEEPLINK_HOST
import com.enezzang.yatte.data.KAKAO_DEEPLINK_SCHEME
import com.enezzang.yatte.data.KakaoCallback
import com.enezzang.yatte.data.parseKakaoCallback
import com.enezzang.yatte.ui.EnezzangYatteScreen
import com.enezzang.yatte.ui.EnezzangYatteViewModel
import com.enezzang.yatte.ui.theme.Enezzang_yatteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /*
     * 액티비티 스코프 ViewModel — 컴포저블의 hiltViewModel()과 같은 인스턴스다.
     * 카카오 딥링크(yatte://kakao)가 브라우저에서 돌아올 때 인가 코드를 여기로 넘긴다.
     * (매니페스트 launchMode=singleTask라 복귀는 onNewIntent로 들어온다.)
     */
    private val viewModel: EnezzangYatteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Enezzang_yatteTheme {
                EnezzangYatteScreen()
            }
        }
        handleKakaoDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleKakaoDeepLink(intent)
    }

    private fun handleKakaoDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != KAKAO_DEEPLINK_SCHEME || uri.host != KAKAO_DEEPLINK_HOST) return

        when (val callback = parseKakaoCallback(uri.query)) {
            is KakaoCallback.Code -> viewModel.kakaoLogin(callback.code)
            is KakaoCallback.Failed -> viewModel.kakaoFailed(callback.reason)
        }
    }
}
