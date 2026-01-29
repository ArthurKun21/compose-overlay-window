import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.android.application")
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)
}
