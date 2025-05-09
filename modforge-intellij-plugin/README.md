# ModForge IntelliJ Plugin

An advanced AI-powered Minecraft mod development platform that leverages intelligent automation and collaborative technologies to streamline mod creation across multiple platforms.

## Features

- **AI-assisted code generation** with OpenAI integration
- **Continuous development mode** that automatically fixes errors
- **Pattern recognition** to reduce API costs
- **Documentation generation** and code explanation
- **Support for multiple mod loaders** (Forge, Fabric, Quilt)
- **Cross-platform mod development** and compatibility tools

## True 24/7 Autonomous Development System

ModForge is a fully autonomous software development platform that continuously operates while you sleep. The system:

1. **Never stops working** - Operates 24/7 without needing any human intervention
2. **Self-identifies problems** - Actively scans the codebase for errors and improvement opportunities
3. **Self-repairs code** - Diagnoses and fixes issues automatically, creating PRs with solutions
4. **Maintains conversations** - Communicates naturally through GitHub interfaces and follows up on stale conversations
5. **Improves itself** - Learns from past fixes and patterns to become more efficient over time
6. **Makes decisions autonomously** - Determines priorities and actions without human guidance
7. **Stays accountable** - Creates trackable issues and PRs for all changes and improvements
8. **Explains everything** - Uses simple, easy-to-understand language in all communications

### Continuous Autonomous Operation

#### True Non-Stop Development

The central autonomous controller works continuously without ever stopping:

- **Every minute**: Checks for any activity that needs attention
- **Built-in breather**: Takes a 1-minute pause after completing tasks
- **Intelligent coordination**: Avoids workflow conflicts by tracking active and completed tasks
- **Every 6 hours**: Performs deep code scanning to find improvement opportunities 
- **Every week**: Updates dependencies automatically with safety checks
- **After 2-5 days**: Follows up on stale conversations with a casual check-in

The system never sleeps - it's always working, always scanning, always ready to respond to any activity. When you wake up, you'll find issues resolved, code improved, and conversations continued - exactly as if a real developer had been working all night.

#### Talks Exactly Like a Real Developer

The system communicates in a casual, direct style just like the project creator would:

1. **No AI formality** - Uses plain, simple language without corporate-speak
2. **Gets to the point** - Avoids lengthy explanations in favor of direct answers
3. **Responds like a colleague** - Casual tone with occasional enthusiasm ("Nice!", "This looks great!")
4. **Shows code immediately** - Provides solutions right away without unnecessary explanation
5. **Keeps things short** - Uses bullet points and brief paragraphs instead of walls of text
6. **Low-key follow-ups** - Checks in on stale conversations with a quick, casual note

When you interact with the system, it's indistinguishable from chatting with a helpful but direct developer friend who just wants to get things done.

#### Advanced Command System

Use slash commands directly in issues or comments for immediate action:

```
/fix ClassName.java               # Fix issues in a specific file
/improve ComponentName            # Enhance code quality in a component
/add "feature X" to TargetClass   # Implement a new feature
/document ServiceClass            # Generate comprehensive documentation
/analyze error handling           # Deep-dive analysis of specific code
/explain how this code works      # Get a clear explanation of functionality
/test UserInterface               # Create or run tests for components
/implement all                    # Apply all suggested improvements 
/refactor DataStructure           # Restructure while preserving functionality
```

When you issue a command, the system:
1. Acknowledges the command and explains what it will do
2. Makes the requested changes in a new branch
3. Creates a pull request with detailed explanations
4. Self-tests the changes to ensure they work correctly
5. Reports back in the conversation with results and next steps

#### Self-Driven Improvement Cycle

The autonomous system operates in a continuous improvement loop:

1. **Monitoring phase**: Scans code, tests, conversations for any needs
2. **Analysis phase**: Identifies issues and improvement opportunities 
3. **Planning phase**: Determines the best approach to fix or enhance
4. **Implementation phase**: Makes changes with appropriate safeguards
5. **Verification phase**: Tests and validates all changes
6. **Documentation phase**: Creates clear explanations and documentation
7. **Communication phase**: Reports results and seeks feedback

This cycle continues indefinitely, ensuring the codebase constantly improves even without human input.

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