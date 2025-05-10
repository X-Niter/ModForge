package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("ModForge AI");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel contentLabel = new JLabel(
            "<html><body style='width: 300px; text-align: center;'>" +
            "<p>ModForge AI is ready for IntelliJ IDEA 2025.1</p>" +
            "<p>This plugin is optimized for Java 21 and leverages virtual threads.</p>" +
            "</body></html>"
        );
        contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(contentLabel, BorderLayout.CENTER);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
