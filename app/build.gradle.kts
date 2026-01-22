
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}
apply(plugin = "kotlin-parcelize")

android {
    namespace = "com.example.myproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myproject"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.2"

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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)

    implementation("io.coil-kt:coil:2.4.0")

    implementation("androidx.fragment:fragment-ktx:1.8.8")

    implementation (libs.github.glide)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    //Notification
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
    //MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
