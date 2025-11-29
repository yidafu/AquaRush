plugins {
  id("aqua.spring.boot.application")
}

// Specify the main class
springBoot {
  mainClass = "dev.yidafu.aqua.AquaRushApplicationKt"
}

dependencies {
  // Include all other modules
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-user"))
  implementation(project(":modules:aqua-product"))
  implementation(project(":modules:aqua-order"))
  implementation(project(":modules:aqua-delivery"))
  implementation(project(":modules:aqua-payment"))
  implementation(project(":modules:aqua-statistics"))
  implementation(project(":modules:aqua-notice"))
  implementation(project(":modules:aqua-review"))

  // Entry module specific dependencies
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.thymeleaf)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.graphql)
  implementation(libs.liquibase.core)
  runtimeOnly(libs.postgresql)
}
