import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

val versionPropertiesFile = file("version.properties")
val versionProps = Properties()

var major = 2
var minor = 4
var buildNum = 0
var vCode = 70

if (versionPropertiesFile.exists()) {
    versionPropertiesFile.inputStream().use { versionProps.load(it) }
    major = versionProps.getProperty("VERSION_MAJOR", "2").toInt()
    minor = versionProps.getProperty("VERSION_MINOR", "4").toInt()
    buildNum = versionProps.getProperty("VERSION_BUILD", "0").toInt()
    vCode = versionProps.getProperty("VERSION_CODE", "70").toInt()
} else {
    versionProps.setProperty("VERSION_MAJOR", major.toString())
    versionProps.setProperty("VERSION_MINOR", minor.toString())
    versionProps.setProperty("VERSION_BUILD", buildNum.toString())
    versionProps.setProperty("VERSION_CODE", vCode.toString())
    versionPropertiesFile.outputStream().use { versionProps.store(it, "Initial version properties") }
}

val isBuilding = gradle.startParameter.taskNames.any { it.contains("assemble") || it.contains("compile") || it.contains("build") }
if (isBuilding) {
    buildNum++
    vCode++
    versionProps.setProperty("VERSION_BUILD", buildNum.toString())
    versionProps.setProperty("VERSION_CODE", vCode.toString())
    versionPropertiesFile.outputStream().use { versionProps.store(it, "Auto-incremented build number") }
}

val computedVersionName = "$major.$minor.$buildNum"

android {
    namespace = "com.ejemplo.laptimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ejemplo.laptimer"
        minSdk = 24
        targetSdk = 34
        versionCode = vCode
        versionName = computedVersionName
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    applicationVariants.all {
        if (buildType.name == "debug") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                output?.outputFileName = "laptimer-${versionName}.apk"
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.mik3y:usb-serial-for-android:3.7.3")
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
