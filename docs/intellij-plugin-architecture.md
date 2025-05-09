# ModForge IntelliJ Plugin Architecture Guide

This document provides a detailed overview of the ModForge IntelliJ plugin architecture, designed for developers who want to understand or extend the plugin functionality.

## Plugin Overview

The ModForge IntelliJ plugin provides seamless integration between the IDE and the ModForge autonomous development system. It enables:

- Direct interaction with the AI system from within the IDE
- Two-way synchronization with GitHub
- Contextual code assistance for Minecraft mod development
- In-IDE testing and debugging of mods

## Architecture Components

### 1. Core Components

The plugin is organized into these main packages:

**`com.modforge.intellij.plugin.core`**
- Core plugin infrastructure and services
- Plugin startup, lifecycle management, and service registration
- Configuration persistence and settings management

**`com.modforge.intellij.plugin.ui`**
- User interface components for the IDE integration
- Tool windows, dialogs, editor extensions, and notification systems
- Action handlers for menu items and keyboard shortcuts

**`com.modforge.intellij.plugin.services`**
- Business logic services for plugin operations
- AI integration and API communication
- GitHub integration
- Mod compilation and testing services

**`com.modforge.intellij.plugin.sync`**
- Bi-directional synchronization with remote GitHub repositories
- Change tracking and conflict resolution

**`com.modforge.intellij.plugin.utils`**
- Utility classes and helper functions
- Logging, error handling, and data conversion utilities

### 2. Extension Points

The plugin utilizes several IntelliJ Platform extension points:

- **Tool Windows**: ModForge Assistant and Issue Tracker
- **File Type Factories**: Custom recognition for mod configuration files
- **Project Components**: Integration with IDE project structure
- **Intention Actions**: Contextual code improvement suggestions
- **Completion Contributors**: Custom code completion for mod development
- **Line Marker Providers**: Visual indicators for AI-enhanced code sections

### 3. Service Interfaces

Key service interfaces that form the API for plugin extension:

```java
// AI communication service
public interface AIService {
    CompletableFuture<String> sendRequest(String prompt, Map<String, Object> parameters);
    CompletableFuture<CodeGenerationResponse> generateCode(CodeGenerationRequest request);
    CompletableFuture<DocGenerationResponse> generateDocumentation(DocGenerationRequest request);
    void cancelRequest(String requestId);
}

// GitHub integration service
public interface GitHubService {
    CompletableFuture<PullRequest> createPullRequest(PullRequestData data);
    CompletableFuture<List<Issue>> getIssues(String repositoryPath);
    CompletableFuture<Boolean> syncRepository(String localPath);
    CompletableFuture<CommitResult> commitAndPush(List<VirtualFile> files, String message);
}

// Mod testing service
public interface MinecraftTestService {
    CompletableFuture<TestResult> runModInDevEnvironment(ModConfig config);
    CompletableFuture<CompileResult> compileModProject(Project project);
    void terminateRunningInstance();
}
```

## Communication Flow

### 1. User Interaction to AI Processing
```
User Action → Action Handler → AIService → AI API → Response Processor → UI Update
```

### 2. GitHub Synchronization
```
Local Edit → Change Detection → SyncService → GitHub API → Remote Update
```
```
GitHub Webhook → Notification Service → SyncService → Local Repository Update
```

### 3. Mod Testing Cycle
```
Run Request → MinecraftTestService → Gradle Build → Minecraft Instance → Log Parser → Result Display
```

## Extending the Plugin

### 1. Adding a New Action

To add a new menu action or command:

1. Create a new Action class:
```java
public class MyCustomAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        // Your implementation here
        AIService aiService = project.getService(AIService.class);
        aiService.sendRequest("My custom prompt", new HashMap<>())
            .thenAccept(response -> {
                // Handle response
            });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Control visibility and enabled status
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}
```

2. Register in `plugin.xml`:
```xml
<actions>
    <action id="ModForge.MyCustomAction" 
            class="com.modforge.intellij.plugin.actions.MyCustomAction"
            text="My Custom Action" 
            description="Performs a custom AI operation">
        <add-to-group group-id="ModForgeActions" anchor="last"/>
        <keyboard-shortcut keymap="$default" first-keystroke="shift ctrl alt M"/>
    </action>
</actions>
```

### 2. Adding a New Service

To add a new service component:

1. Define the service interface:
```java
public interface CustomService {
    CompletableFuture<ResultType> performOperation(Parameters params);
    // Other methods
}
```

2. Implement the service:
```java
public class CustomServiceImpl implements CustomService {
    private final Project project;
    
    public CustomServiceImpl(Project project) {
        this.project = project;
    }
    
    @Override
    public CompletableFuture<ResultType> performOperation(Parameters params) {
        // Implementation
    }
}
```

3. Register in `plugin.xml`:
```xml
<extensions defaultExtensionNs="com.intellij">
    <projectService serviceInterface="com.modforge.intellij.plugin.services.CustomService"
                    serviceImplementation="com.modforge.intellij.plugin.services.impl.CustomServiceImpl"/>
</extensions>
```

### 3. Adding a New Tool Window

To add a new tool window:

1. Create a Tool Window Factory:
```java
public class CustomToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CustomToolWindowContent content = new CustomToolWindowContent(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content toolContent = contentFactory.createContent(content.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(toolContent);
    }
}
```

2. Create the content panel:
```java
public class CustomToolWindowContent {
    private final JPanel contentPanel;
    
    public CustomToolWindowContent(Project project) {
        contentPanel = new JPanel(new BorderLayout());
        // Add components
    }
    
    public JPanel getContentPanel() {
        return contentPanel;
    }
}
```

3. Register in `plugin.xml`:
```xml
<extensions defaultExtensionNs="com.intellij">
    <toolWindow id="Custom Tool" 
                anchor="right" 
                factoryClass="com.modforge.intellij.plugin.ui.CustomToolWindowFactory"/>
</extensions>
```

## Plugin Settings

The plugin stores settings in two levels:

1. **Application-level settings**: API keys, GitHub credentials, global preferences
2. **Project-level settings**: Project-specific configuration, mod settings, repo links

Settings are defined using the IntelliJ Platform settings API and are stored in:
- Application: `~/.config/JetBrains/IntelliJIDEAx/options/modforge.xml`
- Project: `.idea/modforge.xml`

## Testing the Plugin

The plugin includes several testing layers:

1. **Unit Tests**: For core services and utilities
2. **Integration Tests**: For interaction between components
3. **UI Tests**: For user interface components
4. **Platform Tests**: For integration with the IntelliJ Platform

Run tests using Gradle:
```bash
./gradlew test                 # Run unit tests
./gradlew integrationTest      # Run integration tests
./gradlew platformTest         # Run platform tests
```

## Debugging

Debug the plugin by running it in a development instance:
```bash
./gradlew runIde
```

Debug logs are located at:
- IDE logs: Help → Show Log in Explorer/Finder
- Plugin logs: Look for "ModForge" entries in the IDE log

## Performance Considerations

- Use background tasks for long-running operations
- Implement cancelation for API requests
- Cache responses where appropriate
- Use IntelliJ's read/write action system correctly to avoid UI freezes

## Future Development Areas

1. **Multi-IDE Support**: Extending beyond IntelliJ to other platforms
2. **Enhanced Testing**: More sophisticated Minecraft mod testing infrastructure
3. **Collaborative Editing**: Real-time collaboration features
4. **Extended AI Features**: More contextual awareness and code understanding

## Helpful Resources

1. [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
2. [Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development)
3. [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
4. [ModForge Documentation](https://modforge.ai/docs)

---

For questions about plugin architecture or contributions, please create an issue in the GitHub repository.