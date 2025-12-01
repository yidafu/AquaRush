plugins {
  id("aqua.kotlin.spring")
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-logging"))
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.graphql)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.http.client)
  implementation(libs.bundles.cache)
  implementation(libs.spring.boot.starter.quartz)
  implementation(libs.jackson.module.kotlin)
//    implementation(libs.jackson.datatype.jsr310)

  // For Redis cache (optional, can be removed if using in-memory cache)
  implementation(libs.bundles.redis)

  testImplementation(libs.spring.boot.starter.test)
}
