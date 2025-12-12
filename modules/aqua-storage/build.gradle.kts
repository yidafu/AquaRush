plugins {
  id("aqua.kotlin.spring")
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-common"))

  // Spring Boot starters
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // 图片处理库
  implementation("net.coobird:thumbnailator:0.4.20")

  // 文件类型识别库
  implementation("org.apache.tika:tika-core:2.9.1")
  implementation("org.apache.tika:tika-parsers-standard-package:2.9.1")

  // AWS S3 支持
  implementation("software.amazon.awssdk:s3:2.20.26")

  // 阿里云 OSS 支持
  implementation("com.aliyun.oss:aliyun-sdk-oss:3.17.0")

  // 测试依赖
  testImplementation(libs.spring.boot.starter.test)
  testImplementation("io.mockk:mockk:1.13.10")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

// Configure QueryDSL
val querydslDir = "$buildDir/generated/querydsl"

querydsl {
  jpa = true
  hibernate = true
  querydslSourcesDir = querydslDir
}

// Configure Kotlin compilation to include generated source
kotlin {
  sourceSets {
    main {
      // Add QueryDSL generated sources
      kotlin.srcDir(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
    }
  }
}
