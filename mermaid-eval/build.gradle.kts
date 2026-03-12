plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val _jvmTarget = findProperty("jvmTarget").toString()

kotlin {
    jvm {
        withJava()
        mainRun {
            mainClass.set("io.lugf027.github.mermaid.eval.EvalMainKt")
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

// fat jar
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles a fat JAR for mermaid-eval"
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "io.lugf027.github.mermaid.eval.EvalMainKt"
    }
    val jvmMainCompilation = kotlin.jvm().compilations["main"]
    from(jvmMainCompilation.output.allOutputs)
    dependsOn(jvmMainCompilation.compileTaskProvider)
    from({
        jvmMainCompilation.runtimeDependencyFiles.filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
