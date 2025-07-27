plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  // Kotlin standard library
  implementation("org.jetbrains.kotlin:kotlin-stdlib")
  // ByteBuddy for method tracing
  implementation("net.bytebuddy:byte-buddy:1.14.5")
  implementation("net.bytebuddy:byte-buddy-agent:1.14.5")
  
  // JUnit for testing and core functionality
  implementation("junit:junit:4.13.2")
}

