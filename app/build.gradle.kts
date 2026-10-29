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
 *   1. 환경변수 BAND_ROOM_BASE_URL   (CI·일회성 빌드)
 *   2. local.properties의 bandRoom.baseUrl
 * 둘 다 없으면 에뮬레이터용 로컬 주소. release는 아래 packageRelease 가드에서 막는다.
 */
val explicitBaseUrl: String? =
    providers.environmentVariable("BAND_ROOM_BASE_URL").orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("bandRoom.baseUrl")?.takeIf { it.isNotBlank() }

val baseUrl: String = explicitBaseUrl ?: "http://10.0.2.2:3001"

/**
 * API 본문 암호화 키 — 서버의 ENCRYPTION_KEY와 반드시 같아야 한다.
 * 웹도 NUXT_PUBLIC_*로 클라이언트에 실어 보내는 값이라 비밀은 아니지만,
 * 서버마다 다를 수 있어 주소와 같은 경로(env > local.properties)로 받는다.
 * 기본값은 서버가 ENCRYPTION_KEY 없이 떴을 때의 기본 키.
 */
val encryptionKey: String =
    providers.environmentVariable("BAND_ROOM_ENCRYPTION_KEY").orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty("bandRoom.encryptionKey")?.takeIf { it.isNotBlank() }
        ?: "subculture-ground-encryption-key-2024"

android {
    namespace = "com.kanenashitogeari.bandroom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kanenashitogeari.bandroom"
        // 26인 이유: 어댑티브 아이콘만으로 런처 아이콘이 끝나고(레거시 mipmap PNG 불필요),
        // desugaring 없이 java.time·java.util.Base64를 쓸 수 있다. 2026년 기준 26 미만 기기는 무시해도 되는 수준.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "ENCRYPTION_KEY", "\"$encryptionKey\"")
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
              BAND_ROOM_BASE_URL=https://... ./gradlew :app:assembleRelease
              또는 local.properties 에  bandRoom.baseUrl=https://...
            (운영 서버가 커스텀 ENCRYPTION_KEY를 쓰면 bandRoom.encryptionKey도 함께.)
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
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
