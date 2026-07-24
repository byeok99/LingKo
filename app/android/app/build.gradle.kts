plugins {
    id("com.android.application")
    id("kotlin-android")
    // Flutter 플러그인은 Android와 Kotlin 확장을 사용하므로 두 플러그인 다음에 적용한다.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.lingko_app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // Android 설치 단위를 식별하는 값이며 출시 전 실제 LingKo 패키지로 교체해야 한다.
        applicationId = "com.example.lingko_app"
        // 사용하는 인증·보안 저장 플러그인의 지원 범위에 맞춰 최소 SDK를 23으로 제한한다.
        minSdk = 23
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
