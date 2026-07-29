import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.netflix.dgs.codegen") version "8.6.0"
    id("org.jlleitschuh.gradle.ktlint").version("14.2.0")
    id("jacoco")
}

group = "app.hyuabot"
version = "0.0.1-SNAPSHOT"

jacoco {
    toolVersion = "0.8.15"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

extra["netflixDgsVersion"] = "12.0.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Test dependencies
    implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    implementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    implementation("org.springframework.boot:spring-boot-starter-security-test")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // DGS dependencies
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter")
    implementation("com.netflix.graphql.dgs:graphql-dgs-extended-scalars")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Encrypt secret variables
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:4.0.4")
    // Hibernate utilities
    implementation("io.hypersistence:hypersistence-utils-hibernate-73:3.15.4")
    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    // Calendar
    implementation("com.github.usingsky:KoreanLunarCalendar:0.3.1")
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:${property("netflixDgsVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

ktlint {
    reporters {
        reporter(ReporterType.JSON)
    }
    filter {
        exclude { element ->
            element.file.path.contains("build/generated")
        }
    }
}

tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/graphql-client")
    packageName = "app.hyuabot.backend.codegen"
    generateClient = true
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "2g"
    maxParallelForks = 1
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            enabled = true
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal() // 100% line coverage
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal() // 100% branch coverage
            }
        }
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/app/hyuabot/backend/HyuabotBackendKotlinApplication**",
                        "**/app/hyuabot/backend/database/key/**",
                        "**/app/hyuabot/backend/codegen/**",
                        "**/app/hyuabot/backend/config/CacheConfig**",
                    )
                }
            },
        ),
    )
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
//    finalizedBy(tasks.jacocoTestCoverageVerification)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/app/hyuabot/backend/HyuabotBackendKotlinApplication**",
                        "**/app/hyuabot/backend/database/key/**",
                        "**/app/hyuabot/backend/codegen/**",
                        "**/app/hyuabot/backend/config/CacheConfig**",
                    )
                }
            },
        ),
    )
}

tasks.withType<GenerateJavaTask> {
    typeMapping =
        mutableMapOf(
            "DateTime" to "java.time.ZonedDateTime",
            "Date" to "java.time.LocalDate",
        )
}
