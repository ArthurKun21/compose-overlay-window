plugins {
    id("cfw.library")
    id("cfw.library.tests")
    id("cfw.maven.publish")
}

android {
    namespace = "com.github.only52607.compose.window"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
        )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.google.material)
}

mavenPublishing {
    coordinates(
        groupId = "com.github.ArthurKun21",
        artifactId = "compose-overlay-window",
        version = version.toString(),
    )

    pom {
        name.set("compose-overlay-window")
        description.set("Global Floating Window Framework based on Jetpack Compose")
    }
}
