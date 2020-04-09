//________________________________________________________________________________
// BUILD SCRIPT
//________________________________________________________________________________
buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.3.70"))
  }
}

plugins {
  kotlin("jvm") version "1.3.70"
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

//________________________________________________________________________________
// VERSIONS
//________________________________________________________________________________
val flinkVersion by extra("1.8.2")
val flinkScalaRuntime by extra { "2.12" }
val guavaVersion by extra { "28.0-jre" }
val juniperVersion by extra { "5.4.0" }
val jacksonVersion by extra { "2.10.3"}
val kotlinVersion by extra { "1.3.70"}
val kotlinCoroutinesVersion by extra { "1.3.2"}
val koinVersion by extra { "2.1.5"}
val vertxVersion by extra {"3.9.0"}

//________________________________________________________________________________
// GLOBAL
//________________________________________________________________________________
allprojects {


  tasks.register("hello") {
    doLast {
      println("I'm ${this.project.name}")
    }
  }

  repositories {
    mavenCentral()
    jcenter()
  }

  dependencies {
  }

  tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions.jvmTarget = "1.8"
    }
  }
}

