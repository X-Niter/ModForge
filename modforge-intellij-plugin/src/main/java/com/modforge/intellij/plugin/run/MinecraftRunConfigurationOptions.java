package com.modforge.intellij.plugin.run;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

/**
 * Options storage for Minecraft run configurations.
 * Stores persistent settings for run configurations between IDE sessions.
 */
public class MinecraftRunConfigurationOptions extends RunConfigurationOptions {
    
    private final StoredProperty<String> runTypeProperty = string("CLIENT").provideDelegate(this, "runType");
    private final StoredProperty<Boolean> enableDebugProperty = property(true).provideDelegate(this, "enableDebug");
    private final StoredProperty<String> vmArgsProperty = string("").provideDelegate(this, "vmArgs");
    private final StoredProperty<String> programArgsProperty = string("").provideDelegate(this, "programArgs");
    private final StoredProperty<String> modLoaderProperty = string("FORGE").provideDelegate(this, "modLoader");
    
    public String getRunType() {
        return runTypeProperty.getValue(this);
    }
    
    public void setRunType(String runType) {
        runTypeProperty.setValue(this, runType);
    }
    
    public boolean isEnableDebug() {
        return enableDebugProperty.getValue(this);
    }
    
    public void setEnableDebug(boolean enableDebug) {
        enableDebugProperty.setValue(this, enableDebug);
    }
    
    public String getVmArgs() {
        return vmArgsProperty.getValue(this);
    }
    
    public void setVmArgs(String vmArgs) {
        vmArgsProperty.setValue(this, vmArgs);
    }
    
    public String getProgramArgs() {
        return programArgsProperty.getValue(this);
    }
    
    public void setProgramArgs(String programArgs) {
        programArgsProperty.setValue(this, programArgs);
    }
    
    public String getModLoader() {
        return modLoaderProperty.getValue(this);
    }
    
    public void setModLoader(String modLoader) {
        modLoaderProperty.setValue(this, modLoader);
    }
}