# ModForge IntelliJ Plugin

An advanced AI-powered Minecraft mod development platform that leverages intelligent automation and collaborative technologies to streamline mod creation across multiple platforms.

## Features

- **AI-assisted code generation** with OpenAI integration
- **Continuous development mode** that automatically fixes errors
- **Pattern recognition** to reduce API costs
- **Documentation generation** and code explanation
- **Support for multiple mod loaders** (Forge, Fabric, Quilt)
- **Cross-platform mod development** and compatibility tools

## Autonomous Development Platform

ModForge is a fully autonomous software development platform. The system continuously operates to:

1. **Fix errors automatically** - Detects and fixes compilation errors without human intervention
2. **Improve code quality** - Refactors code to follow best practices
3. **Generate documentation** - Adds comprehensive JavaDoc to classes and methods
4. **Add new features** - Implements small features based on repo analysis
5. **Process pull requests** - Reviews and improves submitted code changes
6. **Visualize progress** - Dashboard with metrics and statistics on GitHub Pages

### Autonomous Workflows

#### Continuous Development

The system automatically runs every 6 hours to:
- Check for compilation errors and fix them
- Make small improvements to code quality
- Add documentation where needed

No action is required on your part - the system will commit changes automatically.

#### Manual Workflow Trigger

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

#### Pull Request Processing

When you submit a pull request, the system will:

1. Analyze the code changes
2. Suggest improvements
3. Automatically fix common issues
4. Add documentation where needed

All suggestions and changes will be provided as comments on your PR.

#### Web Dashboard

A detailed dashboard is automatically deployed to GitHub Pages, providing:

1. **Project Metrics** - Code size, class count, documentation coverage
2. **Automation Statistics** - AI-powered changes, fixes, improvements
3. **Command Center** - Interface for sending commands to the system
4. **Git History** - Timeline of project changes
5. **Code Analytics** - Quality metrics and structural analysis

Access it at `https://[your-username].github.io/[repo-name]/`

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