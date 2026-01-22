plugins {
	java
	id("org.springframework.boot") version "3.5.10-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.yuuki.demo"
version = "0.0.1-SNAPSHOT"
description = "concurrency control test"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	// 롬복 라이브러리 추가
	compileOnly("org.projectlombok:lombok")
	// 컴파일 시점에 코드를 생성해주는 프로세서 (핵심!)
	annotationProcessor("org.projectlombok:lombok")

	// 테스트 코드에서도 롬복을 사용하려면 아래 두 줄도 필요합니다
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-web")

	runtimeOnly("com.mysql:mysql-connector-j")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
