import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

plugins {
    id("org.springframework.boot") version "3.2.7"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.expediagroup.graphql") version "7.0.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    jacoco
}

group = "net.leanix"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.1"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.1")
    implementation("com.expediagroup:graphql-kotlin-spring-client:7.0.2")

    // Dependencies for generating JWT token
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2"){
        exclude(module = "mockito-core")
    }
}

val generateGitHubGraphQLClient by tasks.creating(GraphQLGenerateClientTask::class)  {
    packageName.set("net.leanix.githubagent.graphql.data")
    schemaFile = file("${project.projectDir}/src/main/resources/schemas/schema.docs-enterprise.graphql")
    queryFileDirectory = file("${project.projectDir}/src/main/resources/graphql")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            when (requested.module.toString()) {
                "org.bouncycastle:bcprov-jdk18on" -> useVersion("1.78")
                "com.graphql-java:graphql-java" -> useVersion("21.5")
                "com.expediagroup:graphql-kotlin-graalvm-metadata-generator" -> useVersion("7.1.0")
                "org.springframework:spring-web" -> useVersion("6.1.12")
            }
        }
    }
}

detekt {
    autoCorrect = true
    parallel = true
    buildUponDefaultConfig = true
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(File("${projectDir}/build/jacocoXml/jacocoTestReport.xml"))
    }
}

tasks.processResources {
    doLast {
        file("build/resources/main/gradle.properties").writeText("version=${project.version}")
    }
}
