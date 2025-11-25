plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    implementation(project(":modules:order-module"))
    
    // WeChat Pay SDK
    implementation(libs.wechatpay.sdk)
}
