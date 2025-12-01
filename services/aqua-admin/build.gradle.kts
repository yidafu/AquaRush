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

dependencies {
  // Include delivery/admin service specific modules
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-delivery"))
  implementation(project(":modules:aqua-order"))
  implementation(project(":modules:aqua-payment"))
  implementation(project(":modules:aqua-statistics"))
  implementation(project(":modules:aqua-storage"))
  implementation(project(":modules:aqua-reconciliation"))
  implementation(project(":modules:aqua-user")) // For admin user operations

  // Entry module specific dependencies
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.thymeleaf)
  implementation(libs.bundles.spring.boot.liqiubase)
  implementation(libs.bundles.graphql)
  implementation(libs.liquibase.core)
  runtimeOnly(libs.postgresql)
}
