import java.util.Properties

// 1) load local.properties by hand
val localProps = Properties().apply {
    load(rootProject.file("local.properties").reader())
}

// 2) pull your spoonacular key out
val spoonKey: String = localProps
    .getProperty("SPOONACULAR_API_KEY")
    ?: throw GradleException("SPOONACULAR_API_KEY not found in local.properties")

plugins {
    alias(libs.plugins.androidApplication)
    // ↓ explicitly pull in kotlin-android @ 1.9.0
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("kotlin-parcelize")               // this piggy-backs off the kotlin plugin version
    alias(libs.plugins.googleServices)
    id("androidx.navigation.safeargs.kotlin")
    id ("kotlin-kapt")
}


android {
    namespace = "com.example.recipes"
    compileSdk = 34

    defaultConfig {
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$spoonKey\"")

        applicationId = "com.example.recipes"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }

}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.8.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.google.android.material:material:1.8.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.1")
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("androidx.core:core:1.10.1")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.activity)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}