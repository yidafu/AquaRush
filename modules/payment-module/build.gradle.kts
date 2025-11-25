plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    implementation(project(":modules:order-module"))

    // WeChat Pay SDK - temporarily commented out until dependency is verified
    // implementation(libs.wechatpay.sdk)
}
