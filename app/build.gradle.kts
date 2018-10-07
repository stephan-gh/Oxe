import net.minecrell.gradle.licenser.LicenseProperties

plugins {
    id("com.android.application")
    id("net.minecrell.licenser") version "0.4.1"
}

android {
    compileSdkVersion(28)

    defaultConfig {
        applicationId = "net.minecrell.oxe"

        minSdkVersion(19)
        targetSdkVersion(28)

        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer:2.9.0")
}

license {
    header = rootProject.file("licenses/oxe.txt")

    matching("**/drawable/ic_*.xml", delegateClosureOf<LicenseProperties> {
        header = rootProject.file("licenses/material-design-icons.txt")
    })

    matching("**/exo_*.xml", delegateClosureOf<LicenseProperties> {
        header = rootProject.file("licenses/exoplayer.txt")
    })
}
