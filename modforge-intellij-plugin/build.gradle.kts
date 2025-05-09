import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.modforge"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2")
    type.set("IC") // Target IDE Platform: IntelliJ IDEA Community Edition

    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.plugins.gradle",
        "org.intellij.intelliLang"
    ))
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-io:commons-io:2.13.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    test {
        useJUnitPlatform()
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
        
        changeNotes.set("""
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release of ModForge IntelliJ plugin</li>
                <li>AI-driven code generation and refactoring</li>
                <li>Continuous development with automatic error fixing</li>
                <li>Documentation generation</li>
                <li>Pattern recognition to reduce API usage</li>
                <li>Support for multiple mod loaders including Forge, Fabric, and Quilt</li>
            </ul>
        """)
    }

    publishPlugin {
        token.set(System.getenv("INTELLIJ_PUBLISH_TOKEN"))
        channels.set(listOf("stable"))
    }
    
    buildSearchableOptions {
        enabled = false
    }
    
    runIde {
        // Configure JVM arguments for the IDE instance
        jvmArgs("-Xmx2g")
    }
    
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    
    prepareSandbox {
        // Extract any additional files to the sandbox directory
        from("src/main/resources") {
            into("${intellij.pluginName.get()}/resources")
            include("**/*")
        }
    }
}