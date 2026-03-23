plugins {
  id("aqua.kotlin.spring")
  id("aqua.spring.boot.library")
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(libs.spring.boot.starter.aop)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.graphql)
  implementation(libs.spring.boot.starter.security)

  implementation(project(":modules:aqua-common"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.logstash.logback.encoder)
  implementation(libs.jackson.module.kotlin)

  testImplementation(libs.spring.boot.starter.test)
  implementation(libs.spring.boot.starter.aop)
  compileOnly(libs.spotbugs.annotations)
}
