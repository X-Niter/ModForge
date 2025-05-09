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

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- The OpenAI team for their powerful language models
- The Minecraft modding community for inspiration and testing
- All contributors to this project

---

For additional support, please create a GitHub issue or contact the repository maintainer.