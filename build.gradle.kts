import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.3"
	id("io.spring.dependency-management") version "1.0.13.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
	`maven-publish`
	signing
	id("org.jetbrains.dokka") version "1.4.20"
}

group = "io.github.markgregg"
version = "1.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val projectDescription = "Adaptable common web functionality"
val githubRepo = "markgregg/adaptable-agent"
val licenseUrl = "https://opensource.org/licenses/Apache-2.0"
val licenseName = "Apache 2"

val kotestVersion = "5.4.2"
val mockitoKotlinVersion = "3.2.0"
val classgraphVersion="4.8.149"
val rxkotlinVersion="3.0.1"
val rxjavaVersion="3.1.5"
val adaptableExpression = "1.0.0-SNAPSHOT"
val adaptableCommon = "1.0.0-SNAPSHOT"
val adaptableCommonWeb = "1.0.1-SNAPSHOT"

repositories {
	mavenCentral()
	maven {
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
	}
}

dependencies {
	implementation("io.github.markgregg:adaptable-common:$adaptableCommon")
	implementation("io.github.markgregg:adaptable-common-web:$adaptableCommonWeb")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.github.classgraph:classgraph:$classgraphVersion")
	implementation("io.reactivex.rxjava3:rxkotlin:$rxkotlinVersion")
	implementation("io.reactivex.rxjava3:rxjava:$rxjavaVersion")

	testImplementation("io.github.markgregg:adaptable-expression:$adaptableExpression")
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

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = project.group.toString()
			artifactId = project.name
			version = project.version.toString()
			from(components["kotlin"])
			pom {
				name.set(project.name)
				description.set(projectDescription)
				url.set("https://github.com/$githubRepo")
				licenses {
					license {
						name.set(licenseName)
						url.set(licenseUrl)
					}
				}
				developers {
					developer {
						id.set("markgregg")
						name.set("Mark Gregg")
					}
				}
				scm {
					url.set(
						"https://github.com/$githubRepo.git"
					)
					connection.set(
						"scm:git:git://github.com/$githubRepo.git"
					)
					developerConnection.set(
						"scm:git:git://github.com/$githubRepo.git"
					)
				}
				issueManagement {
					url.set("https://github.com/$githubRepo/issues")
				}
			}
		}
	}
	repositories {
		maven {
			name = "mavenStaging"
			url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
			credentials {
				username = providers.gradleProperty("ossrhUsername").orElse("Not found").get()
				password = providers.gradleProperty("ossrhPassword").orElse("Not found").get()
			}
		}
		maven {
			name = "mavenSnapshots"
			url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
			credentials {
				username = providers.gradleProperty("ossrhUsername").orElse("Not found").get()
				password = providers.gradleProperty("ossrhPassword").orElse("Not found").get()
			}
		}
	}

	signing {
		sign(publishing.publications["maven"])
	}
}
