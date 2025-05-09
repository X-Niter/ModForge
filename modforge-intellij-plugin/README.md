# ModForge IntelliJ Plugin

An advanced AI-powered Minecraft mod development platform that leverages intelligent automation and collaborative technologies to streamline mod creation across multiple platforms.

## Features

- **AI-assisted code generation** with OpenAI integration
- **Continuous development mode** that automatically fixes errors
- **Pattern recognition** to reduce API costs
- **Documentation generation** and code explanation
- **Support for multiple mod loaders** (Forge, Fabric, Quilt)
- **Cross-platform mod development** and compatibility tools

## Continuous Development Setup

The ModForge project is designed to run autonomously with continuous development capabilities. The system can:

1. **Fix errors automatically** - Detects and fixes compilation errors without human intervention
2. **Improve code quality** - Refactors code to follow best practices
3. **Generate documentation** - Adds comprehensive JavaDoc to classes and methods
4. **Add new features** - Implements small features based on repo analysis

### How to Use Continuous Development

The project uses GitHub Actions for continuous development. Here's how to use it:

#### Automatic Scheduled Development

The system automatically runs every 6 hours to:
- Check for compilation errors and fix them
- Make small improvements to code quality
- Add documentation where needed

No action is required on your part - the system will commit changes automatically.

#### Manual Trigger

You can manually trigger the development workflow:

1. Go to the "Actions" tab in your GitHub repository
2. Select the "Continuous Development" workflow
3. Click "Run workflow"
4. Choose the task type: fix-errors, improve-code, generate-docs, add-feature
5. Click "Run workflow"

#### Issue Commands

You can create issues with special commands to request specific changes:

1. Create a new issue
2. Include a command in the issue body using this format:
   ```
   /command [action] [target] [parameters]
   ```

Examples:
- `/command fix error in GenerateCodeAction.java`
- `/command add feature "Dark mode support" to the UI components`
- `/command improve AIAssistPanel.java`
- `/command document SettingsPanel.java`

The system will process your command, make the changes, and comment on the issue when done.

## Setting Up the Repository

### Prerequisites

- GitHub Account
- GitHub Repository for the project
- OpenAI API Key (for AI features)

### Repository Setup

1. Push the code to your GitHub repository
2. Set up the required secret:
   - Go to Settings > Secrets and variables > Actions
   - Add a new repository secret named `OPENAI_API_KEY` with your OpenAI API key

### Installation in IntelliJ IDEA

To install the plugin in your IntelliJ IDEA:

1. Clone the repository
2. Open the project in IntelliJ IDEA
3. Run `./gradlew buildPlugin`
4. Find the built plugin in `build/distributions/`
5. Install the plugin in IntelliJ IDEA by going to Settings > Plugins > ⚙️ > Install Plugin from Disk

## Development Guidelines

### Code Style

- Follow IntelliJ platform development best practices
- Use proper JavaDoc documentation
- Follow the existing architectural patterns

### Testing

- Write unit tests for new features
- Run the existing test suite before submitting changes

### Contributions

Contributions are welcome! Please create an issue first to discuss your proposed changes.

## License

This project is licensed under the terms specified in the LICENSE file.

## Contact

For questions or support, please open an issue on the GitHub repository.