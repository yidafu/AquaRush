plugins {
  id("aqua.spring.boot.application")
}

// Specify the main class
springBoot {
  mainClass = "dev.yidafu.aqua.client.AquaClientApplicationKt"
}

// Include shared configuration resources
sourceSets {
  main {
    resources {
      srcDir("../shared-config")
    }
  }
}

// Shared GraphQL types: `shared-config/graphql/shared-types.graphqls` is the single
// source of truth. The copy under `src/main/resources/graphql` is a git-tracked
// SYMLINK pointing to it — do not replace it with a regular copy or re-add a copy task here.


dependencies {
  // Include user service specific modules
  implementation(project(":modules:aqua-foundation"))
  implementation(project(":modules:aqua-trade"))
  implementation(project(":modules:aqua-analytics"))
  implementation(project(":modules:aqua-platform"))
  implementation(project(":modules:aqua-user"))
  implementation(project(":modules:aqua-product"))
  implementation(project(":modules:aqua-ops")) // For operations config domain
  implementation(project(":modules:aqua-marketing")) // For marketing domain (F-MKT-001/002/003/004/007)

  // Entry module specific dependencies
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.thymeleaf)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.graphql)
  implementation(libs.mappie.api)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.liquibase.core)
  runtimeOnly(libs.postgresql)
}
