plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
}

group = "dev.yidafu.aqua"
version = "1.0.0"

dependencies {
  // 核心依赖:实体/生成类型/api 接口均位于 aqua-foundation(CLAUDE.md 规则 3/4)
  implementation(project(":modules:aqua-foundation"))

  // Spring Boot
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // GraphQL
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)

  // Mappie
  implementation(libs.mappie.api)

  // Database
  runtimeOnly(libs.postgresql)

  // 测试依赖
  testImplementation(libs.spring.boot.starter.test)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
  testImplementation(libs.springmockk)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}
