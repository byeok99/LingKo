plugins {
    id("com.android.application")
    id("kotlin-android")
    // Flutter 플러그인은 Android와 Kotlin 확장을 사용하므로 두 플러그인 다음에 적용한다.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.byeok.lingko"
    // flutter_secure_storage 10.x가 사용하는 Android API와 맞추기 위해 SDK 36으로 컴파일한다.
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    compileOptions {
        // minSdk 24에서도 plugin의 최신 Java API를 사용할 수 있도록 D8 core library desugaring을 활성화한다.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // Google OAuth와 앱 배포에서 동일한 LingKo 설치 단위를 식별하는 정식 패키지명이다.
        applicationId = "com.byeok.lingko"
        // flutter_tts의 현재 Android 구현이 사용하는 API에 맞춰 최소 SDK를 24로 제한한다.
        minSdk = 24
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // 현재는 로컬 release 실행만 가능하게 debug 서명을 사용하며 실제 배포 전 운영 키로 교체해야 한다.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}
