plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val _jvmTarget = findProperty("jvmTarget").toString()

kotlin {
    jvm {
        withJava()
        mainRun {
            mainClass.set("io.lugf027.github.mermaid.cli.MainKt")
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(
                        org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(_jvmTarget)
                    )
                }
            }
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":mermaid-core"))
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// 配置可执行 JAR（fat jar / shadow jar 替代方案）
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles a fat JAR containing all dependencies"
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "io.lugf027.github.mermaid.cli.MainKt"
    }
    val jvmMainCompilation = kotlin.jvm().compilations["main"]
    from(jvmMainCompilation.output.allOutputs)
    dependsOn(jvmMainCompilation.compileTaskProvider)
    from({
        jvmMainCompilation.runtimeDependencyFiles.filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
