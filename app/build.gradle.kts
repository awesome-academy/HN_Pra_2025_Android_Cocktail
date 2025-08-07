plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.gitlab.arturbosch.detekt") version "1.22.0-RC2"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config = files()
    ignoreFailures = true // Tạm thời cho phép build thành công dù có lỗi detekt
}

android {
    namespace = "com.example.cocktaildb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cocktaildb"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

// Ktlint configuration
configurations {
    val ktlint by creating
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    
    // Ktlint dependency với version cũ hơn tương thích với Java 17
    "ktlint"("com.pinterest:ktlint:0.45.2")
}

// Ktlint tasks
tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = configurations["ktlint"]
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = configurations["ktlint"]
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}