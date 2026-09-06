import com.android.build.api.dsl.ApplicationExtension
import cfw.buildlogic.AndroidConfig
import cfw.buildlogic.configureAndroid
import cfw.buildlogic.configureCommonKotlinCompileOptions
import cfw.buildlogic.libs
import cfw.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply(libs.pluginId("compose-compiler"))
                apply("cfw.code.lint")
            }

            extensions.configure<ApplicationExtension> {
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

            configureCommonKotlinCompileOptions()
        }
    }
}
