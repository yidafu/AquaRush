import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("aqua.kotlin.spring")
  id("aqua.kotlin.querydsl")
  id("aqua.kotlin.jpa")
  id("aqua.spring.boot.library")
  // aqua-logging 需要 kotlinx.serialization
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  // ========================================================
  // 来自 aqua-common 的依赖
  // ========================================================
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.spring.boot.web)
  implementation("jakarta.persistence:jakarta.persistence-api:4.0.0-M1")
  implementation("org.hibernate.orm:hibernate-core:6.4.1.Final")
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.bundles.graphql)
  implementation(libs.bundles.messaging)
  implementation(libs.reactor.core)
  implementation(libs.micrometer.tracing.bridge.brave)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.aop)
  implementation(libs.bundles.jackson)
  implementation("jakarta.validation:jakarta.validation-api:4.0.0-M1")
  implementation(libs.bundles.jwt)
  implementation(libs.mapdb)
  implementation(libs.wechat.miniapp)
  implementation(platform("io.github.openfeign.querydsl:querydsl-bom:7.1"))
  implementation("io.github.openfeign.querydsl:querydsl-jpa")
  implementation("io.github.openfeign.querydsl:querydsl-core")
  testImplementation(libs.spring.boot.starter.test)

  // ========================================================
  // 来自 aqua-api 的独立依赖（去重后）
  // ========================================================
  implementation(libs.spring.boot.starter.graphql)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.data.jpa)

  // ========================================================
  // 来自 aqua-logging 的独立依赖（去重后）
  // ========================================================
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.logstash.logback.encoder)
  implementation(libs.jackson.module.kotlin)
  compileOnly(libs.spotbugs.annotations)

  // 统一 GraphQL 测试版本
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
}

// ========================================================
// 源码目录配置（合并三个模块的额外源码目录）
// ========================================================
kotlin {
  sourceSets {
    main {
      // aqua-common 的 graphql-gen 源码
      kotlin.srcDir("src/main/graphql-gen")
      // QueryDSL KSP 生成源码
      kotlin.srcDir(layout.buildDirectory.dir("generated/kspKotlin/main"))
    }
  }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
  // Use addAll (not set) so the -Xallopen + entity annotations
  // configured in the root build.gradle.kts subprojects block are preserved.
  // Setting freeCompilerArgs would wipe out the JPA all-open configuration.
  freeCompilerArgs.addAll(listOf("-Xannotation-default-target=param-property"))
}