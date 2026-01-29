plugins {
    id("android.application")
    id("android.tests")
    id("sample.common.deps")
}

android {
    namespace = "io.github.arthurkun.keyboard.usage"

    defaultConfig {
        applicationId = "io.github.arthurkun.floating.window"
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
}
