plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P4:两插件暂共存 —— order/payment/review 的 QueryDSL Impl
  // 全量迁到 Exposed 后摘除 querydsl(与 marketing/product 迁移节奏一致)。
  id("aqua.kotlin.querydsl")
  id("aqua.kotlin.exposed")
  // Testcontainers 真实 PG 测试(容器运行时接线见 build-logic/aqua.testcontainers.gradle.kts)
  id("aqua.testcontainers")
}

dependencies {
  // ========================================================
  // 核心依赖（替代原来对 aqua-common/api/logging 的分别依赖）
  // ========================================================
  implementation(project(":modules:aqua-foundation"))

  // ========================================================
  // 对其他业务模块的依赖
  // ========================================================
  // order 的 mapper 需要 product/user 类型（原 compileOnly，保持）
  compileOnly(project(":modules:aqua-product"))
  compileOnly(project(":modules:aqua-user"))

  // delivery 原本 implementation(:aqua-user)，mapper 层使用 → 保持 compileOnly
  // 如果 delivery 在 service 层（非 mapper）使用了 user 类型，需要改为 implementation
  compileOnly(project(":modules:aqua-user"))

  // ========================================================
  // GraphQL / Security / Data（合并去重）
  // ========================================================
  implementation(libs.bundles.graphql)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.bundles.spring.boot.web)

  // ========================================================
  // 各模块特有依赖
  // ========================================================
  // aqua-order: MapDB
  implementation(libs.mapdb)

  // aqua-payment: WeChat Pay SDK
  implementation(libs.wechatpay.sdk)

  // aqua-payment: JMS 延迟消息(R-ORD-04 支付超时优化,PaymentExpiryCheckConsumer @JmsListener)
  implementation(libs.spring.boot.starter.artemis)

  // aqua-delivery: 微信小程序 & 支付
  implementation(libs.wechat.miniapp)

  // aqua-review: Redis
  implementation(libs.bundles.redis)
  implementation(libs.bundles.cache)

  // ========================================================
  // 测试依赖
  // ========================================================
  testImplementation(libs.spring.boot.starter.test)
  // 仓储实现测试用 Testcontainers 真实 PG(aqua.kotlin.exposed 不带 test 依赖;
  // 不用 H2 —— ON CONFLICT/jsonString/类型绑定等 PG 方言差异 H2 测不出)。
  // 容器运行时接线与共享 PG 基建见 aqua.testcontainers 插件 + foundation testFixtures。
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
}

// QueryDSL 生成源码目录
// Q 类由 querydsl-ksp 生成到 build/generated/ksp/main/kotlin，由 KSP 插件自动加入 source set
