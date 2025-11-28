plugins {
  id("aqua.kotlin.spring")
}

dependencies {
  // Only depend on common-module
  implementation(project(":modules:aqua-common"))

  // GraphQL
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)
  // Security
  implementation(libs.bundles.spring.boot.security)
//  implementation(libs.bundles.jwt)
  implementation(libs.bundles.jackson)
  // Validation
  implementation(libs.spring.boot.starter.validation)

  // Actuator
  implementation(libs.spring.boot.starter.actuator)

  // HTTP Client
  implementation(libs.spring.boot.starter.web)
  testImplementation(libs.spring.boot.starter.test)

  implementation(libs.bundles.jackson)

}
