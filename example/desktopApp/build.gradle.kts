import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":example:shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(libs.kotlinx.coroutinesSwing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.lugf027.mermaid.example.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "mermaid-example"
            packageVersion = "1.0.0"
        }
    }
}
