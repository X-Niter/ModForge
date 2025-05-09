package com.modforge.intellij.plugin.crossloader.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action group for cross-loader actions in the ModForge menu.
 */
public class CrossLoaderActionGroup extends ActionGroup {
    
    /**
     * Gets the actions in the group.
     * @param e The action event
     * @return The actions
     */
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[] {
                new CreateCrossLoaderProjectAction(),
                new ConvertToArchitecturyAction()
        };
    }
}