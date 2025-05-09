package com.modforge.intellij.plugin.crossloader.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Action to convert an existing mod project to use Architectury.
 */
public class ConvertToArchitecturyAction extends AnAction {
    
    /**
     * Creates a new ConvertToArchitecturyAction.
     */
    public ConvertToArchitecturyAction() {
        super("Convert to Architectury", "Convert an existing mod project to use Architectury", AllIcons.Actions.Refresh);
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
        
        // TODO: Implement project conversion
        // This would involve analyzing the current project structure,
        // creating the necessary Architectury modules, and migrating code
        
        Messages.showInfoMessage(
                "Project conversion to Architectury is not yet implemented.\n" +
                "Please create a new cross-loader project instead.",
                "Not Implemented"
        );
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