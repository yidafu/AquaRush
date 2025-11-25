plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))

    // Add Spring Web dependency for REST controllers
    implementation("org.springframework.boot:spring-boot-starter-web")
}
