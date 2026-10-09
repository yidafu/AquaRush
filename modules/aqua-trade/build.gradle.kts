plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P4:两插件暂共存 —— order/payment/review 的 QueryDSL Impl
  // 全量迁到 Exposed 后摘除 querydsl(与 marketing/product 迁移节奏一致)。
  id("aqua.kotlin.querydsl")
  id("aqua.kotlin.exposed")
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
  // 不用 H2 —— ON CONFLICT/jsonString/类型绑定等 PG 方言差异 H2 测不出)
  testImplementation(libs.testcontainers.core)
  testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test>().configureEach {
  // Testcontainers 连 podman(本机无 Docker Desktop):未显式设置 DOCKER_HOST 且
  // podman machine socket 存在时回退到它;CI/其他机器用 Docker 时不受影响。
  val podmanSocket =
    file("${System.getProperty("user.home")}/.local/share/containers/podman/machine/podman.sock")
  if (System.getenv("DOCKER_HOST").isNullOrBlank() && podmanSocket.exists()) {
    val host = "unix://${podmanSocket.absolutePath}"
    environment("DOCKER_HOST", host)
    systemProperty("docker.host", host)
    // ryuk 要把宿主 socket 挂进清理容器,macOS 宿主路径在 podman VM 内 statfs 不支持
    // (operation not supported)→ podman 环境禁用 ryuk,容器由 JVM shutdown hook 清理。
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
  }
}

// QueryDSL 生成源码目录
// Q 类由 querydsl-ksp 生成到 build/generated/ksp/main/kotlin，由 KSP 插件自动加入 source set
