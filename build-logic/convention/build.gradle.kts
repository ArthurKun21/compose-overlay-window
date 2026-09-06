plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.androidx.gradle)
    compileOnly(libs.kotlin.gradle)
    compileOnly(libs.compose.compiler.gradle)
    compileOnly(libs.vanniktech.maven.publish.gradle)
    implementation(libs.spotless.gradle)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "cfw.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidTests") {
            id = "cfw.android.tests"
            implementationClass = "AndroidTestsConventionPlugin"
        }
        register("codeLint") {
            id = "cfw.code.lint"
            implementationClass = "CodeLintConventionPlugin"
        }
        register("androidLibrary") {
            id = "cfw.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryTests") {
            id = "cfw.library.tests"
            implementationClass = "AndroidLibraryTestsConventionPlugin"
        }
        register("sampleCommonDeps") {
            id = "cfw.sample.common.deps"
            implementationClass = "SampleCommonDepsConventionPlugin"
        }
        register("mavenPublish") {
            id = "cfw.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
    }
}
