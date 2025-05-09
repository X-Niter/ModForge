package com.modforge.intellij.plugin.ml;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Classifier for error messages.
 * Used to categorize errors for better pattern matching.
 */
public class ErrorClassifier {
    private static final Logger LOG = Logger.getInstance(ErrorClassifier.class);
    
    // Error category patterns for Minecraft mod development
    private static final Map<String, Pattern> ERROR_PATTERNS = new HashMap<>();
    
    static {
        // Java compilation errors
        ERROR_PATTERNS.put("syntax_error", Pattern.compile("(?i)\\b(syntax error|unexpected token|illegal start of expression|;\\s+expected)\\b"));
        ERROR_PATTERNS.put("unresolved_symbol", Pattern.compile("(?i)\\b(cannot find symbol|cannot resolve symbol|symbol not found|cannot resolve method|cannot resolve class|package .* does not exist)\\b"));
        ERROR_PATTERNS.put("type_mismatch", Pattern.compile("(?i)\\b(incompatible types|inconvertible types|bad type|required type|cannot be applied to|cannot be converted to)\\b"));
        ERROR_PATTERNS.put("null_pointer", Pattern.compile("(?i)\\b(null pointer exception|npe|null reference|nullpointerexception)\\b"));
        ERROR_PATTERNS.put("array_bounds", Pattern.compile("(?i)\\b(arrayindexoutofboundsexception|array index out of bounds|array index.*range|index.*out of bounds)\\b"));
        ERROR_PATTERNS.put("casting_error", Pattern.compile("(?i)\\b(classcastexception|cannot cast|class cast exception|invalid cast)\\b"));
        ERROR_PATTERNS.put("access_violation", Pattern.compile("(?i)\\b(is not accessible|has private access|protected|not visible|visibility|illegal access)\\b"));
        ERROR_PATTERNS.put("resource_leak", Pattern.compile("(?i)\\b(resource leak|not closed|stream not closed|connection not closed)\\b"));
        
        // Minecraft/Forge specific errors
        ERROR_PATTERNS.put("registry_error", Pattern.compile("(?i)\\b(registry|registries|registerblocks|registeritems|registertiles|register.*entity|deferred register)\\b"));
        ERROR_PATTERNS.put("mixin_error", Pattern.compile("(?i)\\b(mixin|mixins|mixin error|mixin target|mixin injection|failed to apply mixin)\\b"));
        ERROR_PATTERNS.put("capability_error", Pattern.compile("(?i)\\b(capability|capabilities|icapability|provider|icapabilityprovider)\\b"));
        ERROR_PATTERNS.put("networking_error", Pattern.compile("(?i)\\b(network|packet|packetbuffer|packethandler|messagetype|channel|simpleimpl)\\b"));
        ERROR_PATTERNS.put("event_error", Pattern.compile("(?i)\\b(event|eventbus|forgeeventbus|subscribeevent|eventsubscriber|event handler)\\b"));
        ERROR_PATTERNS.put("datagen_error", Pattern.compile("(?i)\\b(datagenerator|data gen|blockstate|modelprovider|taggenerator|lootable|recipe|advancement)\\b"));
        ERROR_PATTERNS.put("model_error", Pattern.compile("(?i)\\b(model|modelloader|bakemodel|ibakemodel|modelresourcelocation|modeldata|modelfile|blockmodel)\\b"));
        ERROR_PATTERNS.put("render_error", Pattern.compile("(?i)\\b(renderer|rendertype|render|tesrbase|tesr|tileentityrenderer|spriteuploader|renderermanager)\\b"));
        ERROR_PATTERNS.put("config_error", Pattern.compile("(?i)\\b(config|configuration|configvalue|configspec|modconfig|configuration file)\\b"));
        ERROR_PATTERNS.put("lifecycle_error", Pattern.compile("(?i)\\b(lifecycle|modloadingcontext|setupevent|fmlevent|clientsetup|commonsetup|mod container|mod class)\\b"));
        
        // Fabric specific errors
        ERROR_PATTERNS.put("fabric_mixin_error", Pattern.compile("(?i)\\b(fabric.*mixin|mixin.*fabric|spongepowered.*mixin)\\b"));
        ERROR_PATTERNS.put("fabric_registry_error", Pattern.compile("(?i)\\b(registry|registries|registry.*fabric|identifier|register.*fabric)\\b"));
        ERROR_PATTERNS.put("fabric_networking_error", Pattern.compile("(?i)\\b(networkhandler|packetbytebuf|clientplaynetworkhandler|serverplaynetworkhandler|channel)\\b"));
        ERROR_PATTERNS.put("fabric_rendering_error", Pattern.compile("(?i)\\b(blockrendererdispatcher|model|spriteatlastexture|renderphase|renderlayer|vertexconsumer|renderlayers)\\b"));
        ERROR_PATTERNS.put("fabric_api_error", Pattern.compile("(?i)\\b(api.*fabric|fabric.*api|fbric.*loader|fabricloader)\\b"));
        
        // BuildTools and dependency errors
        ERROR_PATTERNS.put("gradle_error", Pattern.compile("(?i)\\b(gradle|build.gradle|settings.gradle|gradle-wrapper|gradlew|task|plugin.*gradle)\\b"));
        ERROR_PATTERNS.put("dependency_error", Pattern.compile("(?i)\\b(dependency|dependencies|compile|implementation|runtimeonly|api|modimplementation|include)\\b"));
        ERROR_PATTERNS.put("version_mismatch", Pattern.compile("(?i)\\b(version mismatch|incompatible version|requires version|different version|correct version)\\b"));
        ERROR_PATTERNS.put("missing_dependency", Pattern.compile("(?i)\\b(missing dependency|could not find|dependency.*not found|required dependency|mod.*depends on)\\b"));
    }
    
    /**
     * Creates a new ErrorClassifier.
     */
    public ErrorClassifier() {
    }
    
    /**
     * Classifies an error message.
     * @param errorMessage The error message
     * @return The error category
     */
    @NotNull
    public String classifyError(@NotNull String errorMessage) {
        for (Map.Entry<String, Pattern> entry : ERROR_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(errorMessage).find()) {
                LOG.info("Classified error as: " + entry.getKey());
                return entry.getKey();
            }
        }
        
        // If no specific pattern matches, try to infer from common error types
        if (errorMessage.contains("Exception") || errorMessage.contains("exception")) {
            return inferExceptionType(errorMessage);
        }
        
        // Default to unknown
        LOG.info("Could not classify error, using general category");
        return "general_error";
    }
    
    /**
     * Infers the exception type from an error message.
     * @param errorMessage The error message
     * @return The inferred exception type
     */
    @NotNull
    private String inferExceptionType(@NotNull String errorMessage) {
        // Check for common exception types
        if (errorMessage.contains("NullPointer") || errorMessage.contains("null pointer")) {
            return "null_pointer";
        } else if (errorMessage.contains("IndexOutOfBounds") || errorMessage.contains("index out of bounds")) {
            return "array_bounds";
        } else if (errorMessage.contains("ClassCast") || errorMessage.contains("class cast")) {
            return "casting_error";
        } else if (errorMessage.contains("IllegalArgument") || errorMessage.contains("illegal argument")) {
            return "illegal_argument";
        } else if (errorMessage.contains("IOException") || errorMessage.contains("I/O") || errorMessage.contains("input/output")) {
            return "io_error";
        } else if (errorMessage.contains("OutOfMemory") || errorMessage.contains("out of memory")) {
            return "memory_error";
        } else if (errorMessage.contains("ConcurrentModification") || errorMessage.contains("concurrent modification")) {
            return "concurrency_error";
        } else if (errorMessage.contains("IllegalState") || errorMessage.contains("illegal state")) {
            return "state_error";
        } else if (errorMessage.contains("NumberFormat") || errorMessage.contains("number format")) {
            return "parse_error";
        } else if (errorMessage.contains("Assertion") || errorMessage.contains("assertion")) {
            return "assertion_error";
        } else if (errorMessage.contains("Timeout") || errorMessage.contains("timed out")) {
            return "timeout_error";
        }
        
        return "unknown_exception";
    }
    
    /**
     * Gets additional insights about an error based on its category.
     * @param errorCategory The error category
     * @return Insights about the error
     */
    @NotNull
    public Map<String, String> getErrorInsights(@NotNull String errorCategory) {
        Map<String, String> insights = new HashMap<>();
        
        switch (errorCategory) {
            case "syntax_error":
                insights.put("common_causes", "Missing semicolons, braces, or parentheses; incorrect syntax for lambda expressions or streams");
                insights.put("fix_approaches", "Check for missing punctuation; review Java syntax rules; look for unbalanced brackets or quotes");
                break;
                
            case "unresolved_symbol":
                insights.put("common_causes", "Missing imports; mistyped class or method names; missing dependencies; wrong package structure");
                insights.put("fix_approaches", "Add missing imports; check spelling of identifiers; verify package structure; verify classpath");
                break;
                
            case "type_mismatch":
                insights.put("common_causes", "Assigning incompatible types; passing wrong parameter types; incorrect return types");
                insights.put("fix_approaches", "Check method signatures; add explicit casts where appropriate; use correct parameter types");
                break;
                
            case "registry_error":
                insights.put("common_causes", "Registering items/blocks in wrong phase; incorrect registry names; duplicate registry entries");
                insights.put("fix_approaches", "Use DeferredRegister; ensure registry runs at correct time; check for duplicate names");
                break;
                
            case "mixin_error":
                insights.put("common_causes", "Targeting non-existent class or method; incorrect mixin configuration; incompatible game version");
                insights.put("fix_approaches", "Verify target class/method exists; check method signatures; update mixin annotations");
                break;
                
            case "fabric_api_error":
                insights.put("common_causes", "Incompatible Fabric API version; missing required Fabric modules; incorrect module usage");
                insights.put("fix_approaches", "Update Fabric API version; add missing modules to dependencies; check API method usage");
                break;
                
            case "version_mismatch":
                insights.put("common_causes", "Incompatible mod or library versions; wrong Minecraft version; API changes");
                insights.put("fix_approaches", "Update dependencies to compatible versions; adapt code to API changes; check version requirements");
                break;
                
            default:
                insights.put("common_causes", "Various code issues specific to this error type");
                insights.put("fix_approaches", "Analyze error message carefully; check related code; search for similar issues online");
                break;
        }
        
        return insights;
    }
}