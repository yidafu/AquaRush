plugins {
  id("aqua.spring.boot.library")
  id("aqua.kotlin.querydsl")
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
  runtimeOnly(libs.postgresql)

  // ========================================================
  // storage 第三方库（图片处理 / 文件类型检测 / 云存储）
  // ========================================================
  implementation("net.coobird:thumbnailator:0.4.20")
  implementation("org.apache.tika:tika-core:2.9.1")
  implementation("org.apache.tika:tika-parsers-standard-package:2.9.1")
  implementation("software.amazon.awssdk:s3:2.20.26")
  implementation("com.aliyun.oss:aliyun-sdk-oss:3.17.0")

  // ========================================================
  // 测试依赖
  // ========================================================
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

// QueryDSL 生成源码目录
// Q 类由 querydsl-ksp 生成到 build/generated/ksp/main/kotlin，由 KSP 插件自动加入 source set
