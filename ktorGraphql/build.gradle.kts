import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {
    val ktor = "1.1.2"
}

plugins {
    kotlin("jvm") version "1.3.21"
    application
}

group = "info.vividcode.playground"
version = "1.0-SNAPSHOT"

configure<ApplicationPluginConvention> {
    mainClassName = "info.vividcode.playground.graphql.ktor.MainKt"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("com.graphql-java:graphql-java:11.0")
    implementation("org.slf4j:slf4j-simple:1.7.25")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
