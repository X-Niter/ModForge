plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.modforge"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // WebSocket client for real-time collaboration
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // HTTP client for API calls
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3")
    type.set("IC") // Target IDE Platform: IntelliJ IDEA Community Edition
    
    plugins.set(listOf(
        "com.intellij.java", // Java plugin for Java support
        "org.jetbrains.kotlin" // Kotlin plugin for Kotlin support
    ))
}

tasks {
    // Set the JVM compatibility version
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    patchPluginXml {
        sinceBuild.set("231") // Minimum IntelliJ version: 2023.1
        untilBuild.set("")    // No maximum version
        
        // Extract version from plugin.xml
        version.set(project.version.toString())
        
        // Plugin description from plugin.xml
        pluginDescription.set("""
            ModForge - AI-powered Minecraft mod development platform.
            
            Features:
            - Autonomous code generation for Minecraft mods
            - Intelligent error fixing
            - Real-time collaborative editing
            - Cross-loader mod development support
            - API cost reduction through pattern recognition
            
            ModForge helps you create, test, and improve Minecraft mods efficiently by leveraging AI and collaborative tools.
        """.trimIndent())
        
        // Change notes for the latest version
        changeNotes.set("""
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release</li>
                <li>AI-powered code generation</li>
                <li>Autonomous error fixing</li>
                <li>Collaborative editing</li>
                <li>Pattern recognition for reduced API costs</li>
                <li>Cross-loader mod development support</li>
            </ul>
        """.trimIndent())
    }
    
    // Sign the plugin for distribution
    signPlugin {
        // Keystore parameters can be set through environment variables
        certificateChain.set(System.getenv("INTELLIJ_CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("INTELLIJ_PRIVATE_KEY"))
        password.set(System.getenv("INTELLIJ_PRIVATE_KEY_PASSWORD"))
    }
    
    // Publish the plugin to JetBrains Marketplace
    publishPlugin {
        token.set(System.getenv("INTELLIJ_PUBLISH_TOKEN"))
    }
    
    // Run IntelliJ with the plugin
    runIde {
        // Increase memory for IntelliJ
        jvmArgs = listOf("-Xmx2048m")
    }
}