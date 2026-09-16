plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}



android {
    namespace = "com.oneforth.cousininthecity"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.oneforth.cousininthecity"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationIdSuffix = ".test"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // Retained to check BuildConfig.DEBUG for local network logging rules
    }
}

dependencies {
    // --- CORE ANDROID & LIFECYCLE ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // --- COMPOSE CORE FRAMEWORK ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation.layout)

    // --- MATERIAL 3 EXTENSIONS ---
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha03")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // --- NAVIGATION & TYPE-SAFE SERIALIZATION ---
    val nav_version = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // --- NETWORK ARCHITECTURE (SPRING BOOT BFF BINDINGS) ---
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    // --- IMAGE LOADING PIPELINE ---
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // --- DEPENDENCY INJECTION (HILT) ---
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // --- ASYNCHRONOUS BACKGROUND THREADS & SCHEDULERS ---
    val work_version = "2.11.1"
    implementation("androidx.work:work-runtime-ktx:$work_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$nav_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0-alpha03")

    // --- LOCAL STORAGE ENGINES ---
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")

    // --- DATA PAGINATION LAYERS ---
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // --- VIDEO PLAYBACK CAPABILITIES ---
    val media3_version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")

    // --- VAULTS, SECURITY, & PREFERENCES ---
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- IDENTITY INFRASTRUCTURE (GOOGLE CREDENTIAL MANAGER) ---
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.6.0")

    // --- TECHNICAL UTILITIES ---
    implementation("io.arrow-kt:arrow-core:2.1.2")
    implementation(libs.androidx.palette)
    implementation(libs.androidx.collection.ktx)

    //---  GSON SERIALIZATION  ---
    implementation("com.google.code.gson:gson:2.14.0")

    //Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")

    // --- TEST LAB SUITE ---
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("androidx.work:work-testing:$work_version")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")
    androidTestImplementation("androidx.work:work-testing:$work_version")
    androidTestImplementation("com.google.truth:truth:1.4.5")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.60.1")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
