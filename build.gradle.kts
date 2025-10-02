import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.expediagroup.graphql") version "8.8.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    jacoco
}

group = "net.leanix"
version = "v1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2024.0.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.3.0")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")
    implementation("com.expediagroup:graphql-kotlin-spring-client:8.8.1")
    developmentOnly("io.netty:netty-resolver-dns-native-macos:4.2.6.Final") {
        artifact {
            classifier = "osx-aarch_64"
        }
    }

    // Dependencies for generating JWT token
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

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
                "com.google.protobuf:protobuf-java" -> useVersion("4.28.2")
                "commons-io:commons-io" -> useVersion("2.14.0")
                "io.ktor:ktor-client-core" -> useVersion("3.0.0-rc-2")
            }
        }
    }
}

detekt {
    autoCorrect = true
    parallel = true
    buildUponDefaultConfig = true
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
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
