plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
}

group = "dev.yidafu.aqua"
version = "1.0.0"

configurations.all {
  exclude(group = "org.yaml", module = "snakeyaml")
}

dependencies {
  // AquaRush modules
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))

  // Spring Boot
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // GraphQL
  implementation(libs.bundles.graphql)
  // Database
  runtimeOnly(libs.postgresql)
  implementation(libs.flyway.core)

  // HTTP Client for WeChat API
  implementation(libs.bundles.http.client)
  implementation(libs.jackson.module.kotlin)
//  implementation(libs.jackson.datatype.jsr310)

  // JWT
//  implementation(libs.bundles.jwt)

  // Apache Commons
  implementation(libs.commons.lang3)
  implementation(libs.commons.collections4)
  implementation("com.github.javafaker:javafaker:1.0.2")
  // Retry and resilience
  implementation(libs.spring.retry)
  implementation(libs.resilience4j.spring.boot3)

  // Testing
  testImplementation(libs.spring.boot.starter.test)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.postgresql)
  testImplementation(libs.springmockk)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}
