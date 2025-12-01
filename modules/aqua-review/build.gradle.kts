plugins {
  id("aqua.kotlin.spring")
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-delivery"))
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.graphql)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.http.client)
  implementation(libs.bundles.cache)
  implementation(libs.postgresql)
  // Validation
  implementation(libs.spring.boot.starter.validation)

  // For Redis cache
  implementation(libs.bundles.redis)

  testImplementation(libs.spring.boot.starter.test)
}
