plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-order"))
  implementation(project(":modules:aqua-delivery"))
  implementation(project(":modules:aqua-payment"))
  implementation(project(":modules:aqua-review"))
  implementation(project(":modules:aqua-user"))
  implementation(project(":modules:aqua-product"))

  implementation(libs.bundles.graphql)

  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.graphql)
  implementation(libs.bundles.spring.boot.data)

  implementation(libs.mappie.api)
  // Statistics-specific dependencies can be added here
}
