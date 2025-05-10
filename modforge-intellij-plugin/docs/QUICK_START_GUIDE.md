# ModForge IntelliJ Plugin: Quick Start Guide

This guide will help you get started with the ModForge IntelliJ Plugin quickly and efficiently.

## Installation

### Prerequisites

Before installing the ModForge plugin, ensure you have:

- IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) or later
- Java Development Kit (JDK) 21.0.6 or later
- Minecraft Development plugin installed

### Installation Steps

1. Download the ModForge plugin ZIP file from the release page or build it using `./build-production-plugin.sh`
2. Open IntelliJ IDEA
3. Go to **File → Settings → Plugins**
4. Click the **⚙️** (gear) icon and select **Install Plugin from Disk...**
5. Select the downloaded ZIP file
6. Click **Install**
7. Restart IntelliJ IDEA when prompted

## Initial Setup

### API Configuration

1. After restarting IntelliJ IDEA, go to **File → Settings → Tools → ModForge AI Settings**
2. Enter your OpenAI API key (required for AI features)
3. Optionally enter your GitHub token (required for GitHub integration)
4. Click **Test Connection** to verify your API keys are working
5. Click **Apply** then **OK**

## Creating Your First Mod

### Using a Template

1. Go to **File → New → ModForge → Minecraft Mod**
2. Select the mod loader you want to use (Forge, Fabric, Quilt, or Architectury)
3. Enter basic mod information:
   - Mod Name: A display name for your mod
   - Mod ID: A unique identifier (lowercase, no spaces)
   - Package Name: Java package structure (e.g., com.example.mymod)
   - Mod Version: Starting version (e.g., 1.0.0)
   - Minecraft Version: Target Minecraft version
4. Click **Create** to generate the mod template

### Generating Code with AI

1. Open a Java file in your mod project
2. Place your cursor where you want to insert code
3. Right-click and select **Generate Code with ModForge AI**
4. In the popup dialog, describe what you want to generate:
   - Example: "Create a custom sword item that deals fire damage"
5. Click **Generate**
6. Review the generated code and click **Insert** if satisfied

### Fixing Compilation Errors

1. If your code has compilation errors, navigate to the error in the editor
2. Right-click on the error marker and select **Fix Errors with ModForge AI**
3. The plugin will analyze the error and suggest fixes
4. Review the suggested fix and click **Apply** if satisfied

## Setting Up Continuous Development

1. With your mod project open, go to **Tools → ModForge AI → Toggle Continuous Development**
2. In the dialog, configure the continuous development settings:
   - Development Frequency: How often the AI should check your code
   - Feature Types: What kinds of improvements to make
3. Click **Start** to begin continuous development
4. The plugin will now periodically analyze your code and suggest improvements

## GitHub Integration

1. Go to **Tools → ModForge AI → Push to GitHub**
2. Enter your repository details:
   - Repository Name: Name for your GitHub repository
   - Description: Brief description of your mod
   - Visibility: Public or Private
3. Click **Create Repository** if it's a new repository, or **Push** if it already exists
4. The plugin will create/update the repository and push your code

## Using the ModForge Tool Window

1. Open the ModForge tool window by clicking the ModForge icon in the right toolbar
2. The tool window has several tabs:
   - **Dashboard**: Overview of your mod and recent activities
   - **Generation**: Interface for generating code with AI
   - **Errors**: List of errors in your project that can be fixed
   - **Continuous**: Controls for continuous development
   - **GitHub**: GitHub integration tools
3. Use the appropriate tab for the task you want to perform

## Getting Help

If you encounter any issues:

1. Check the IntelliJ IDEA log (**Help → Show Log in Explorer/Finder**)
2. Verify your API keys are correctly configured
3. Make sure you're using compatible versions of IntelliJ IDEA and Java
4. Consult the full documentation or contact support if needed

---

For more detailed information, refer to the main [README.md](../README.md) and [INTELLIJ_2025_COMPATIBILITY.md](../INTELLIJ_2025_COMPATIBILITY.md) files.