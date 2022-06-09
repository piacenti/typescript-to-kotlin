import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.github.piacenti:antlr-kotlin-gradle-plugin:0.0.14")
    }
}
plugins {
    kotlin("multiplatform") version "1.6.21"
//    id("org.jetbrains.dokka") version "1.6.20"
    application
}
apply(plugin = "maven-publish")
apply(plugin = "antlr")
apply(plugin = "signing")

group = "com.github.piacenti"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}
val antlrSourceFolder="$buildDir/generated-src/antlr/kotlin"
kotlin {
    js{
        browser()
    }
    jvm()
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("react-spreadsheet", "0.7.4"))
            }
        }
        val jvmMain by getting{
            kotlin.srcDir(antlrSourceFolder)
            dependencies{
                implementation("com.github.piacenti:dsl-maker:1.1.47")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.8.1")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
tasks {
    val generateKotlinGrammarSource by registering(com.strumenta.antlrkotlin.gradleplugin.AntlrKotlinTask::class) {
        group = "build"
        // the classpath used to run antlr code generation
        antlrClasspath = configurations.detachedConfiguration(
            project.dependencies.create("com.github.piacenti:antlr-kotlin-target:0.0.14")
        )
        maxHeapSize = "64m"
        packageName = "piacenti.typescript2kotlin.antlr.generated"
//    arguments = listOf("-atn")
        source = project.objects
            .sourceDirectorySet("antlr", "antlr")
            .srcDir("src/jvmMain/antlr").apply {
                include("*.g4")
            }
        // outputDirectory is required, put it into the build directory
        // if you do not want to add the generated sources to version control
        outputDirectory = file(antlrSourceFolder)
    }
    assemble{
        dependsOn(generateKotlinGrammarSource)
    }
}