import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Local, git-ignored configuration for the self-hosted LiveKit server.
// Add these keys to local.properties (never commit them):
//   livekit.url=wss://your-vps-host:7880
//   livekit.token.url=https://your-vps-host:8081/voice/token
//   livekit.token.key=optional value for x-weplay-token-key
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
fun localProp(key: String): String = localProps.getProperty(key) ?: ""

android {
    namespace = "com.sundram.weplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sundram.weplay"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Platform API base URL for shared backend contracts. Empty keeps local/demo data.
        buildConfigField("String", "API_BASE_URL", "\"${localProp("api.base.url")}\"")

        // LiveKit server coordinates. Empty strings mean "no server configured",
        // and the voice room falls back to the offline demo engine.
        buildConfigField("String", "LIVEKIT_URL", "\"${localProp("livekit.url")}\"")
        buildConfigField("String", "LIVEKIT_TOKEN_URL", "\"${localProp("livekit.token.url")}\"")
        buildConfigField("String", "LIVEKIT_TOKEN_KEY", "\"${localProp("livekit.token.key")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Demo releases are signed with the debug key until a release keystore
            // is provisioned. The build stays installable on test devices.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    // LiveKit client SDK (talks to our own self-hosted SFU, no third-party service).
    implementation("io.livekit:livekit-android:2.30.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
