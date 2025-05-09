# ModForge IntelliJ Plugin Architecture

This document describes the architecture of the ModForge IntelliJ plugin, which integrates ModForge's autonomous AI-powered Minecraft mod development capabilities directly into the IDE.

## Overview

The ModForge IntelliJ plugin is designed to make Minecraft mod development faster and more accessible through the following key features:

1. **AI-Assisted Development**: Generate code, fix errors, and create documentation using OpenAI integration
2. **Continuous Development**: Automatically detect and fix errors in real-time
3. **Pattern Recognition**: Reduce API costs by caching similar requests
4. **Multi-Loader Support**: Support for Forge, Fabric, and Quilt mod loaders

## Core Components

### Services

| Service | Purpose |
|---------|---------|
| `ModForgeProjectService` | Central service managing all project-level operations |
| `AutonomousCodeGenerationService` | Handles AI-powered code generation, error fixing, and documentation |
| `ContinuousDevelopmentService` | Provides background monitoring and automatic fixing of errors |
| `AIServiceManager` | Manages communication with the OpenAI API |
| `PatternRecognitionService` | Stores and matches patterns to reduce API calls |

### Listeners

| Listener | Purpose |
|----------|---------|
| `ModForgeCompilationListener` | Tracks compilation errors and issues |
| `ModForgeFileListener` | Monitors file changes to detect potential issues |

### User Interface

| Component | Purpose |
|-----------|---------|
| `ModForgeToolWindowFactory` | Creates the ModForge tool window |
| `AIAssistPanel` | UI panel for AI-assisted development |
| `MetricsPanel` | UI panel for displaying usage metrics |
| `ModForgeSettingsConfigurable` | Settings UI for configuring the plugin |

## Service Architecture

### ModForgeProjectService

This is the core service that coordinates all project-level activities:

```
ModForgeProjectService
├── AutonomousCodeGenerationService
├── ContinuousDevelopmentService
├── ModForgeFileListener
└── ModForgeCompilationListener
```

When a project is opened, ModForgeProjectService initializes all the necessary components and registers the listeners. It also handles lifecycle events such as project closing.

### AutonomousCodeGenerationService

This service manages AI-powered code generation with pattern recognition to minimize API usage:

```
AutonomousCodeGenerationService
├── AIServiceManager
└── PatternRecognitionService
```

It implements:
- Code generation from natural language prompts
- Error fixing based on compiler messages
- Documentation generation for existing code
- Feature addition to existing code
- Code explanation

### ContinuousDevelopmentService

This service runs in the background and periodically checks for compilation issues:

```
ContinuousDevelopmentService
└── ModForgeCompilationListener
```

When issues are detected, it attempts to fix them automatically using the AutonomousCodeGenerationService.

## Pattern Recognition System

The pattern recognition system is designed to reduce API costs by caching similar requests and their responses:

```
PatternRecognitionService
├── CodeGenerationPattern
├── ErrorFixPattern
├── DocumentationPattern
└── FeatureAdditionPattern
```

Each pattern stores:
- Input data (e.g., prompt, code, error message)
- Output data (e.g., generated code, fixed code)
- Usage count for least-recently-used eviction

When a request is made, the system first checks if there's a similar pattern with a similarity score above a threshold. If found, it returns the cached response instead of making an API call.

## Workflow

### Compilation Error Handling

1. User's code is compiled
2. `ModForgeCompilationListener` captures any errors
3. `ContinuousDevelopmentService` detects the errors
4. `AutonomousCodeGenerationService` attempts to fix the errors:
   - First checking for similar patterns in `PatternRecognitionService`
   - If no match, using `AIServiceManager` to get a fix from OpenAI
5. If successful, the fixed code is written back to the file

### Manual AI Assistance

1. User selects code in the editor
2. User chooses an action from the context menu (Generate, Fix, Document, Explain)
3. Request is passed to `AutonomousCodeGenerationService`
4. Result is either:
   - Written back to the editor (for code changes)
   - Displayed in a popup (for explanations)
   - Added as comments (for documentation)

## Extension Points

The plugin can be extended in the following ways:

1. **New Actions**: Additional actions can be added to the context menu or tools menu
2. **Pattern Recognition Enhancements**: The pattern matching algorithm can be improved
3. **UI Customizations**: Additional panels can be added to the tool window
4. **Support for Additional Languages**: Beyond Java, support for other languages used in modding

## Performance Considerations

1. **Memory Usage**: Pattern storage is limited to prevent excessive memory usage
2. **Threading**: Long-running operations happen on background threads to prevent UI freezing
3. **Rate Limiting**: API calls are rate-limited to stay within OpenAI's guidelines
4. **Cached Results**: Pattern recognition reduces API usage by up to 70% for common tasks

## Security Considerations

1. **API Keys**: Stored securely in IntelliJ's credential store
2. **Local Processing**: Pattern matching happens locally to minimize data transmission
3. **Minimized Data**: Only essential code snippets are sent to OpenAI API
4. **User Confirmation**: Changes to code require user confirmation by default

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for information on how to contribute to the plugin.