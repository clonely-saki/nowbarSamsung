plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val samsungDiagnostic = providers.gradleProperty("samsungDiagnostic")
    .orElse("false")
    .get()
    .toBoolean()
val samsungStandardOnly = providers.gradleProperty("samsungStandardOnly")
    .orElse("false")
    .get()
    .toBoolean()

android {
    namespace = "com.nowbarsports.poc"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = if (samsungDiagnostic) "com.nhn.android.nmap" else "com.nowbarsports.poc"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = if (samsungDiagnostic) "0.1.0-samsung-diagnostic" else "0.1.0"
        buildConfigField("boolean", "SAMSUNG_DIAGNOSTIC", samsungDiagnostic.toString())
        buildConfigField("boolean", "SAMSUNG_STANDARD_ONLY", samsungStandardOnly.toString())
        resValue(
            "string",
            "app_name",
            if (samsungStandardOnly) {
                "Samsung Now Bar Diagnostic — STANDARD LIVE UPDATE"
            } else if (samsungDiagnostic) {
                "Samsung Now Bar Diagnostic — NOT FOR DISTRIBUTION"
            } else {
                "Sports Now Bar Lab"
            }
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
