#!/usr/bin/env python3
"""
Process commands from GitHub issue and pull request comments.

This script detects and processes commands in comments using the format:
/command [arguments]

Supported commands:
- /fix [target] - Fix issues in the specified file or component
- /improve [target] - Improve code quality in the specified file
- /document [target] - Generate documentation for the specified file
- /add [feature] to [target] - Add a feature to the specified component
- /explain [target] - Explain how the specified code works
- /help - Show available commands
"""

import os
import re
import sys
import json
import logging
import subprocess
from pathlib import Path
import openai

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ModForge-CommentCommand')

# Initialize OpenAI API
openai.api_key = os.environ.get('OPENAI_API_KEY')
if not openai.api_key:
    logger.error("OPENAI_API_KEY not found in environment variables")
    sys.exit(1)

# Get comment details from environment variables
COMMENT_BODY = os.environ.get('COMMENT_BODY', '')
ISSUE_NUMBER = os.environ.get('ISSUE_NUMBER', '0')
ISSUE_TITLE = os.environ.get('ISSUE_TITLE', '')
COMMENT_ID = os.environ.get('COMMENT_ID', '0')
IS_PR = os.environ.get('IS_PR', 'false').lower() == 'true'
PR_NUMBER = os.environ.get('PR_NUMBER', '0')

# Root directory of the project
ROOT_DIR = Path(__file__).parent.parent.parent
SRC_DIR = ROOT_DIR / 'src'
JAVA_SRC_DIR = SRC_DIR / 'main' / 'java' / 'com' / 'modforge' / 'intellij' / 'plugin'
RESOURCES_DIR = SRC_DIR / 'main' / 'resources'
RESPONSE_FILE = Path('comment_response.txt')

# Command regex patterns
FIX_PATTERN = r'/fix\s+(?:"([^"]+)"|([^\s"]+))'
IMPROVE_PATTERN = r'/improve\s+(?:"([^"]+)"|([^\s"]+))'
DOCUMENT_PATTERN = r'/document\s+(?:"([^"]+)"|([^\s"]+))'
ADD_PATTERN = r'/add\s+(?:"([^"]+)"|([^\s"]+))\s+to\s+(?:"([^"]+)"|([^\s"]+))'
EXPLAIN_PATTERN = r'/explain\s+(?:"([^"]+)"|([^\s"]+))'
HELP_PATTERN = r'/help'

class CommandProcessor:
    def __init__(self, comment_body, issue_number, is_pr):
        self.comment_body = comment_body
        self.issue_number = issue_number
        self.is_pr = is_pr
        self.changes_made = []
        
    def process(self):
        """Process the comment for commands"""
        logger.info(f"Processing comment on issue/PR #{self.issue_number}")
        
        # Extract all commands from the comment
        commands = self._extract_commands()
        
        if not commands:
            logger.info("No valid commands found in comment")
            return
            
        # Process each command
        results = []
        for cmd_type, args in commands:
            logger.info(f"Processing command: {cmd_type} with args: {args}")
            result = self._execute_command(cmd_type, args)
            results.append((cmd_type, args, result))
            
        # Respond with results
        self._respond_with_results(results)
        
    def _extract_commands(self):
        """Extract all commands from the comment"""
        commands = []
        
        # Check for fix command
        fix_matches = re.finditer(FIX_PATTERN, self.comment_body)
        for match in fix_matches:
            target = match.group(1) if match.group(1) else match.group(2)
            commands.append(("fix", target))
            
        # Check for improve command
        improve_matches = re.finditer(IMPROVE_PATTERN, self.comment_body)
        for match in improve_matches:
            target = match.group(1) if match.group(1) else match.group(2)
            commands.append(("improve", target))
            
        # Check for document command
        document_matches = re.finditer(DOCUMENT_PATTERN, self.comment_body)
        for match in document_matches:
            target = match.group(1) if match.group(1) else match.group(2)
            commands.append(("document", target))
            
        # Check for add command
        add_matches = re.finditer(ADD_PATTERN, self.comment_body)
        for match in add_matches:
            feature = match.group(1) if match.group(1) else match.group(2)
            target = match.group(3) if match.group(3) else match.group(4)
            commands.append(("add", {"feature": feature, "target": target}))
            
        # Check for explain command
        explain_matches = re.finditer(EXPLAIN_PATTERN, self.comment_body)
        for match in explain_matches:
            target = match.group(1) if match.group(1) else match.group(2)
            commands.append(("explain", target))
            
        # Check for help command
        if re.search(HELP_PATTERN, self.comment_body):
            commands.append(("help", None))
            
        return commands
    
    def _execute_command(self, cmd_type, args):
        """Execute a command"""
        try:
            if cmd_type == "fix":
                return self._fix_code(args)
            elif cmd_type == "improve":
                return self._improve_code(args)
            elif cmd_type == "document":
                return self._document_code(args)
            elif cmd_type == "add":
                return self._add_feature(args["feature"], args["target"])
            elif cmd_type == "explain":
                return self._explain_code(args)
            elif cmd_type == "help":
                return self._show_help()
            else:
                return f"Unknown command type: {cmd_type}"
        except Exception as e:
            logger.error(f"Error executing command {cmd_type}: {e}")
            return f"Error: {str(e)}"
    
    def _fix_code(self, target):
        """Fix issues in a file or component"""
        target_path = self._find_file(target)
        if not target_path:
            return f"Could not find target file: {target}"
            
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Use AI to fix the code
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to fix issues in code without changing its functionality."},
                    {"role": "user", "content": f"Fix any issues or bugs in this code file, focusing on correctness, performance, and best practices:\n\n```java\n{content}\n```\n\nProvide only the corrected code without explanations."}
                ]
            )
            
            fixed_code = response.choices[0].message['content']
            
            # Extract just the code if it's wrapped in markdown
            code_match = re.search(r'```(?:java)?\n(.*?)\n```', fixed_code, re.DOTALL)
            if code_match:
                fixed_code = code_match.group(1)
                
            # Check if anything actually changed
            if fixed_code == content:
                return f"No issues found in {target}"
                
            # Write the fixed code back to the file
            with open(target_path, 'w') as f:
                f.write(fixed_code)
                
            # Stage the changes
            subprocess.run(["git", "add", target_path], check=True)
            
            self.changes_made.append(f"Fixed issues in {target}")
            return f"Successfully fixed issues in {target}"
            
        except Exception as e:
            logger.error(f"Error fixing {target}: {e}")
            return f"Failed to fix {target}: {str(e)}"
    
    def _improve_code(self, target):
        """Improve code quality in a file"""
        target_path = self._find_file(target)
        if not target_path:
            return f"Could not find target file: {target}"
            
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Use AI to improve the code
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to improve code quality without changing functionality."},
                    {"role": "user", "content": f"Improve the quality of this code file while preserving its functionality. Focus on readability, maintainability, and performance:\n\n```java\n{content}\n```\n\nProvide only the improved code without explanations."}
                ]
            )
            
            improved_code = response.choices[0].message['content']
            
            # Extract just the code if it's wrapped in markdown
            code_match = re.search(r'```(?:java)?\n(.*?)\n```', improved_code, re.DOTALL)
            if code_match:
                improved_code = code_match.group(1)
                
            # Check if anything actually changed
            if improved_code == content:
                return f"No improvements identified for {target}"
                
            # Write the improved code back to the file
            with open(target_path, 'w') as f:
                f.write(improved_code)
                
            # Stage the changes
            subprocess.run(["git", "add", target_path], check=True)
            
            self.changes_made.append(f"Improved code quality in {target}")
            return f"Successfully improved code quality in {target}"
            
        except Exception as e:
            logger.error(f"Error improving {target}: {e}")
            return f"Failed to improve {target}: {str(e)}"
    
    def _document_code(self, target):
        """Generate documentation for a file"""
        target_path = self._find_file(target)
        if not target_path:
            return f"Could not find target file: {target}"
            
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Use AI to document the code
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to add comprehensive JavaDoc documentation to code."},
                    {"role": "user", "content": f"Add comprehensive JavaDoc documentation to this code file, including class-level docs, method docs, parameter descriptions, and return value descriptions:\n\n```java\n{content}\n```\n\nProvide only the documented code without explanations."}
                ]
            )
            
            documented_code = response.choices[0].message['content']
            
            # Extract just the code if it's wrapped in markdown
            code_match = re.search(r'```(?:java)?\n(.*?)\n```', documented_code, re.DOTALL)
            if code_match:
                documented_code = code_match.group(1)
                
            # Check if anything actually changed
            if documented_code == content:
                return f"No documentation changes needed for {target}"
                
            # Write the documented code back to the file
            with open(target_path, 'w') as f:
                f.write(documented_code)
                
            # Stage the changes
            subprocess.run(["git", "add", target_path], check=True)
            
            self.changes_made.append(f"Added documentation to {target}")
            return f"Successfully added documentation to {target}"
            
        except Exception as e:
            logger.error(f"Error documenting {target}: {e}")
            return f"Failed to document {target}: {str(e)}"
    
    def _add_feature(self, feature, target):
        """Add a feature to a component"""
        target_path = self._find_file(target)
        new_file = False
        
        if not target_path:
            # Check if it's a package
            if target.endswith(".java"):
                # It's a specific file that doesn't exist yet
                target_path = JAVA_SRC_DIR / target
                new_file = True
            else:
                # It's a package or component name
                package_path = JAVA_SRC_DIR
                for part in target.split('.'):
                    package_path = package_path / part
                    
                if not package_path.exists():
                    return f"Could not find target package or component: {target}"
                    
                # Create a new class for the feature
                class_name = ''.join(word.capitalize() for word in feature.split())
                target_path = package_path / f"{class_name}.java"
                new_file = True
                
        try:
            if new_file:
                # Create a new file for the feature
                package_name = os.path.dirname(target_path).replace(str(JAVA_SRC_DIR), "").replace("/", ".").lstrip(".")
                if not package_name:
                    package_name = "com.modforge.intellij.plugin"
                
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to create a new Java class implementing a specific feature."},
                        {"role": "user", "content": f"Create a new Java class for an IntelliJ plugin that implements the following feature: {feature}\n\nPackage: {package_name}\nClass name: {os.path.basename(target_path).replace('.java', '')}\n\nUse proper JavaDoc, follow IntelliJ platform conventions, and include all necessary imports and implementations."}
                    ]
                )
                
                new_code = response.choices[0].message['content']
                
                # Extract just the code if it's wrapped in markdown
                code_match = re.search(r'```(?:java)?\n(.*?)\n```', new_code, re.DOTALL)
                if code_match:
                    new_code = code_match.group(1)
                    
                # Create directory if it doesn't exist
                os.makedirs(os.path.dirname(target_path), exist_ok=True)
                
                # Write the new file
                with open(target_path, 'w') as f:
                    f.write(new_code)
                    
                # Stage the changes
                subprocess.run(["git", "add", target_path], check=True)
                
                self.changes_made.append(f"Created new file {target_path} implementing {feature}")
                return f"Successfully created new file {os.path.basename(target_path)} implementing {feature}"
                
            else:
                # Add the feature to an existing file
                with open(target_path, 'r') as f:
                    content = f.read()
                    
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to add a new feature to an existing class."},
                        {"role": "user", "content": f"Add the following feature to this existing Java class: {feature}\n\n```java\n{content}\n```\n\nMake sure to maintain the existing code structure and functionality while adding the new feature. Include proper JavaDoc for the new methods. Provide only the complete updated code without explanations."}
                    ]
                )
                
                updated_code = response.choices[0].message['content']
                
                # Extract just the code if it's wrapped in markdown
                code_match = re.search(r'```(?:java)?\n(.*?)\n```', updated_code, re.DOTALL)
                if code_match:
                    updated_code = code_match.group(1)
                    
                # Write the updated code back to the file
                with open(target_path, 'w') as f:
                    f.write(updated_code)
                    
                # Stage the changes
                subprocess.run(["git", "add", target_path], check=True)
                
                self.changes_made.append(f"Added feature {feature} to {target}")
                return f"Successfully added feature {feature} to {target}"
                
        except Exception as e:
            logger.error(f"Error adding feature {feature} to {target}: {e}")
            return f"Failed to add feature {feature} to {target}: {str(e)}"
    
    def _explain_code(self, target):
        """Explain how a file or component works"""
        target_path = self._find_file(target)
        if not target_path:
            return f"Could not find target file: {target}"
            
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Use AI to explain the code
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specialized in IntelliJ plugin development. Your task is to explain code in a clear, concise way."},
                    {"role": "user", "content": f"Explain how this code works, focusing on its purpose, key components, and how it integrates with the rest of the system:\n\n```java\n{content}\n```\n\nProvide a clear, educational explanation suitable for a developer joining the project."}
                ]
            )
            
            explanation = response.choices[0].message['content']
            
            # No changes to the file, just return the explanation
            return f"## Explanation of `{target}`\n\n{explanation}"
            
        except Exception as e:
            logger.error(f"Error explaining {target}: {e}")
            return f"Failed to explain {target}: {str(e)}"
    
    def _show_help(self):
        """Show available commands"""
        help_text = """## Available Commands

Use these commands in comments to interact with the autonomous development system:

### Code Manipulation
- `/fix [target]` - Fix issues in the specified file or component
- `/improve [target]` - Improve code quality in the specified file
- `/document [target]` - Generate documentation for the specified file
- `/add [feature] to [target]` - Add a feature to the specified component

### Information
- `/explain [target]` - Explain how the specified code works
- `/help` - Show this help message

### Examples
- `/fix GenerateCodeAction.java`
- `/improve com.modforge.intellij.plugin.ui.toolwindow.AIAssistPanel`
- `/document SettingsPanel.java`
- `/add "dark mode toggle" to MetricsPanel.java`
- `/explain ContinuousDevelopmentService.java`

The system will process your command and respond with the results in a comment.
"""
        return help_text
    
    def _find_file(self, target):
        """Find a file based on a target specification"""
        # Check if it's a direct path
        if os.path.exists(target) and os.path.isfile(target):
            return target
            
        # Check if it's a path relative to the Java source directory
        java_path = JAVA_SRC_DIR / target
        if java_path.exists() and java_path.is_file():
            return java_path
            
        # Check if it's a class name without .java extension
        if not target.endswith('.java'):
            class_path = JAVA_SRC_DIR / f"{target}.java"
            if class_path.exists() and class_path.is_file():
                return class_path
            
        # Check if it's a fully qualified class name
        if '.' in target:
            parts = target.split('.')
            class_name = parts[-1]
            package_path = JAVA_SRC_DIR
            
            for part in parts[:-1]:
                package_path = package_path / part
                
            class_path = package_path / f"{class_name}.java"
            if class_path.exists() and class_path.is_file():
                return class_path
            
        # Recursive search for the file
        for root, _, files in os.walk(JAVA_SRC_DIR):
            for file in files:
                if file == target or file == f"{target}.java":
                    return os.path.join(root, file)
                    
        return None
    
    def _respond_with_results(self, results):
        """Generate a response with the results of all commands"""
        response = []
        response.append(f"## ModForge Automation Response")
        response.append("")
        
        for cmd_type, args, result in results:
            if cmd_type == "add":
                args_str = f"{args['feature']} to {args['target']}"
            else:
                args_str = str(args) if args else ""
                
            response.append(f"### Command: `/{cmd_type} {args_str}`")
            response.append("")
            response.append(result)
            response.append("")
            
        if self.changes_made:
            response.append("### Changes Made")
            response.append("")
            for change in self.changes_made:
                response.append(f"- ✅ {change}")
            response.append("")
            
        response.append("---")
        response.append("*Executed by ModForge Automation System*")
        
        # Write the response to the file
        with open(RESPONSE_FILE, 'w') as f:
            f.write("\n".join(response))
            
        logger.info("Response prepared")

# Run the command processor
if __name__ == "__main__":
    processor = CommandProcessor(COMMENT_BODY, ISSUE_NUMBER, IS_PR)
    processor.process()