plugins {
  id("aqua.spring.boot.application")
}

// Specify the main class
springBoot {
  mainClass = "dev.yidafu.aqua.admin.AquaAdminApplicationKt"
}

// Include shared configuration resources
sourceSets {
  main {
    resources {
      srcDir("../shared-config")
    }
  }
  test {
    resources {
      srcDir("../shared-config")
    }
  }
}

// Copy shared GraphQL schemas to the correct location for Spring Boot GraphQL
tasks.register<Copy>("copySharedGraphQL") {
  from("../../shared-config/graphql")
  into("src/main/resources/graphql")
  include("*.graphqls")
}

// Copy database changelog files from aqua-entry module
tasks.register<Copy>("copyDatabaseChangelog") {
  from("../../modules/aqua-entry/src/main/resources/db")
  into("src/main/resources/db")
}

// Ensure resources are copied before compilation
tasks.named("compileKotlin") {
  dependsOn("copySharedGraphQL")
  dependsOn("copyDatabaseChangelog")
}

tasks.named("processResources") {
  dependsOn("copySharedGraphQL")
  dependsOn("copyDatabaseChangelog")
}

// Copy GraphQL schemas for tests
tasks.register<Copy>("copyGraphQLForTest") {
  from("../../shared-config/graphql")
  into("src/test/resources/graphql")
  include("*.graphqls")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("processTestResources") {
  dependsOn("copyGraphQLForTest")
}

dependencies {
  // Include delivery/admin service specific modules
  implementation(project(":modules:aqua-foundation"))
  implementation(project(":modules:aqua-trade"))
  implementation(project(":modules:aqua-analytics"))
  implementation(project(":modules:aqua-platform"))
  implementation(project(":modules:aqua-user")) // For admin user operations
  implementation(project(":modules:aqua-product")) // For product services

  // Entry module specific dependencies
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.bundles.messaging)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.thymeleaf)
  implementation(libs.bundles.spring.boot.liqiubase)
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.mappie.api)
  implementation(libs.liquibase.core)
  runtimeOnly(libs.postgresql)

  // Test dependencies
  testImplementation(libs.h2)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.springmockk)
  testImplementation(libs.mockk.core)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
  testImplementation("org.springframework.security:spring-security-test")
  // Source: https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-graphql-test
  testImplementation("org.springframework.boot:spring-boot-starter-graphql-test:4.1.0-M4")
}
