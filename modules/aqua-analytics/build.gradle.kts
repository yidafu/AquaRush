plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P3:仓储全部改写为 Exposed DSL,摘除 QueryDSL/KSP
  id("aqua.kotlin.exposed")
  // Testcontainers 真实 PG 测试(容器运行时接线见 build-logic/aqua.testcontainers.gradle.kts)
  id("aqua.testcontainers")
}

group = "dev.yidafu.aqua"
version = "1.0.0"

// aqua-reconciliation 原有的 snakeyaml 排除
configurations.all {
  exclude(group = "org.yaml", module = "snakeyaml")
}

dependencies {
  // ========================================================
  // 核心依赖
  // ========================================================
  implementation(project(":modules:aqua-foundation"))

  // ========================================================
  // 跨业务模块依赖（TODO: 应通过 API 接口访问）
  // ========================================================
  // reconciliation 原本 compileOnly 依赖 order + delivery
  // 合并后改为对 aqua-trade 的正式依赖
  implementation(project(":modules:aqua-trade"))

  // ========================================================
  // Spring Boot（合并去重）
  // ========================================================
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // ========================================================
  // GraphQL
  // ========================================================
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)

  // ========================================================
  // Database
  // ========================================================
  runtimeOnly(libs.postgresql)

  // ========================================================
  // 各模块特有依赖
  // ========================================================
  // aqua-reconciliation
  implementation(libs.bundles.http.client)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.commons.lang3)
  implementation(libs.commons.collections4)
  implementation("com.github.javafaker:javafaker:1.0.2")

  // ========================================================
  // 测试依赖
  // ========================================================
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
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
