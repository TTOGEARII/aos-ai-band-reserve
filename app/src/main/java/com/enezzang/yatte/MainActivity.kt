package com.enezzang.yatte

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.enezzang.yatte.data.KAKAO_DEEPLINK_HOST
import com.enezzang.yatte.data.KAKAO_DEEPLINK_SCHEME
import com.enezzang.yatte.data.KakaoCallback
import com.enezzang.yatte.data.parseKakaoCallback
import com.enezzang.yatte.ui.EnezzangYatteScreen
import com.enezzang.yatte.ui.EnezzangYatteViewModel
import com.enezzang.yatte.ui.theme.Enezzang_yatteTheme
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /*
     * 액티비티 스코프 ViewModel — 컴포저블의 hiltViewModel()과 같은 인스턴스다.
     * 카카오 딥링크(yatte://kakao)가 브라우저에서 돌아올 때 인가 코드를 여기로 넘긴다.
     * (매니페스트 launchMode=singleTask라 복귀는 onNewIntent로 들어온다.)
     */
    private val viewModel: EnezzangYatteViewModel by viewModels()

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    /*
     * 업데이트 화면에서 돌아왔을 때. 결과를 보지 않는다 — 사용자가 도중에 그만두면
     * 그냥 이번 버전으로 쓰게 둔다. 다음에 앱을 다시 켤 때 또 묻는다.
     */
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Enezzang_yatteTheme {
                EnezzangYatteScreen()
            }
        }
        handleKakaoDeepLink(intent)

        // 새 버전이 있으면 켜는 김에 받는다. 화면 회전 같은 재생성 때는 묻지 않는다.
        if (savedInstanceState == null) startUpdateIfNeeded(resumeOnly = false)
    }

    override fun onResume() {
        super.onResume()
        /*
         * 업데이트를 받다가 앱을 나갔다 오면 Play가 반쯤 설치된 채로 남는다. 그때만 이어서 한다.
         * 여기서 새 업데이트까지 묻지 않는 건, 사용자가 한 번 그만둔 뒤에도 카카오 인증 등으로
         * 홈·브라우저를 다녀올 때마다 같은 창이 다시 뜨면 흐름이 끊기기 때문이다.
         */
        startUpdateIfNeeded(resumeOnly = true)
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

    /**
     * 새 버전이 있으면 Play의 전체 화면 업데이트를 띄운다.
     *
     * 즉시(immediate) 방식이라 Play가 받고 설치하고 앱을 다시 열어 준다. 서버 계약(암호화 키·
     * 라우트)이 바뀌면 구버전은 로그인부터 깨지는 앱이라 버전을 빨리 수렴시키는 쪽이 낫고,
     * 잠깐 켜서 빈 시간을 묻고 마는 앱이라 몇 초 기다리는 편이 "나중에 할까요?"를
     * 판단하게 만드는 것보다 낫다. (water_fee와 같은 선택)
     */
    private fun startUpdateIfNeeded(resumeOnly: Boolean) {
        lifecycleScope.launch {
            val info = try {
                appUpdateManager.requestAppUpdateInfo()
            } catch (e: Exception) {
                /*
                 * Play로 설치되지 않았거나(installDebug, 에뮬레이터) Play 서비스가 없는 기기.
                 * 업데이트만 못 할 뿐 앱은 멀쩡히 써야 하므로 조용히 넘어간다 — 여기서 오류를
                 * 띄우면 정작 합주실을 찾으러 온 사람이 영문 모를 경고부터 보게 된다.
                 */
                return@launch
            }

            val interrupted =
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

            val shouldStart = if (resumeOnly) interrupted else interrupted || available
            if (!shouldStart) return@launch
            if (!info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) return@launch

            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        }
    }
}
