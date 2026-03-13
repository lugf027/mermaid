import java.io.FileInputStream
import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.mavenPublish).apply(false)
}

rootProject.projectDir.resolve("local.properties").let {
    if (it.exists()) {
        Properties().apply {
            load(FileInputStream(it))
        }.forEach { (k, v) -> rootProject.ext.set(k.toString(), v) }
        System.getenv().forEach { (k, v) ->
            rootProject.ext.set(k, v)
        }
    }
}

// Resolve version: prioritize VERSION_TAG env var (set by CI from git tag), fall back to gradle.properties
val resolvedVersion: String = System.getenv("VERSION_TAG").takeUnless { it.isNullOrBlank() }
    ?: findProperty("VERSION") as String

version = resolvedVersion

subprojects {
    group = findProperty("group") as String
    version = resolvedVersion
}

// Only mermaid-core is published to Maven Central
project(":mermaid-core") {
    apply(plugin = "com.vanniktech.maven.publish")

    extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral(true)
        signAllPublications()

        pom {
            name.set("mermaid-kmp")
            description.set("Kotlin Multiplatform implementation of Mermaid.js — parse and render Mermaid diagrams to SVG without a browser.")
            url.set("https://github.com/lugf027/mermaid-kmp")
            inceptionYear.set("2025")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("lugf027")
                    name.set("lugf027")
                    url.set("https://github.com/lugf027")
                }
            }
            scm {
                url.set("https://github.com/lugf027/mermaid-kmp")
                connection.set("scm:git:https://github.com/lugf027/mermaid-kmp.git")
                developerConnection.set("scm:git:https://github.com/lugf027/mermaid-kmp.git")
            }
        }
    }
}