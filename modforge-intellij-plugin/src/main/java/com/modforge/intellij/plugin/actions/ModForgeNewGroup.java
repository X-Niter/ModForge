package com.modforge.intellij.plugin.actions;

import com.intellij.ide.actions.NonEmptyActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Action group for ModForge new actions.
 */
public class ModForgeNewGroup extends NonEmptyActionGroup implements DumbAware {
    public ModForgeNewGroup() {
        // Default constructor
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Always enable
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(true);
    }
}