import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("aqua.kotlin.spring")
  id("aqua.spring.boot.library")
  // java-test-fixtures：把共用的 Testcontainers PG 基建(PostgresTestContainer)以
  // testFixtures 形式提供给各业务模块的 test 源集，避免容器参数(内存/连接数)在 8 处漂移。
  id("java-test-fixtures")
  id("aqua.testcontainers")
  // aqua-logging 需要 kotlinx.serialization
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  // ========================================================
  // 来自 aqua-common 的依赖
  // ========================================================
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.spring.boot.web)
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

  // ========================================================
  // Exposed(1.x)：JPA -> Exposed 迁移期的表定义与 Database 基建。
  // 与 JPA 并存，不注册主事务管理器（见 ExposedAutoConfiguration 的类注释）。
  // 版本与接入方式见 AquaRush-JPA-to-Exposed-迁移方案.md §5.1
  // ========================================================
  implementation(libs.exposed.core)
  implementation(libs.exposed.jdbc)
  implementation(libs.exposed.java.time)
  implementation(libs.exposed.json)
  // SpringTransactionManager(spring7-transaction):ExposedAutoConfiguration 的主事务管理器
  implementation(libs.exposed.spring.transaction)
  testImplementation(libs.spring.boot.starter.test)

  // testFixtures 里的 PostgresTestContainer 供业务模块 test 源集复用；
  // 用 testFixturesApi 让消费方(testImplementation(testFixtures(...)))直接拿到
  // PostgreSQLContainer/Database 类型，不必各自重复声明 testcontainers/exposed。
  testFixturesApi(libs.testcontainers.core)
  testFixturesApi(libs.testcontainers.postgresql)
  testFixturesApi(libs.exposed.jdbc)

  // ========================================================
  // 来自 aqua-api 的独立依赖（去重后）
  // ========================================================
  implementation(libs.spring.boot.starter.graphql)
  implementation(libs.spring.boot.starter.validation)

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
      // GraphQL codegen 生成的 Kotlin 源码（schema.kt）
      kotlin.srcDir("src/main/graphql-gen")
    }
  }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
  // JPA all-open 已随 P5 下线;该参数保留为了实体/领域模型构造器注解的默认落点语义。
  freeCompilerArgs.addAll(listOf("-Xannotation-default-target=param-property"))
}
