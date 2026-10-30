import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 서명 정보는 keystore.properties에만 둔다(버전관리 제외). 파일이 없으면 release 서명 없이 빌드된다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * 백엔드 주소는 저장소에 박아두지 않는다 (water_fee와 같은 방식). 우선순위:
 *   1. 환경변수 ENEZZANG_YATTE_BASE_URL   (CI·일회성 빌드)
 *   2. local.properties의 enezzangYatte.baseUrl
 * 둘 다 없으면 에뮬레이터용 로컬 주소. release는 아래 packageRelease 가드에서 막는다.
 */
val explicitBaseUrl: String? =
    providers.environmentVariable("ENEZZANG_YATTE_BASE_URL").orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("enezzangYatte.baseUrl")?.takeIf { it.isNotBlank() }

val baseUrl: String = explicitBaseUrl ?: "http://10.0.2.2:3001"

/**
 * API 본문 암호화 키 — 서버의 ENCRYPTION_KEY와 반드시 같아야 한다.
 * 웹도 NUXT_PUBLIC_*로 클라이언트에 실어 보내는 값이라 비밀은 아니지만,
 * 서버마다 다를 수 있어 주소와 같은 경로(env > local.properties)로 받는다.
 * 기본값은 서버가 ENCRYPTION_KEY 없이 떴을 때의 기본 키.
 */
val encryptionKey: String =
    providers.environmentVariable("ENEZZANG_YATTE_ENCRYPTION_KEY").orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("enezzangYatte.encryptionKey")?.takeIf { it.isNotBlank() }
        ?: "subculture-ground-encryption-key-2024"

/**
 * 카카오 로그인 REST 키(인가 URL의 client_id). 웹도 공개로 싣는 값이라 비밀은 아니지만
 * 주소처럼 저장소에 박지 않는다. 비어 있으면 앱에서 카카오 버튼이 숨는다.
 */
val kakaoRestKey: String =
    providers.environmentVariable("ENEZZANG_YATTE_KAKAO_REST_KEY").orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("enezzangYatte.kakaoRestKey")?.takeIf { it.isNotBlank() }
        ?: ""

android {
    namespace = "com.enezzang.yatte"
    compileSdk {
        version = release(37)
    }

    /*
     * 우리 코드엔 네이티브가 없지만 androidx.graphics.path와 DataStore가 .so를 들고 온다.
     * 아래 debugSymbolLevel로 기호를 뽑을 때 AGP가 NDK의 objcopy를 쓰므로 설치된 버전을 명시한다 —
     * 안 적으면 AGP 기본값을 찾다가 없으면 조용히 건너뛰고 Play에 기호가 안 올라간다. (water_fee와 동일)
     */
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.enezzang.yatte"
        // 26인 이유: 어댑티브 아이콘만으로 런처 아이콘이 끝나고(레거시 mipmap PNG 불필요),
        // desugaring 없이 java.time·java.util.Base64를 쓸 수 있다. 2026년 기준 26 미만 기기는 무시해도 되는 수준.
        minSdk = 26
        targetSdk = 37
        /*
         * 콘솔에 한 번 올라간 versionCode는 (초안이어도) 재사용 불가라 업로드마다 올려야 한다.
         * CI(깃허브 액션)는 실행 번호 기반 코드를 env로 주입하고(10 + run_number),
         * 로컬 수동 빌드는 아래 기본값을 쓴다. CI 도입 후에는 콘솔 업로드를 CI로 일원화할 것 —
         * 수동 업로드를 섞으면 코드가 엇갈려 거부된다. (1: 리네임 전, 2: 첫 업로드, 3: 기호 설정판 업로드됨)
         */
        versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 7
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "ENCRYPTION_KEY", "\"$encryptionKey\"")
        buildConfigField("String", "KAKAO_REST_KEY", "\"$kakaoRestKey\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")

            /*
             * 네이티브 디버그 기호를 번들에 같이 넣는다 — 없으면 Play Console이 경고하고,
             * 비정상 종료·ANR 스택이 주소 숫자로만 남는다. 남의 프리빌트 .so라 소스가 없으니
             * 함수 이름까지 풀어 주는 SYMBOL_TABLE이면 충분하다(FULL은 번들만 커진다).
             */
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 주소를 깜빡한 채 배포 APK/AAB가 만들어지는 걸 패키징 직전에 막는다 (water_fee 가드 + 플레이스토어용 번들 태스크까지).
tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    val baseUrlWasProvided = explicitBaseUrl != null
    doFirst {
        check(baseUrlWasProvided) {
            """
            릴리스 빌드에 백엔드 주소가 없습니다. 둘 중 하나로 주세요:
              ENEZZANG_YATTE_BASE_URL=https://... ./gradlew :app:assembleRelease
              또는 local.properties 에  enezzangYatte.baseUrl=https://...
            (운영 서버가 커스텀 ENCRYPTION_KEY를 쓰면 enezzangYatte.encryptionKey도 함께.)
            """.trimIndent()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    // 로깅 인터셉터는 코드에서 BuildConfig.DEBUG로 껐다 켜므로 릴리스에서도 컴파일 경로에 있어야 한다.
    implementation(libs.okhttp.logging.interceptor)
    // Play 인앱 업데이트. Play로 설치된 기기에서만 동작하고, 그 외에는 조용히 건너뛴다.
    implementation(libs.play.app.update.ktx)
    // 이 앱은 Fragment를 안 쓴다. Play Core가 딸려 보내는 1.1.0이 ActivityResult API와
    // 맞지 않아 lintVitalRelease가 막으므로 버전만 끌어올린다.
    implementation(libs.androidx.fragment)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
