plugins {
  id("aqua.spring.boot.library")
  id("aqua.kotlin.exposed")
  // Testcontainers 真实 PG 测试(容器运行时接线见 build-logic/aqua.testcontainers.gradle.kts)
  id("aqua.testcontainers")
}

group = "dev.yidafu.aqua"
version = "1.0.0"

// 排除与 aqua-analytics 相同的 snakeyaml 冲突
configurations.all {
  exclude(group = "org.yaml", module = "snakeyaml")
}

dependencies {
  // ========================================================
  // 核心依赖
  // ========================================================
  implementation(project(":modules:aqua-foundation"))

  // ========================================================
  // Spring Boot（web + JPA）
  // ========================================================
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.spring.boot.starter.data.jpa)
  // storage 控制器的 @PreAuthorize 注解来自 spring-security-core
  implementation("org.springframework.security:spring-security-core")
  runtimeOnly(libs.postgresql)

  // ========================================================
  // storage 第三方库（图片处理 / 文件类型检测 / 云存储）
  // ========================================================
  implementation("net.coobird:thumbnailator:0.4.20")
  implementation("org.apache.tika:tika-core:2.9.1")
  implementation("org.apache.tika:tika-parsers-standard-package:2.9.1") {
    // 排除已 EOL 的 Xerces 2.x：它会覆盖 JDK 内置 JAXP 解析器，
    // 导致 Liquibase 无法启用 accessExternalSchema 安全属性（SAXNotRecognizedException），
    // 并携带已知 CVE（如 CVE-2022-23437）。移除后回退到 JDK 自带解析器。
    exclude(group = "xerces", module = "xercesImpl")
    exclude(group = "xml-apis", module = "xml-apis")
  }
  implementation("software.amazon.awssdk:s3:2.20.26")
  implementation("com.aliyun.oss:aliyun-sdk-oss:3.17.0")

  // ========================================================
  // 通知域第三方库(PRD 13-notification.md F-NTF-003)
  // spring-jms 提供 @JmsListener(notifications-events 队列消费 + push-events 主题分发);
  // spring-websocket 提供客户端实时推送端点(WebSocket 直连,替代 MQTT)
  // ========================================================
  implementation("org.springframework:spring-jms")
  implementation("jakarta.jms:jakarta.jms-api")
  implementation("org.springframework.boot:spring-boot-starter-websocket")

  // ========================================================
  // 测试依赖
  // ========================================================
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
  testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}
