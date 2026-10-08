plugins {
    id("aqua.kotlin.jpa")
    id("aqua.kotlin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Configure bootJar task
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
    // mainClass 由各服务自行声明(services/*/build.gradle.kts 的 springBoot { mainClass }),
    // 不再在此硬编码,否则会覆盖服务级配置导致 java -jar 启动失败。
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
}
