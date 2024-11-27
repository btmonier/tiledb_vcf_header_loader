plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "org.btmonier"
version = "0.1"

application {
    mainClass = "${group}.vcfhload.cli.IngestVcfHeaderKt"
    applicationName = "ingest-vcf-header"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation("io.tiledb:tiledb-java:0.28.1")
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
    implementation("com.github.samtools:htsjdk:4.0.1")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

