plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.kotlin.compose)

}

android {
    namespace = "com.cadetia.simplicadet"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.cadetia.simplicadet"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 2
        versionName = "1.3b"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    androidResources {
        localeFilters += setOf("en-rGB", "es-rES", "fr-rFR", "ro-rRO")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        //noinspection DataBindingWithoutKapt
        dataBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    //DEFAULT DEPENDENCIES
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.generativeai)


    // Design
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.constraintlayout.v214)
    implementation("com.otaliastudios:zoomlayout:1.9.0")
    implementation("com.github.Dimezis:BlurView:version-2.0.6")
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("com.github.amarjain07:StickyScrollView:1.0.3")

    // SDP and SSP libraries
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    //AI

    //PDF
    implementation ("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // Ads
    implementation("com.google.android.gms:play-services-ads:24.4.0")

    // ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")


    // Aesthetics
    implementation ("com.airbnb.android:lottie:4.2.0")
    implementation ("androidx.transition:transition:1.6.0")
    implementation ("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    // Room Database Dependencies
    implementation ("androidx.room:room-runtime:2.7.2")
    annotationProcessor ("androidx.room:room-compiler:2.7.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-firestore:25.1.4")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-functions:21.2.1")
    implementation("com.google.firebase:firebase-crashlytics")

    //Facebook
    implementation ("com.facebook.android:facebook-login:latest.release")

    //Google
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("androidx.credentials:credentials:1.5.0")
    implementation ("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}
