import com.android.build.api.dsl.LibraryExtension
import cfw.buildlogic.library
import cfw.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class AndroidLibraryTestsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                testOptions {
                    unitTests.all {
                        it.useJUnitPlatform()
                    }
                }
            }

            dependencies {
                add("testImplementation", libs.library("junit"))
                add("androidTestImplementation", libs.library("androidx-test-junit"))
                add("androidTestImplementation", libs.library("androidx-test-espresso"))
                add("androidTestImplementation", platform(libs.library("compose-bom")))
                add("androidTestImplementation", libs.library("compose-ui-test-junit4"))
            }
        }
    }
}
