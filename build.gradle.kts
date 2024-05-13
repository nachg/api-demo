import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion
    id("io.qameta.allure") version "2.11.2"
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation ("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    implementation("org.xpathqs:framework-testng:0.0.8")
    implementation("org.xpathqs:cache:0.2")

    implementation("io.qameta.allure:allure-testng:2.14.0")
    implementation("io.qameta.allure:allure-rest-assured:2.14.0")
    implementation("io.qameta.allure:allure-attachments:2.14.0")

    implementation("io.rest-assured:rest-assured:5.2.0")

    implementation("org.slf4j:slf4j-log4j12:1.7.29")
    implementation("io.swagger.core.v3:swagger-core:2.1.11")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.2")

    implementation("org.springframework.data:spring-data-commons:2.6.2")

    implementation("com.jayway.jsonpath:json-path:2.7.0")

    implementation("io.konform:konform-jvm:0.3.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.1.1")

    implementation("com.jayway.jsonpath:json-path:2.7.0")

    implementation("org.mockito:mockito-core:2.1.0")
    implementation("org.reflections:reflections:0.10.2")

    implementation("com.github.javafaker:javafaker:1.0.2") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    implementation("io.swagger.parser.v3:swagger-parser-v3:2.1.20")
}

tasks.named<Test>("test") {
    defaultCharacterEncoding = "UTF-8"

    useTestNG(closureOf<TestNGOptions> {
        val path =  System.getenv("xml_suite_path") ?: "src/test/resources/suites/all.xml"

        println("XML Suite path: $path")
        suites(path)
    })

    testLogging {
        showStandardStreams = true
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }
}

tasks.named<Jar>("jar") {
    from(sourceSets["test"].output)
    from(configurations.testRuntimeClasspath.map { config -> config.map { if (it.isDirectory) it else zipTree(it) } })
    from(configurations.runtimeClasspath.map { config -> config.map { if (it.isDirectory) it else zipTree(it) } })
    isZip64 = true
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

allure {
    version.set("2.14.0")
    autoconfigure = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}