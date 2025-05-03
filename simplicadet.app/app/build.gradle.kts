plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.cadetia.simplicadet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cadetia.simplicadet"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.generativeai)

    // Design
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.constraintlayout.v214)
    implementation("com.otaliastudios:zoomlayout:1.9.0")
    implementation ("androidx.core:core-splashscreen:1.0.1")

    // SDP and SSP libraries
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    //AI
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    //PDF
    implementation ("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")

    // Ads
    implementation("com.google.android.gms:play-services-ads:24.2.0")

    // ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Aesthetics
    implementation ("com.airbnb.android:lottie:4.2.0")
    implementation ("androidx.transition:transition:1.6.0")
    implementation ("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    // Room Database Dependencies
    implementation ("androidx.room:room-runtime:2.7.1")
    annotationProcessor ("androidx.room:room-compiler:2.7.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-firestore:25.1.4")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-functions:21.2.1")

    //Facebook
    implementation ("com.facebook.android:facebook-login:latest.release")

    //Google
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("androidx.credentials:credentials:1.5.0")
    implementation ("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation (libs.material)

}