plugins {
    id("aqua.spring.boot.application")
}

dependencies {
    // Include all other modules
    implementation(project(":modules:common-module"))
    implementation(project(":modules:user-module"))
    implementation(project(":modules:product-module"))
    implementation(project(":modules:order-module"))
    implementation(project(":modules:delivery-module"))
    implementation(project(":modules:payment-module"))
    implementation(project(":modules:statistics-module"))
    
    // Entry module specific dependencies
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgresql)
}
