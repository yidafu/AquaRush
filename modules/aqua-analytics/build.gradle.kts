plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
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
  implementation(libs.spring.boot.starter.data.jpa)
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
  implementation(libs.flyway.core)

  // ========================================================
  // 各模块特有依赖
  // ========================================================
  // aqua-reconciliation
  implementation(libs.bundles.http.client)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.commons.lang3)
  implementation(libs.commons.collections4)
  implementation("com.github.javafaker:javafaker:1.0.2")
  implementation(libs.spring.retry)
  implementation(libs.resilience4j.spring.boot3)

  // ========================================================
  // Mappie
  // ========================================================
  implementation(libs.mappie.api)

  // ========================================================
  // 测试依赖
  // ========================================================
  testImplementation(libs.spring.boot.starter.test)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.postgresql)
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

// QueryDSL 生成源码目录
kotlin {
  sourceSets {
    main {
      kotlin.srcDir(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
      kotlin.srcDir(layout.buildDirectory.dir("generated/kspKotlin/main"))
    }
  }
}
