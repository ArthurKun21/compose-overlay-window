pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // fallback for the rest of the dependencies
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

    }
}

val isJitPack = System.getenv("JITPACK") == "true"

rootProject.name = "ComposeFloatingWindow"
include(":library")
if (!isJitPack) {
    include(":samples:app-activity")
    include(":samples:service-hilt")
    include(":samples:fullscreen-dialog")
    include(":samples:keyboard-usage")
}
