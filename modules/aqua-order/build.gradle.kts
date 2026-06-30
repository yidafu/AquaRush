plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))

  // Mapper dependencies - needed for OrderMappers to reference other module mappers
  // These are compileOnly because they are only used for object mapping, not data access
  compileOnly(project(":modules:aqua-product"))
  compileOnly(project(":modules:aqua-user"))
  compileOnly(project(":modules:aqua-delivery"))

  // GraphQL support
  implementation(libs.bundles.graphql)

  // Spring Security
  implementation(libs.bundles.spring.boot.security)

  // Validation
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.mapdb)

  // Order-specific dependencies can be added here

  // Mappie
  implementation(libs.mappie.api)
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
