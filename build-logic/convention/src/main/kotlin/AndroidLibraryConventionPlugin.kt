import cfw.buildlogic.AndroidConfig
import cfw.buildlogic.configureCommonKotlinCompileOptions
import cfw.buildlogic.libs
import cfw.buildlogic.pluginId
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply(libs.pluginId("compose-compiler"))
                apply("cfw.code.lint")
                apply("cfw.android.base")
            }

            extensions.configure<LibraryExtension> {
                defaultConfig {
                    lint.targetSdk = AndroidConfig.TARGET_SDK
                }

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
