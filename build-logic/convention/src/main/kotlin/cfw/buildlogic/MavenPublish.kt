package cfw.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

internal fun Project.configureMavenPublish() {
    version = providers.environmentVariable("RELEASE_TAG")
        .map { it.removePrefix("v") }
        .getOrElse("1.0.0")

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ArthurKun21/compose-overlay-window")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        pom {
            url.set("https://github.com/ArthurKun21/compose-overlay-window")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("ArthurKun21")
                    name.set("Arthur")
                    email.set("16458204+ArthurKun21@users.noreply.github.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/ArthurKun21/compose-overlay-window.git")
                developerConnection.set("scm:git:ssh://github.com/ArthurKun21/compose-overlay-window.git")
                url.set("https://github.com/ArthurKun21/compose-overlay-window")
            }
        }
    }
}
