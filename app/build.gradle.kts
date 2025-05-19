// app/build.gradle.kts
import java.util.Properties

// 1) Load local.properties by hand
val localProps = Properties().apply {
    load(rootProject.file("local.properties").reader())
}

// 2) Pull your Spoonacular key out
val spoonKey: String = localProps
    .getProperty("SPOONACULAR_API_KEY")
    ?: throw GradleException("SPOONACULAR_API_KEY not found in local.properties")

val placesKey: String = localProps
    .getProperty("PLACES_API_KEY")
    ?: throw GradleException("PLACES_API_KEY not found in local.properties")


plugins {
    alias(libs.plugins.androidApplication)
    // explicitly pull in kotlin-android @1.9.0
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("kotlin-parcelize")
    alias(libs.plugins.googleServices)
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.recipes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.recipes"
        minSdk        = 24
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // expose your keys to BuildConfig
        // (make sure spoonKey and placesKey are defined in your gradle.properties)
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$spoonKey\"")
        buildConfigField("String", "PLACES_API_KEY", "\"$placesKey\"")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding  = true
        dataBinding  = true
        buildConfig  = true
    }
}



dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines + Play Services
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.activity)
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // Firebase
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Google Play services location & places
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.android.libraries.places:places:3.1.0")
    implementation ("com.google.android.material:material:1.9.0")

    kapt ("com.github.bumptech.glide:compiler:4.15.1")

    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation ("com.squareup.moshi:moshi:1.15.0")
    implementation ("com.squareup.moshi:moshi-kotlin:1.15.0")


    implementation ("com.google.android.libraries.places:places:3.4.0")



}
