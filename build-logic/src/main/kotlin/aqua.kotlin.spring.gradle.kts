plugins {
    id("aqua.kotlin.common")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.1"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
