import buildlogic.AndroidConfig
import buildlogic.configureAndroid

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("code.lint")
}

android {
    defaultConfig {
        lint.targetSdk = AndroidConfig.TARGET_SDK
    }
    configureAndroid(this)
}