package cfw.buildlogic

import org.gradle.api.Project

/**
 * Compose Multiplatform ships its Android artifacts aligned to a single Compose version. Material3
 * declares much older minimums for its transitive dependencies, so without these constraints the
 * graph silently mixes e.g. foundation 1.13.0-alpha01 with ui 1.13.0-alpha03.
 */
object ComposeAlignment {
    private val MODULES =
        listOf(
            "compose-foundation",
            "compose-foundation-layout",
            "compose-animation",
            "compose-animation-core",
            "compose-material-ripple",
        )

    fun constrain(
        project: Project,
        configuration: String = "implementation",
    ) {
        val libs = project.libs
        val constraints = project.dependencies.constraints
        MODULES.forEach { alias -> constraints.add(configuration, libs.library(alias)) }
    }
}