plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.vanniktech.maven.publish")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(libs.kotlinStdlib)
  implementation(libs.byteBuddy)
  implementation(libs.byteBuddyAgent)
  compileOnly(libs.junit)
  
  testImplementation(libs.junit)
}
