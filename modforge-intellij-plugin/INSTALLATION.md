# ModForge IntelliJ Plugin Installation Guide

## Prerequisites
- IntelliJ IDEA (Community or Ultimate edition) 2023.2 or newer
- Java Development Kit (JDK) 17 or newer

## Method 1: Using the source code directly

### Step 1: Get the source code
Download the source code from this repository.

### Step 2: Create a new IntelliJ Platform Plugin project
1. Open IntelliJ IDEA
2. Go to File > New > Project...
3. Select "IntelliJ Platform Plugin" and click Next
4. Configure the project:
   - Name: ModForge
   - Location: Choose a location on your system
   - JDK: Select JDK 17 or newer
5. Click Finish

### Step 3: Replace the source code
1. Copy all files from the downloaded source code into your newly created project
2. Make sure to keep the correct directory structure:
   - `src/main/java/com/modforge/intellij/plugin` - Java source files
   - `src/main/resources/META-INF` - Plugin configuration

### Step 4: Build and run the plugin
1. In IntelliJ IDEA, go to the Gradle tool window
2. Run the `buildPlugin` task
3. The plugin will be built and available in `build/distributions/`
4. Use "Run Plugin" from the Gradle tasks to test the plugin

## Recommended IDE Configuration
In order to have the best development experience, make sure to:

1. Set Java language level to 17 in Project Structure > Project
2. Install "Plugin DevKit" and "Gradle" plugins if they are not already installed
3. Configure the IntelliJ Platform SDK in Project Structure > SDKs

## Features
- AI-assisted code generation with OpenAI integration
- Continuous development mode that automatically fixes errors
- Pattern recognition to reduce API costs
- Documentation generation and code explanation
- Support for multiple mod loaders (Forge, Fabric, Quilt)

## Troubleshooting
If you encounter any issues during installation or usage:
- Make sure you have the correct JDK version
- Ensure all dependencies are resolved correctly
- Check IntelliJ IDEA version compatibility
- Verify your OpenAI API key configuration (required for AI features)

For more detailed instructions or help, please contact the ModForge team.