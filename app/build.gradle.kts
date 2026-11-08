plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fws.howtolivebetter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fws.howtolivebetter"
        minSdk = 26
        targetSdk = 36          // Google Play: new apps/updates must target API 36 since 31/08/2026
        versionCode = 1
        versionName = "1.0.0"

        // ⚠️ GOOGLE TEST IDs. Thay bằng ID AdMob thật của bạn trước khi phát hành.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")      // Quảng cáo mở ứng dụng (ID thử nghiệm)
        // URL chính sách quyền riêng tư (GitHub Pages, thư mục docs/)
        buildConfigField("String", "PRIVACY_URL", "\"https://lequang0996.github.io/how-to-live-better-android/privacy-policy.html\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin { jvmToolchain(17) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core") // chỉ bộ icon lõi (nhẹ); icon khác nằm trong AppIcons.kt
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // In-App Update (Flexible)
    implementation("com.google.android.play:app-update:2.1.0")
    // Google AdMob + User Messaging Platform (đồng ý GDPR/CCPA)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
