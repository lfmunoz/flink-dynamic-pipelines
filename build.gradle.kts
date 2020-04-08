import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPom

//val kotlinVersion = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion

//________________________________________________________________________________
// DEPENDENCY VERSIONS
//________________________________________________________________________________
val kotlinVersion = "1.3.70"
val kotlinCoroutinesVersion = "1.3.2"
val vertxVersion = "3.9.0"
val junitJupiterEngineVersion = "5.4.0"
val koinVersion = "2.1.5"
val jacksonVersion = "2.10.3"

project.group = "com.lfmunoz"
project.version = "1.0.0-SNAPSHOT"
val artifactID = "flink"

plugins {
  kotlin("jvm") version "1.3.70"
  `maven-publish`
  `java`
}

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  // MISC
  implementation("com.google.guava:guava:21.0")
  // LOGGING
  implementation("org.fissore:slf4j-fluent:0.12.0")
  implementation( "ch.qos.logback:logback-classic:1.2.3")
  implementation( "ch.qos.logback:logback-core:1.2.3")
  implementation( "org.codehaus.janino:janino:3.0.8")
  // KOIN
  implementation("org.koin:koin-core:$koinVersion")
  implementation("org.koin:koin-logger-slf4j:$koinVersion")
  testImplementation("org.koin:koin-test:$koinVersion")
  // KOTLIN
  implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
  implementation(kotlin("stdlib-jdk8", kotlinVersion))
  implementation(kotlin("reflect", kotlinVersion))
  // KOTLIN SCRIPT
  implementation(kotlin("script-runtime", kotlinVersion))
  implementation(kotlin("script-util", kotlinVersion))
  implementation(kotlin("compiler-embeddable", kotlinVersion))
  implementation(kotlin("scripting-compiler-embeddable", kotlinVersion))
  implementation(kotlin("script-util", kotlinVersion))
  implementation("net.java.dev.jna:jna:4.2.2")
  // JSON
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
  // VERTX
  implementation ("io.vertx:vertx-web-client:$vertxVersion")
  implementation ("io.vertx:vertx-junit5:$vertxVersion")
  implementation ("io.vertx:vertx-web:$vertxVersion")
  implementation ("io.vertx:vertx-lang-kotlin:$vertxVersion")
  implementation ("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")

  // TEST
  //   testImplementation 'org.awaitility:awaitility-kotlin:4.0.1'
//  testCompile("io.mockk:mockk:1.9")
  testImplementation("org.assertj:assertj-core:3.11.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterEngineVersion")
  testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterEngineVersion")
}


tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }
  withType(Test::class.java) {
    testLogging.showStandardStreams = true
    useJUnitPlatform()
  }

}
