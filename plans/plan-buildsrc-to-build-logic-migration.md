# Implementation Plan — Migrate buildSrc to build-logic

**Branch:** `chore/gradle/switch-to-build-logic`
**Status:** Implemented (working tree, pending commit)
**Date:** 2026-09-05

## Problem statement

The project keeps its convention plugins in `buildSrc/`, the legacy mechanism for sharing build
logic. `buildSrc` is compiled as part of every build, invalidates caches frequently, and needs a
well-known workaround (`implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))`)
to make the version catalog accessible to precompiled script plugins. The sibling repository
[generic-datastore](../generic-datastore) already uses the modern `build-logic` included-build
pattern; this migration ports that structure here (`gd.*` → `cfw.*`).

## Current state (buildSrc)

- Six precompiled script plugins: `android.application`, `android.tests`, `code.lint`, `library`,
  `library.tests`, `sample.common.deps`; `code.lint` is applied transitively by
  `android.application` and `library`.
- Helpers in package `buildlogic`: `AndroidConfig` (COMPILE_SDK 37, TARGET_SDK 37, MIN_SDK 24,
  Java/JVM 17) and `ProjectExtensions` (`configureAndroid`).
- AGP 9.4.0, Kotlin 2.4.10, Spotless 8.10.2 are put on the buildSrc classpath via `implementation`;
  plugins are applied by literal ID with the version coming from that classpath.
- Modules consume the plugins by ID; module scripts otherwise only use the root `libs` catalog.
- Gradle 9.7.1, AGP 9 built-in Kotlin (`org.jetbrains.kotlin.android` is never applied),
  configuration cache enabled, JDK 17, JitPack builds only `:library`.
- Root `build.gradle.kts` sets `jvmTarget = JVM_17` on every subproject's `KotlinCompile` tasks.

## Target structure (mirroring generic-datastore)

A `build-logic` included build with a single `:convention` subproject; class-based convention
plugins in the default package registered in a `gradlePlugin` block; catalog access via a
`VersionCatalog` helper (`Libs.kt`); `compileOnly` for AGP/KGP/compose-compiler; the root build
pins every consumed plugin `apply false` so versions resolve on the main build classpath.

```
build-logic/
├── gradle.properties          # gradle#2534 comment; parallel/caching/configureondemand
├── settings.gradle.kts        # dependencyResolutionManagement (google regex-filtered,
│                              #   mavenCentral, gradlePluginPortal) + versionCatalogs
│                              #   create("libs") from files("../gradle/libs.versions.toml")
│                              #   rootProject.name = "build-logic"; include(":convention")
└── convention/
    ├── build.gradle.kts       # `kotlin-dsl`; compileOnly(libs.androidx.gradle / libs.kotlin.gradle /
    │                          #   libs.compose.compiler.gradle); implementation(libs.spotless.gradle);
    │                          #   gradlePlugin { plugins { register(...) } } for the 6 plugins
    └── src/main/kotlin/
        ├── AndroidApplicationConventionPlugin.kt
        ├── AndroidTestsConventionPlugin.kt
        ├── CodeLintConventionPlugin.kt
        ├── AndroidLibraryConventionPlugin.kt
        ├── AndroidLibraryTestsConventionPlugin.kt
        ├── SampleCommonDepsConventionPlugin.kt
        └── cfw/buildlogic/
            ├── AndroidConfig.kt        # ported unchanged (37/37/24, Java 17)
            ├── ProjectExtensions.kt    # configureAndroid() ported; + configureCommonKotlinCompileOptions()
            └── Libs.kt                 # copied from generic-datastore (libs accessor + library/pluginId/version helpers)
```

## Plugin ID mapping (chosen: `cfw.*` namespace)

| buildSrc (old)      | build-logic (new)          | Class (default package)            |
|---------------------|----------------------------|------------------------------------|
| `android.application` | `cfw.android.application` | `AndroidApplicationConventionPlugin` |
| `android.tests`       | `cfw.android.tests`       | `AndroidTestsConventionPlugin`       |
| `code.lint`           | `cfw.code.lint`           | `CodeLintConventionPlugin`           |
| `library`             | `cfw.library`             | `AndroidLibraryConventionPlugin`     |
| `library.tests`       | `cfw.library.tests`       | `AndroidLibraryTestsConventionPlugin`|
| `sample.common.deps`  | `cfw.sample.common.deps`  | `SampleCommonDepsConventionPlugin`   |

Helpers move from package `buildlogic` to `cfw.buildlogic` (mirroring `gd.buildlogic`).

## Convention plugin bodies (1:1 ports of existing logic)

- **CodeLintConventionPlugin** — same shape as generic-datastore's `SpotlessConventionPlugin`:
  `pluginManager.apply("com.diffplug.spotless")`; ktlint version via
  `libs.findLibrary("ktlint-core")`, `editorConfigOverride(ktlint_standard_annotation = disabled)`,
  `trimTrailingWhitespace()`, `endWithNewline()`, xml `format("xml")` block.
- **AndroidApplicationConventionPlugin** — applies `com.android.application`,
  `libs.pluginId("compose-compiler")`, chained `cfw.code.lint`; configures `ApplicationExtension`:
  `defaultConfig.targetSdk`, `configureAndroid(this)`, `buildFeatures.compose = true`, packaging
  excludes `/META-INF/{AL2.0,LGPL2.1}`; then `configureCommonKotlinCompileOptions()`.
- **AndroidTestsConventionPlugin** — applies `com.android.application`; `testInstrumentationRunner`;
  deps via catalog helpers: `testImplementation` junit; `androidTestImplementation`
  androidx-test-junit, androidx-test-espresso, `platform(compose-bom)`, compose-ui-test-junit4.
- **AndroidLibraryConventionPlugin** — applies `com.android.library`, compose-compiler, chained
  `cfw.code.lint`; configures `LibraryExtension`: `defaultConfig.lint.targetSdk`,
  `configureAndroid`, `buildFeatures.compose`, packaging excludes; then
  `configureCommonKotlinCompileOptions()`.
- **AndroidLibraryTestsConventionPlugin** — applies `com.android.library`; testInstrumentationRunner;
  `testOptions.unitTests.all { useJUnitPlatform() }`; same test-dep set as AndroidTests (plus junit).
- **SampleCommonDepsConventionPlugin** — applies `com.android.application`; adds appcompat,
  androidx-core-ktx, lifecycle-runtime-ktx, lifecycle-viewmodel-compose, activity-compose,
  `platform(compose-bom)`, bundle `compose-ui`.
- **`configureCommonKotlinCompileOptions()`** —
  `tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JVM_17) } }`
  only (no opt-ins — preserves current behavior).

## Existing file changes

1. **`settings.gradle.kts`** — add `includeBuild("build-logic")` as the first statement inside
   `pluginManagement`.
2. **`build.gradle.kts`** (root) — becomes a plugins-only block (basis pattern), pinning with
   `apply false`: `agp`, `android-library`, `compose-compiler`, `kotlin-android` (all already in
   the catalog, currently unused), `spotless`, plus the existing `ksp` and `hilt-android`. Remove
   the `subprojects { KotlinCompile … }` block and imports — that jvmTarget=17 config moves into
   the app/library convention plugins (`configureCommonKotlinCompileOptions`), covering exactly the
   same modules. The `kotlin-android` pin is what puts KGP on the main build classpath so those
   AGP/KGP types resolve at runtime (build-logic deps are `compileOnly`, per the basis).
3. **Module scripts** — swap to `cfw.*` IDs: `library` (`cfw.library`, `cfw.library.tests`), and
   the 4 samples (`cfw.android.application`, `cfw.android.tests`, `cfw.sample.common.deps`).
   Nothing else in them changes.
4. **Delete `buildSrc/`** entirely.
5. **Catalog cleanup** — remove the stale, unused `[versions]` `compile-sdk = "36"` /
   `target-sdk = "36"` / `min-sdk = "24"` (they contradict `AndroidConfig`'s actual 37/37/24 and
   nothing references them; the basis catalog carries no SDK entries).

## Behavior preserved

- `code.lint` remains applied transitively by the app/library conventions.
- AGP 9 built-in Kotlin: `org.jetbrains.kotlin.android` still never applied to projects (only a
  classpath pin).
- All SDK/JVM constants, dependency sets, packaging excludes, and spotless config stay
  byte-for-byte equivalent.
- JitPack (`JITPACK=true`) still configures only root + `:library`; the included build works there
  since it's wired via `pluginManagement`.

## Verification

1. `./gradlew help` — full configuration with the included build.
2. `./gradlew :library:assembleRelease` — library convention + spotless + publishing setup.
3. `./gradlew :samples:app-activity:assembleDebug :samples:service-hilt:assembleDebug` — app
   conventions, sample deps, hilt/ksp path.
4. `./gradlew spotlessCheck` — lint config still wired (spotless lints module sources only,
   exactly as before; build-logic sources are not linted, same as the basis).
5. `JITPACK=true ./gradlew help` — library-only JitPack path.
6. Spot-check configuration cache still works (enabled in `gradle.properties`; the included build
   is CC-compatible).
