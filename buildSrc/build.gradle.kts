plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.androidx.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.compose.compiler.gradle)
    implementation(libs.spotless.gradle)
    implementation(gradleApi())

    // workaround to enable version catalogs (libs) in buildSrc
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
