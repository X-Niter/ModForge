package com.modforge.intellij.plugin.crossloader.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.crossloader.ui.CrossLoaderProjectSetupDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action to create a new cross-loader project.
 */
public class CreateCrossLoaderProjectAction extends AnAction {
    
    /**
     * Creates a new CreateCrossLoaderProjectAction.
     */
    public CreateCrossLoaderProjectAction() {
        super("Create Cross-Loader Project", "Create a new cross-loader mod project", AllIcons.Actions.New);
    }
    
    /**
     * Performs the action.
     * @param e The action event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        CrossLoaderProjectSetupDialog dialog = new CrossLoaderProjectSetupDialog(project);
        dialog.show();
    }
    
    /**
     * Updates the action's state.
     * @param e The action event
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}