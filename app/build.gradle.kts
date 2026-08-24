import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

// Credenziali firma release da local.properties (mai committato):
// signing.storeFile / signing.storePassword / signing.keyAlias / signing.keyPassword
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace  = "com.anpaiss.axsratio"
    compileSdk = 34

    signingConfigs {
        if (localProps.containsKey("signing.storeFile")) {
            create("release") {
                storeFile     = file(localProps["signing.storeFile"] as String)
                storePassword = localProps["signing.storePassword"] as String
                keyAlias      = localProps["signing.keyAlias"] as String
                keyPassword   = localProps["signing.keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.anpaiss.axsratio"
        minSdk        = 23
        targetSdk     = 34
        versionCode   = 13
        versionName   = "1.1.0-beta1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.hammerhead.karoo.ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
}
