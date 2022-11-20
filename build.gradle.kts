import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.3"
	id("io.spring.dependency-management") version "1.0.13.RELEASE"
	`maven-publish`
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "org.adaptable"
version = "1.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val kotestVersion = "5.4.2"
val mockitoKotlinVersion = "3.2.0"
repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":common"))
	implementation(project(":common-web"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
	implementation("io.github.classgraph:classgraph:4.8.149")
	implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
	implementation("io.reactivex.rxjava3:rxjava:3.1.5")

	testImplementation(project(":expression"))
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
	testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
	testImplementation("io.kotest:kotest-property:$kotestVersion")
	testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
	enabled = true
	archiveClassifier.set("")
}


configure<PublishingExtension> {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])
			groupId = "org.adaptable"
			artifactId = "agent"
			version = "1.0.1-SNAPSHOT"
		}
	}
}

tasks.register("prepareKotlinBuildScriptModel"){}