import cfw.buildlogic.AndroidConfig
import cfw.buildlogic.android
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with

@Suppress("unused")
class AndroidBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            android {
                compileSdk {
                    version = release(AndroidConfig.COMPILE_SDK) {
                        minorApiLevel = AndroidConfig.COMPILE_SDK_MINOR
                    }
                }
                defaultConfig.apply {
                    minSdk = AndroidConfig.MIN_SDK
                }
                compileOptions.apply {
                    sourceCompatibility = AndroidConfig.JavaVersion
                    targetCompatibility = AndroidConfig.JavaVersion
                }
            }
        }
    }
}
