import com.github.gradle.node.npm.task.NpmTask

plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.0.2"
}

group = "com.gruppe2"
version = "0.0.1-SNAPSHOT"
description = "FeedApp project"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.h2database:h2:2.3.232")
	implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.hibernate.orm:hibernate-core:7.1.1.Final")


	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.apache.kafka:kafka-clients:3.7.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

node {
    version.set("22.12.0")
    npmVersion.set("")
    download.set(true)
    nodeProjectDir.set(file("../frontend"))
}

tasks.register<NpmTask>("npmBuildFrontend") {
    dependsOn("npmInstall")
    args.set(listOf("run", "build"))
    workingDir.set(file("../frontend"))
}

tasks.register<Copy>("copyWebApp") {
    dependsOn("npmBuildFrontend")
    from("../frontend/dist")
    into("src/main/resources/static")
}

tasks.register("buildFrontend") {
    dependsOn("copyWebApp")
}

tasks.named("bootJar") {
    dependsOn("buildFrontend")
}

tasks.named("bootRun") {
    dependsOn("buildFrontend")
}

tasks.named("processResources") {
    dependsOn("copyWebApp")
}