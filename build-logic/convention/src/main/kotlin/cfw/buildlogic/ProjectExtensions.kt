package cfw.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = AndroidConfig.COMPILE_SDK

        defaultConfig.apply {
            minSdk = AndroidConfig.MIN_SDK
        }

        compileOptions.apply {
            sourceCompatibility = AndroidConfig.JavaVersion
            targetCompatibility = AndroidConfig.JavaVersion
        }
    }
}

internal fun Project.configureCommonKotlinCompileOptions() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(AndroidConfig.JvmTarget)
        }
    }
}
