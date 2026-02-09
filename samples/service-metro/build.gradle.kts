plugins {
    id("android.application")
    id("android.tests")
    id("sample.common.deps")

    alias(libs.plugins.metro)
}

android {
    namespace = "io.github.arthurkun.service.metro"

    defaultConfig {
        applicationId = "io.github.arthurkun.floating.window.metro"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":library"))

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.metro.runtime)

    implementation(libs.bundles.datastore)
}
