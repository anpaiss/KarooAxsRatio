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
        versionCode   = 15
        versionName   = "1.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

// Karoo extension manifest: the JSON the Karoo fetches from MANIFEST_URL (see
// AndroidManifest.xml) to list the app in its Extension Library and offer
// updates. Written on every build to app/manifest.json (ignored); attach it to
// each GitHub release next to KarooAxsRatio.apk. latestVersionCode must be the
// versionCode of that APK — it is what decides whether an update is offered.
// Schema: io.hammerhead.karooext.models.KarooAppManifest.
val generateManifest by tasks.registering {
    val out = layout.projectDirectory.file("manifest.json")
    val version = android.defaultConfig.versionName ?: "0"
    val code = android.defaultConfig.versionCode ?: 0
    val notes = System.getenv("RELEASE_NOTES").orEmpty()
    inputs.property("version", version); inputs.property("code", code); inputs.property("notes", notes)
    outputs.file(out)
    doLast {
        fun q(v: String) = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        val shots = listOf("vivid", "outline", "pastel", "ink", "settings")
            .joinToString(", ") { q("https://raw.githubusercontent.com/anpaiss/KarooAxsRatio/master/docs/screenshot-$it.png") }
        out.asFile.writeText(
            """
            {
              "label": "AXS Ratio",
              "packageName": "com.anpaiss.axsratio",
              "latestApkUrl": "https://github.com/anpaiss/KarooAxsRatio/releases/latest/download/KarooAxsRatio.apk",
              "latestVersion": ${q(version)},
              "latestVersionCode": $code,
              "developer": "Andrea Paissan",
              "description": ${q("Small, always-visible metric tiles in the four corners of the ride screen, on top of whatever page you are viewing: SRAM AXS gear, heart rate, power, cadence, speed, grade, temperature, distance to next turn. Four tile styles, all readable in direct sunlight.")},
              "releaseNotes": ${q(notes)},
              "screenshotUrls": [$shots],
              "tags": ["performance"]
            }
            """.trimIndent()
        )
    }
}
tasks.named("preBuild") { dependsOn(generateManifest) }

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.hammerhead.karoo.ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
}
