plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    implementation(project(":modules:user-module"))
    implementation(project(":modules:product-module"))
    
    // Order-specific dependencies can be added here
}
