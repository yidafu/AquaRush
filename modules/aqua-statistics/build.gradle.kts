plugins {
  id("aqua.spring.boot.library")
}

dependencies {
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-order"))
  implementation(project(":modules:aqua-delivery"))
  implementation(project(":modules:aqua-payment"))

  // Statistics-specific dependencies can be added here
}
