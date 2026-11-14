import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.android.room)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "io.wiger.timeup"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.wiger.timeup"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("wiger_dev.jks")
            val pStorePassword = System.getenv("KEYSTORE_PASSWORD")
            val pKeyAlias = System.getenv("KEY_ALIAS")
            val pKeyPassword = System.getenv("KEY_PASSWORD")

            if (keystoreFile.exists() && !pStorePassword.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = pStorePassword
                keyAlias = pKeyAlias
                keyPassword = pKeyPassword
            } else {
                // 如果在 CI 环境下缺失配置，打印警告（或者你可以改为抛出异常）
                println("Warning: Release signing configuration is incomplete. APK will be unsigned.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

}
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {

    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
}