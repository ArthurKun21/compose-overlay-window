import buildlogic.AndroidConfig
import buildlogic.configureAndroid

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("code.lint")
}

android {
    defaultConfig {
        targetSdk = AndroidConfig.TARGET_SDK
    }
    configureAndroid(this)

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}
