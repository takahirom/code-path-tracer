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

// JAR manifest configuration for runtime agent usage
// Note: No Premain-Class needed as we use ByteBuddyAgent.install() at runtime
tasks.jar {
  manifest {
    attributes(
      "Can-Retransform-Classes" to "true",  
      "Can-Redefine-Classes" to "true"
    )
  }
}

// Test JVM configuration with memory dump settings
tasks.test {
  jvmArgs(
    "-Xmx2g",
    "-XX:MaxMetaspaceSize=512m",
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:HeapDumpPath=${layout.buildDirectory.dir("heap-dumps").get().asFile.absolutePath}/",
    "-Xlog:gc*:file=${layout.buildDirectory.file("gc.log").get().asFile.absolutePath}:time"
  )
}
