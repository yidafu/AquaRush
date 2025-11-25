plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    implementation(project(":modules:order-module"))
    implementation(project(":modules:user-module"))
    
    // Delivery-specific dependencies can be added here
}
