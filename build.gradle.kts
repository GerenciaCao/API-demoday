// Build da API do GerenciaCão (Spring Boot + Kotlin + MongoDB).
// Requer JDK 17 ou superior (recomendado 21). O Gradle 9.6.0 vem do wrapper (gradle/wrapper/gradle-wrapper.properties).
// ATENÇÃO: este é um projeto SEPARADO do app Android. Abra esta pasta sozinha, nunca dentro do projeto do app.

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20" // abre as classes anotadas do Spring (@Service, @Configuration...)
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.gerenciacao"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Web (controllers REST) — no Spring Boot 4 o starter se chama "webmvc"
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Validação dos corpos das requisições (@Valid, @NotBlank, @Email...)
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Segurança + validação de JWT (resource server, HS256)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    // MongoDB (Atlas)
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    // JSON com Kotlin (Jackson 3 no Spring Boot 4)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict") // respeita as anotações de nulidade do Spring
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
