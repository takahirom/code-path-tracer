plugins {
  id("org.jetbrains.kotlin.jvm")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib")
  implementation("net.bytebuddy:byte-buddy:1.14.5")
  implementation("net.bytebuddy:byte-buddy-agent:1.14.5")
  implementation("junit:junit:4.13.2")
}

