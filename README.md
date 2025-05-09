# ModForge - Autonomous Minecraft Mod Development System

ModForge is a revolutionary AI-powered Minecraft mod development platform that democratizes mod creation through intelligent technologies and collaborative tools. The system automatically generates, tests, improves, and maintains Minecraft mods with minimal user intervention.

## Core Features

- **AI-Driven Development**: Uses OpenAI's GPT-4 to generate, fix, and improve mod code
- **Multi-Loader Support**: Works with Forge, Fabric, and Quilt mod loaders
- **Continuous Self-Improvement**: Autonomous workflows run 24/7 to test and enhance mods
- **Pattern Learning**: Reduces API costs by recognizing and reusing common patterns
- **Full GitHub Integration**: Manages issues, pull requests, and conversations autonomously
- **Cross-Loader Compatibility**: Built-in support for Architectury API for cross-platform mods
- **IntelliJ Plugin Integration**: Seamless two-way synchronization with the IDE

## System Architecture

The system consists of several integrated components:

1. **Web Application**: Front-end interface for users to interact with the system
2. **API Server**: Handles requests between the frontend and backend services
3. **AI Services**: Pattern learning and OpenAI integration for code generation
4. **GitHub Workflows**: Autonomous CI/CD, issue management, and development
5. **IntelliJ Plugin**: Direct IDE integration for professional developers

## GitHub Workflow System

The autonomous GitHub workflow system is the heart of the continuous development capability. It consists of several workflow files:

- `autonomous-controller.yml`: Central coordinator for all autonomous operations
- `autonomous-scanning.yml`: Scans repositories for issues that need attention
- `conversation-handler.yml`: Manages GitHub issue conversations with natural language
- `pull-request-review.yml`: Reviews and improves pull requests
- `dependency-update.yml`: Automatically updates dependencies
- `continuous-development.yml`: Continuously compiles and improves mods
- `monitor-and-dispatch.yml`: Monitors system health and dispatches tasks

## Setup Instructions

### Requirements

- GitHub repository with appropriate permissions
- OpenAI API key with access to GPT-4
- Java Development Kit (JDK) 17 or newer
- Gradle 7.5+
- PostgreSQL database (for pattern learning)

### Configuration

1. **Set up GitHub Secrets**:
   - `OPENAI_API_KEY`: Your OpenAI API key
   - `GH_TOKEN`: GitHub token with repo and workflow permissions
   - `DATABASE_URL`: PostgreSQL connection string (if using pattern learning)

2. **Enable GitHub Workflows**:
   - Ensure Actions are enabled in your repository settings
   - Push all workflow files to the `.github/workflows` directory

3. **Database Setup** (for pattern learning):
   - Create PostgreSQL database
   - Run the schema creation scripts from `scripts/`
   - Update database connection details in configuration

## Usage Guide

### Starting a New Mod

1. Create a new GitHub issue with the title format: "Mod Idea: [Your Mod Title]"
2. Describe your mod concept in the issue body
3. The autonomous system will respond with questions and suggestions
4. Once requirements are clear, the system will create and set up the mod project

### Development Commands

In GitHub issues, you can use the following commands:

- `/help`: Shows available commands
- `/fix [file]`: Fixes issues in a specific file
- `/improve [file]`: Improves code quality in a file
- `/add "[feature]" to [file]`: Adds a new feature to a file
- `/implement [idea numbers]`: Implements specific ideas from a brainstorming session
- `/document [file]`: Adds comprehensive documentation to a file
- `/analyze [topic]`: Analyzes a specific aspect of the codebase

### Monitoring Progress

- Check the Actions tab in GitHub to monitor workflow executions
- Active issues will receive updates as progress is made
- The system will create pull requests for significant changes

## Troubleshooting

### Common Workflow Errors

1. **YAML Syntax Errors**:
   - Check for proper indentation in workflow files
   - Ensure Python code blocks have consistent indentation
   - Verify that EOF delimiters match the indentation of their opening commands

2. **JSON Serialization Issues**:
   - Replace JavaScript's `Infinity` with large numeric values (e.g., 9999999)
   - Avoid using `NaN` values in any JSON data

3. **Permission Errors**:
   - Verify the GitHub token has sufficient permissions
   - Check repository settings to ensure workflows can create pull requests

4. **API Rate Limiting**:
   - Implement exponential backoff strategies
   - Use pattern learning to reduce OpenAI API calls
   - Consider upgrading API usage tiers

## Extending the System

### Adding New Features

To extend the system with new capabilities:

1. Define the feature requirements
2. Create appropriate workflow files or modify existing ones
3. Update the pattern learning system to recognize new patterns
4. Test thoroughly before deploying to production

### Integrating New AI Models

The system is designed to work with OpenAI's GPT-4 by default, but can be adapted to use other models:

1. Modify the API client code in relevant files
2. Update prompt templates to work with the new model
3. Adjust token limits and response handling
4. Test extensively to ensure quality of generated code

## Future Development Roadmap

1. **Enhanced Pattern Learning**: More sophisticated pattern matching and learning algorithms
2. **Multi-Model AI Support**: Integration with additional AI models for specialized tasks
3. **Advanced Conversation Handling**: More natural and context-aware GitHub conversations
4. **Improved Code Generation**: Better code quality and documentation generation
5. **Extended IDE Integration**: Support for additional IDEs beyond IntelliJ

## IntelliJ Plugin Setup & Usage Guide

The ModForge system includes an IntelliJ plugin that provides direct IDE integration for a seamless development experience. This guide explains how to set up and use the plugin effectively.

### Installation

1. **Build the Plugin**:
   ```bash
   cd modforge-intellij-plugin
   ./gradlew buildPlugin
   ```
   This will generate a plugin JAR file in `build/distributions/`.

2. **Install in IntelliJ IDEA**:
   - Open IntelliJ IDEA
   - Go to File → Settings → Plugins
   - Click the gear icon and select "Install Plugin from Disk..."
   - Navigate to the generated JAR file and install it
   - Restart IntelliJ when prompted

3. **Configuration**:
   - Open the ModForge settings panel in IntelliJ (Settings → Tools → ModForge)
   - Enter your GitHub credentials and API token
   - Set the path to your local repository clone
   - Enter your OpenAI API key

### Key Features

1. **AI Assistant Panel**:
   - Access via View → Tool Windows → ModForge Assistant
   - Ask questions about your code
   - Request code improvements
   - Get documentation help

2. **Two-Way Synchronization**:
   - Changes made in the IDE are automatically pushed to GitHub
   - Changes made through the web interface are pulled to the IDE
   - Real-time conflict resolution

3. **Context-Aware Actions**:
   - Right-click on a file or code selection to access ModForge actions
   - Options include: Improve Code, Add Feature, Document, Fix Issues

4. **Command Reference**:
   The following commands can be used in the AI Assistant panel:

   ```
   /help - Show available commands
   /fix [file] - Fix bugs or issues in a file
   /improve [file] - Improve code quality of a file
   /add "[feature]" to [file] - Add a new feature to a file
   /document [file] - Add comprehensive documentation
   /analyze [topic] - Analyze code or architecture
   /explain [file] - Get a detailed explanation of code
   ```

5. **Mod Testing**:
   - Run Minecraft with your mod directly from the IDE
   - Access mod testing logs and error reports
   - Integrated debugging with the autonomous system

### Workflow Integration

1. **Starting a New Mod**:
   - Create a new mod via File → New → Minecraft Mod Project
   - Select mod loader, Minecraft version, and other settings
   - The plugin automatically sets up the project structure

2. **Working with Continuous Development**:
   - Enable continuous development via Tools → ModForge → Enable Continuous Development
   - The system will monitor your code for errors, suggest improvements, and perform optimizations
   - Review suggested changes in the ModForge panel

3. **Collaboration**:
   - Share mods with team members via the ModForge panel
   - View all active issues and PRs directly in the IDE
   - Respond to comments and reviews without leaving the IDE

### Troubleshooting

1. **Connection Issues**:
   - Verify your GitHub token has the correct permissions
   - Check network connectivity to both GitHub and OpenAI
   - Validate your API keys in the settings panel

2. **Plugin Errors**:
   - Check the IntelliJ logs at Help → Show Log in Explorer/Finder
   - Try disabling and re-enabling the plugin
   - Update to the latest plugin version

3. **Synchronization Problems**:
   - Use Tools → ModForge → Force Synchronize to reset the connection
   - Manually pull changes if automatic sync fails
   - Check for merge conflicts in the Git panel

### Running the Plugin in Development Mode

For developers working on the plugin itself, you can run it directly from the source code:

1. **Prerequisites**:
   - IntelliJ IDEA (Community or Ultimate)
   - Java Development Kit (JDK) 17 or later
   - Gradle 7.5+

2. **Import the Plugin Project**:
   ```bash
   cd modforge-intellij-plugin
   ```
   Open the project in IntelliJ IDEA by selecting the directory.

3. **Configure Plugin SDK**:
   - Go to File → Project Structure → Project
   - Set the Project SDK to JDK 17 or higher
   - Set the Project Language Level to 17

4. **Run/Debug the Plugin**:
   - Configure a Plugin run configuration via Run → Edit Configurations
   - Add a new Gradle configuration with the following settings:
     - Gradle project: `modforge-intellij-plugin`
     - Tasks: `runIde`
   - Run this configuration to start an instance of IntelliJ with the plugin installed

5. **Hot Reload**:
   When making changes to the plugin code, you can use hot reload for some changes:
   - Make your code changes
   - Select Build → Build Project
   - In the running development IDE instance, go to File → Invalidate Caches / Restart
   - Select "Invalidate and Restart"

6. **Debugging**:
   - Set breakpoints in your plugin code
   - Start the plugin in debug mode via Run → Debug
   - The debugger will connect to the development IDE instance
   - You can now step through your code, inspect variables, etc.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- The OpenAI team for their powerful language models
- The Minecraft modding community for inspiration and testing
- All contributors to this project

---

For additional support, please create a GitHub issue or contact the repository maintainer.