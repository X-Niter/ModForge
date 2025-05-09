#!/usr/bin/env python3
"""
Process commands from GitHub issues to automate development tasks.

This script parses command syntax from GitHub issues and performs the requested
development tasks using AI assistance.

Command Format:
  /command [action] [target] [parameters]

Examples:
  /command fix error in GenerateCodeAction.java
  /command add feature "Dark mode support" to the UI components
  /command improve AIAssistPanel.java
  /command document SettingsPanel.java
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
logger = logging.getLogger('ModForge-IssueCommand')

# Initialize OpenAI API
openai.api_key = os.environ.get('OPENAI_API_KEY')
if not openai.api_key:
    logger.error("OPENAI_API_KEY not found in environment variables")
    sys.exit(1)

# Get issue details from environment variables
ISSUE_BODY = os.environ.get('ISSUE_BODY', '')
ISSUE_NUMBER = os.environ.get('ISSUE_NUMBER', '0')
ISSUE_TITLE = os.environ.get('ISSUE_TITLE', '')

# Root directory of the project
ROOT_DIR = Path(__file__).parent.parent.parent
SRC_DIR = ROOT_DIR / 'src'
JAVA_SRC_DIR = SRC_DIR / 'main' / 'java' / 'com' / 'modforge' / 'intellij' / 'plugin'
RESOURCES_DIR = SRC_DIR / 'main' / 'resources'
RESPONSE_FILE = Path('issue_response.txt')

class CommandProcessor:
    def __init__(self, issue_body, issue_title):
        self.issue_body = issue_body
        self.issue_title = issue_title
        self.command = self._extract_command()
        self.changes_made = []
        
    def _extract_command(self):
        """Extract command from issue body"""
        command_match = re.search(r'/command\s+(.*)', self.issue_body, re.IGNORECASE)
        if command_match:
            return command_match.group(1).strip()
        return None
        
    def process(self):
        """Process the command"""
        if not self.command:
            self._respond("No command found. Please use the format `/command [action] [target] [parameters]`.")
            return
            
        logger.info(f"Processing command: {self.command}")
        
        # Parse the command
        action, target, details = self._parse_command()
        
        # Execute the command
        result = self._execute_command(action, target, details)
        
        # Respond to the issue
        if result:
            self._respond(f"Command executed successfully.\n\n**Changes made:**\n" + 
                          "\n".join([f"- {change}" for change in self.changes_made]))
        else:
            self._respond(f"Command execution failed. Please check the logs for details.")
        
    def _parse_command(self):
        """Parse action, target, and details from the command"""
        # Common command patterns
        fix_pattern = re.match(r'fix\s+(error|bug|issue)?\s*in\s+(.+)', self.command, re.IGNORECASE)
        add_pattern = re.match(r'add\s+(feature|functionality|support)\s+(?:"([^"]+)"|([^"\s]+))\s+to\s+(.+)', self.command, re.IGNORECASE)
        improve_pattern = re.match(r'improve\s+(.+)', self.command, re.IGNORECASE)
        document_pattern = re.match(r'document\s+(.+)', self.command, re.IGNORECASE)
        
        if fix_pattern:
            return 'fix', fix_pattern.group(2), fix_pattern.group(1) if fix_pattern.group(1) else 'error'
        elif add_pattern:
            feature = add_pattern.group(2) if add_pattern.group(2) else add_pattern.group(3)
            return 'add', add_pattern.group(4), feature
        elif improve_pattern:
            return 'improve', improve_pattern.group(1), None
        elif document_pattern:
            return 'document', document_pattern.group(1), None
        else:
            # Fallback to AI for more complex command parsing
            try:
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are a command parser for automated development tasks. Extract the action, target, and additional details from the command."},
                        {"role": "user", "content": f"Parse this command into action, target, and details: '{self.command}'\n\nRespond with a JSON object with fields: 'action', 'target', and 'details'."}
                    ]
                )
                
                parsed = json.loads(response.choices[0].message['content'])
                return parsed.get('action'), parsed.get('target'), parsed.get('details')
            except Exception as e:
                logger.error(f"Failed to parse command with AI: {e}")
                return 'unknown', self.command, None
    
    def _execute_command(self, action, target, details):
        """Execute the parsed command"""
        try:
            if action == 'fix':
                return self._fix_issue(target, details)
            elif action == 'add':
                return self._add_feature(target, details)
            elif action == 'improve':
                return self._improve_code(target)
            elif action == 'document':
                return self._generate_documentation(target)
            else:
                logger.warning(f"Unknown action: {action}")
                self._respond(f"Unknown action: {action}. Supported actions are: fix, add, improve, document.")
                return False
        except Exception as e:
            logger.error(f"Error executing command: {e}")
            return False
    
    def _fix_issue(self, target, error_type):
        """Fix an issue in the specified target"""
        # Find the target file
        target_path = self._find_file(target)
        if not target_path:
            self._respond(f"Could not find target file: {target}")
            return False
            
        logger.info(f"Fixing {error_type} in {target_path}")
        
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Ask AI to fix the issue
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development."},
                    {"role": "user", "content": f"Fix any {error_type} in this file based on best practices for IntelliJ plugin development.\n\nFile: {target}\nIssue Description: {self.issue_title}\n\n```java\n{content}\n```\n\nProvide only the complete fixed code without explanations."}
                ]
            )
            
            fixed_code = response.choices[0].message['content'].strip()
            
            # Extract code if wrapped in markdown
            if fixed_code.startswith('```java'):
                fixed_code = fixed_code.split('```java')[1]
                if '```' in fixed_code:
                    fixed_code = fixed_code.split('```')[0]
            elif fixed_code.startswith('```'):
                fixed_code = fixed_code.split('```')[1]
                if '```' in fixed_code:
                    fixed_code = fixed_code.split('```')[0]
                    
            # Write the fixed content
            with open(target_path, 'w') as f:
                f.write(fixed_code)
                
            self.changes_made.append(f"Fixed {error_type} in {target}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to fix issue in {target_path}: {e}")
            return False
    
    def _add_feature(self, target, feature_description):
        """Add a feature to the specified target"""
        # Target could be a component, package, or specific file
        target_path = self._find_file(target)
        
        if not target_path:
            # Check if it's a component or package
            target_package = f"com.modforge.intellij.plugin.{target.lower()}"
            package_dir = JAVA_SRC_DIR / target.lower()
            
            if package_dir.exists() and package_dir.is_dir():
                # It's a package, create a new file in the package
                class_name = ''.join(word.capitalize() for word in feature_description.split())
                target_path = package_dir / f"{class_name}.java"
                
                logger.info(f"Creating new file {target_path} for feature: {feature_description}")
                
                try:
                    # Generate new file using AI
                    response = openai.ChatCompletion.create(
                        model="gpt-4",
                        messages=[
                            {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development."},
                            {"role": "user", "content": f"Create a new Java class for a feature in the ModForge IntelliJ plugin.\n\nPackage: {target_package}\nClass Name: {class_name}\nFeature Description: {feature_description}\n\nProvide only the complete Java code with package, imports, class definition, and implementation."}
                        ]
                    )
                    
                    new_code = response.choices[0].message['content'].strip()
                    
                    # Extract code if wrapped in markdown
                    if new_code.startswith('```java'):
                        new_code = new_code.split('```java')[1]
                        if '```' in new_code:
                            new_code = new_code.split('```')[0]
                    elif new_code.startswith('```'):
                        new_code = new_code.split('```')[1]
                        if '```' in new_code:
                            new_code = new_code.split('```')[0]
                            
                    # Make sure the directory exists
                    os.makedirs(os.path.dirname(target_path), exist_ok=True)
                    
                    # Write the new file
                    with open(target_path, 'w') as f:
                        f.write(new_code)
                        
                    # Update plugin.xml if needed
                    self._update_plugin_xml_for_feature(class_name, feature_description)
                    
                    self.changes_made.append(f"Created new class {class_name} for feature: {feature_description}")
                    return True
                    
                except Exception as e:
                    logger.error(f"Failed to create new feature file: {e}")
                    return False
            else:
                self._respond(f"Could not find target: {target}. Please specify a valid file, package, or component.")
                return False
        else:
            # It's an existing file, modify it
            logger.info(f"Adding feature to existing file {target_path}")
            
            try:
                # Read the file
                with open(target_path, 'r') as f:
                    content = f.read()
                    
                # Ask AI to add the feature
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development."},
                        {"role": "user", "content": f"Add a new feature to this Java file for an IntelliJ plugin.\n\nFeature Description: {feature_description}\n\nCurrent code:\n```java\n{content}\n```\n\nProvide only the complete modified code with the new feature added."}
                    ]
                )
                
                modified_code = response.choices[0].message['content'].strip()
                
                # Extract code if wrapped in markdown
                if modified_code.startswith('```java'):
                    modified_code = modified_code.split('```java')[1]
                    if '```' in modified_code:
                        modified_code = modified_code.split('```')[0]
                elif modified_code.startswith('```'):
                    modified_code = modified_code.split('```')[1]
                    if '```' in modified_code:
                        modified_code = modified_code.split('```')[0]
                        
                # Write the modified content
                with open(target_path, 'w') as f:
                    f.write(modified_code)
                    
                self.changes_made.append(f"Added feature '{feature_description}' to {target}")
                return True
                
            except Exception as e:
                logger.error(f"Failed to add feature to {target_path}: {e}")
                return False
    
    def _improve_code(self, target):
        """Improve code in the specified target"""
        target_path = self._find_file(target)
        if not target_path:
            self._respond(f"Could not find target file: {target}")
            return False
            
        logger.info(f"Improving code in {target_path}")
        
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Ask AI to improve the code
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Improve this code's quality, performance, and readability without changing its functionality."},
                    {"role": "user", "content": f"Improve this Java file's code quality, focusing on readability, maintainability, and performance. Don't change the functionality.\n\nFile: {target}\n\n```java\n{content}\n```\n\nProvide only the complete improved code without explanations."}
                ]
            )
            
            improved_code = response.choices[0].message['content'].strip()
            
            # Extract code if wrapped in markdown
            if improved_code.startswith('```java'):
                improved_code = improved_code.split('```java')[1]
                if '```' in improved_code:
                    improved_code = improved_code.split('```')[0]
            elif improved_code.startswith('```'):
                improved_code = improved_code.split('```')[1]
                if '```' in improved_code:
                    improved_code = improved_code.split('```')[0]
                    
            # Write the improved content
            with open(target_path, 'w') as f:
                f.write(improved_code)
                
            self.changes_made.append(f"Improved code quality in {target}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to improve code in {target_path}: {e}")
            return False
    
    def _generate_documentation(self, target):
        """Generate documentation for the specified target"""
        target_path = self._find_file(target)
        if not target_path:
            self._respond(f"Could not find target file: {target}")
            return False
            
        logger.info(f"Generating documentation for {target_path}")
        
        try:
            # Read the file
            with open(target_path, 'r') as f:
                content = f.read()
                
            # Ask AI to generate documentation
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development, with a focus on comprehensive JavaDoc documentation."},
                    {"role": "user", "content": f"Generate comprehensive JavaDoc documentation for this Java file. Add class-level documentation, method documentation, parameter descriptions, and return value descriptions.\n\nFile: {target}\n\n```java\n{content}\n```\n\nProvide only the complete documented code without explanations."}
                ]
            )
            
            documented_code = response.choices[0].message['content'].strip()
            
            # Extract code if wrapped in markdown
            if documented_code.startswith('```java'):
                documented_code = documented_code.split('```java')[1]
                if '```' in documented_code:
                    documented_code = documented_code.split('```')[0]
            elif documented_code.startswith('```'):
                documented_code = documented_code.split('```')[1]
                if '```' in documented_code:
                    documented_code = documented_code.split('```')[0]
                    
            # Write the documented content
            with open(target_path, 'w') as f:
                f.write(documented_code)
                
            self.changes_made.append(f"Added comprehensive documentation to {target}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to document {target_path}: {e}")
            return False
    
    def _find_file(self, filename):
        """Find a file in the project"""
        # Try exact path first
        if os.path.exists(filename) and os.path.isfile(filename):
            return filename
            
        # Try with standard source dir
        java_path = JAVA_SRC_DIR / filename
        if java_path.exists() and java_path.isfile():
            return java_path
            
        # Try as class name without .java extension
        if not filename.endswith('.java'):
            java_path = JAVA_SRC_DIR / f"{filename}.java"
            if java_path.exists() and java_path.isfile():
                return java_path
                
        # Try recursive search
        for root, _, files in os.walk(JAVA_SRC_DIR):
            for file in files:
                if file == filename or file == f"{filename}.java":
                    return os.path.join(root, file)
                    
        return None
        
    def _update_plugin_xml_for_feature(self, class_name, feature_description):
        """Update plugin.xml to register the new feature if needed"""
        plugin_xml_path = RESOURCES_DIR / 'META-INF' / 'plugin.xml'
        if not plugin_xml_path.exists():
            return
            
        try:
            # Read the plugin.xml file
            with open(plugin_xml_path, 'r') as f:
                content = f.read()
                
            # Ask AI to update the plugin.xml
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Update the plugin.xml file to include a new feature."},
                    {"role": "user", "content": f"Update this plugin.xml file to register a new feature that was added to the ModForge IntelliJ plugin.\n\nClass Name: {class_name}\nFeature Description: {feature_description}\n\nCurrent plugin.xml:\n```xml\n{content}\n```\n\nProvide only the complete updated XML without explanations. Only add what's necessary for this new feature."}
                ]
            )
            
            updated_xml = response.choices[0].message['content'].strip()
            
            # Extract XML if wrapped in markdown
            if updated_xml.startswith('```xml'):
                updated_xml = updated_xml.split('```xml')[1]
                if '```' in updated_xml:
                    updated_xml = updated_xml.split('```')[0]
            elif updated_xml.startswith('```'):
                updated_xml = updated_xml.split('```')[1]
                if '```' in updated_xml:
                    updated_xml = updated_xml.split('```')[0]
                    
            # Write the updated plugin.xml
            with open(plugin_xml_path, 'w') as f:
                f.write(updated_xml)
                
            self.changes_made.append(f"Updated plugin.xml to register new feature '{feature_description}'")
            return True
            
        except Exception as e:
            logger.error(f"Failed to update plugin.xml: {e}")
            return False
    
    def _respond(self, message):
        """Write response to the issue_response.txt file for the GitHub Action to comment on the issue"""
        with open(RESPONSE_FILE, 'w') as f:
            f.write(message)
            
        logger.info(f"Response prepared: {message[:100]}...")

# Run the command processor
if __name__ == "__main__":
    processor = CommandProcessor(ISSUE_BODY, ISSUE_TITLE)
    processor.process()