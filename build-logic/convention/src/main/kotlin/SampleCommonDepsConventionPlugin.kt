import cfw.buildlogic.library
import cfw.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class SampleCommonDepsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            dependencies {
                add("implementation", libs.library("appcompat"))
                add("implementation", libs.library("androidx-core-ktx"))
                add("implementation", libs.library("androidx-lifecycle-runtime-ktx"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("activity-compose"))
                add("implementation", platform(libs.library("compose-bom")))
                add("implementation", libs.findBundle("compose-ui").get())
            }
        }
    }
}
