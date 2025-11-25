plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    
    // User-specific dependencies
    implementation(libs.spring.boot.starter.security)
}
