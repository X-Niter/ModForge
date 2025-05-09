#!/usr/bin/env python3
"""
Automated Development Script for ModForge IntelliJ Plugin

This script runs continuous development tasks on the ModForge codebase,
using AI to fix errors, improve code, generate documentation, and add features.

Usage:
  python run_auto_development.py --task [fix-errors|improve-code|generate-docs|add-feature]
"""

import os
import re
import sys
import json
import argparse
import subprocess
import logging
from pathlib import Path
import openai

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ModForge-AutoDev')

# Initialize OpenAI API
openai.api_key = os.environ.get('OPENAI_API_KEY')
if not openai.api_key:
    logger.error("OPENAI_API_KEY not found in environment variables")
    sys.exit(1)

# Parse arguments
parser = argparse.ArgumentParser(description='Run automated development tasks on ModForge codebase')
parser.add_argument('--task', type=str, default='fix-errors',
                    choices=['fix-errors', 'improve-code', 'generate-docs', 'add-feature'],
                    help='Type of development task to perform')
args = parser.parse_args()

# Root directory of the project
ROOT_DIR = Path(__file__).parent.parent.parent
SRC_DIR = ROOT_DIR / 'src'
JAVA_SRC_DIR = SRC_DIR / 'main' / 'java' / 'com' / 'modforge' / 'intellij' / 'plugin'
RESOURCES_DIR = SRC_DIR / 'main' / 'resources'

class ModForgeAutoDeveloper:
    def __init__(self, task_type):
        self.task_type = task_type
        self.error_log = []
        self.changes_made = []
        
    def run(self):
        """Main execution method for the automated developer"""
        logger.info(f"Starting automated development task: {self.task_type}")
        
        # First, gather information about the codebase
        codebase_info = self.analyze_codebase()
        
        # Execute the chosen task
        if self.task_type == 'fix-errors':
            self.fix_errors(codebase_info)
        elif self.task_type == 'improve-code':
            self.improve_code(codebase_info)
        elif self.task_type == 'generate-docs':
            self.generate_documentation(codebase_info)
        elif self.task_type == 'add-feature':
            self.add_feature(codebase_info)
        else:
            logger.error(f"Unknown task type: {self.task_type}")
            
        # Log summary of changes
        logger.info(f"Completed {self.task_type} task. {len(self.changes_made)} changes made.")
        for change in self.changes_made:
            logger.info(f"- {change}")
            
        return len(self.changes_made) > 0
        
    def analyze_codebase(self):
        """Gather information about the codebase structure and state"""
        logger.info("Analyzing codebase structure...")
        
        # Run gradle build for compilation errors
        try:
            logger.info("Running gradle build to check for errors...")
            result = subprocess.run(['./gradlew', 'build', '--info'], 
                                   cwd=ROOT_DIR,
                                   capture_output=True, 
                                   text=True)
            
            if result.returncode != 0:
                logger.warning("Build failed, capturing errors for analysis")
                self.error_log.append(result.stderr)
            else:
                logger.info("Build successful")
                
        except Exception as e:
            logger.error(f"Failed to run gradle build: {e}")
            
        # Collect Java source files
        java_files = list(JAVA_SRC_DIR.glob('**/*.java'))
        logger.info(f"Found {len(java_files)} Java source files")
        
        # Extract class structure from Java files (simplified)
        classes = {}
        package_structure = {}
        
        for java_file in java_files:
            try:
                with open(java_file, 'r') as f:
                    content = f.read()
                    
                # Extract package
                package_match = re.search(r'package\s+([\w.]+);', content)
                package = package_match.group(1) if package_match else "unknown"
                
                # Extract class name
                class_match = re.search(r'(public|private)\s+(final\s+)?(class|interface|enum)\s+(\w+)', content)
                if class_match:
                    class_name = class_match.group(4)
                    if package not in package_structure:
                        package_structure[package] = []
                    package_structure[package].append(class_name)
                    
                    # Store class info
                    classes[class_name] = {
                        'path': str(java_file),
                        'package': package,
                        'type': class_match.group(3),  # class, interface, or enum
                        'content': content
                    }
            except Exception as e:
                logger.error(f"Error processing {java_file}: {e}")
                
        # Collect information about the plugin.xml file
        plugin_xml_path = RESOURCES_DIR / 'META-INF' / 'plugin.xml'
        plugin_xml_content = None
        if plugin_xml_path.exists():
            try:
                with open(plugin_xml_path, 'r') as f:
                    plugin_xml_content = f.read()
            except Exception as e:
                logger.error(f"Error reading plugin.xml: {e}")
                
        return {
            'classes': classes,
            'package_structure': package_structure,
            'error_log': self.error_log,
            'plugin_xml': plugin_xml_content,
            'java_files': [str(f) for f in java_files]
        }
        
    def fix_errors(self, codebase_info):
        """Fix compilation errors in the codebase"""
        if not self.error_log:
            logger.info("No compilation errors found")
            return
            
        logger.info(f"Analyzing {len(self.error_log)} error logs")
        
        for error_text in self.error_log:
            # Extract individual errors
            error_patterns = re.findall(r'(error|warning):\s+(.*?)\n\s+at\s+(.*?):(\d+)', error_text, re.DOTALL)
            
            for severity, message, file_path, line_num in error_patterns:
                try:
                    self._fix_specific_error(severity, message, file_path, int(line_num), codebase_info)
                except Exception as e:
                    logger.error(f"Failed to fix error in {file_path}: {e}")
    
    def _fix_specific_error(self, severity, message, file_path, line_num, codebase_info):
        """Fix a specific error using AI assistance"""
        # Read the file content
        try:
            with open(file_path, 'r') as f:
                content = f.read().split('\n')
        except Exception as e:
            logger.error(f"Could not read {file_path}: {e}")
            return
            
        # Extract a code snippet around the error (context)
        start_line = max(0, line_num - 10)
        end_line = min(len(content), line_num + 10)
        code_context = '\n'.join(content[start_line:end_line])
        
        # Generate fix using OpenAI
        try:
            logger.info(f"Asking AI to fix {severity}: {message} in {file_path}:{line_num}")
            
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to fix compilation errors in the code."},
                    {"role": "user", "content": f"Fix the following {severity} in a Java file:\n\nError message: {message}\nFile: {file_path}\nLine: {line_num}\n\nCode context:\n```java\n{code_context}\n```\n\nProvide only the corrected code snippet without explanations."}
                ]
            )
            
            fixed_code = response.choices[0].message['content'].strip()
            
            # Extract just the code (remove markdown formatting if present)
            if fixed_code.startswith('```java'):
                fixed_code = fixed_code.split('```java')[1]
                if '```' in fixed_code:
                    fixed_code = fixed_code.split('```')[0]
            
            # Apply the fix
            new_content = content.copy()
            # Replace the relevant lines with the fix (estimate range based on the AI response)
            code_lines = fixed_code.strip().split('\n')
            target_start = max(0, line_num - 3)
            target_end = min(len(content), line_num + len(code_lines))
            
            new_content[target_start:target_end] = code_lines
            
            # Write the fixed content back to the file
            with open(file_path, 'w') as f:
                f.write('\n'.join(new_content))
                
            self.changes_made.append(f"Fixed {severity} in {file_path}:{line_num}: {message[:50]}...")
            logger.info(f"Fixed {severity} in {file_path}:{line_num}")
            
        except Exception as e:
            logger.error(f"Failed to fix error with AI: {e}")
            
    def improve_code(self, codebase_info):
        """Improve code quality without changing functionality"""
        # Select files to improve (we'll limit to 3 per run to avoid too many changes)
        java_files = codebase_info['java_files'][:3]
        
        for file_path in java_files:
            try:
                with open(file_path, 'r') as f:
                    content = f.read()
                    
                # Ask AI to improve the code
                logger.info(f"Improving code quality in {file_path}")
                
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to improve code quality without changing functionality."},
                        {"role": "user", "content": f"Improve the code quality of this Java file without changing its functionality. Focus on readability, maintainability, and performance:\n\n```java\n{content}\n```\n\nProvide only the improved code without explanations."}
                    ]
                )
                
                improved_code = response.choices[0].message['content'].strip()
                
                # Extract just the code (remove markdown formatting if present)
                if improved_code.startswith('```java'):
                    improved_code = improved_code.split('```java')[1]
                    if '```' in improved_code:
                        improved_code = improved_code.split('```')[0]
                elif improved_code.startswith('```'):
                    improved_code = improved_code.split('```')[1]
                    if '```' in improved_code:
                        improved_code = improved_code.split('```')[0]
                
                # Write the improved code back to the file
                with open(file_path, 'w') as f:
                    f.write(improved_code)
                    
                self.changes_made.append(f"Improved code quality in {file_path}")
                logger.info(f"Improved code quality in {file_path}")
                
            except Exception as e:
                logger.error(f"Failed to improve code in {file_path}: {e}")
                
    def generate_documentation(self, codebase_info):
        """Generate or improve documentation for classes"""
        # Select classes that might need better documentation
        classes = list(codebase_info['classes'].items())[:3]  # Limit to 3 per run
        
        for class_name, class_info in classes:
            try:
                file_path = class_info['path']
                content = class_info['content']
                
                # Check if class already has detailed JavaDoc
                if '/**' in content and '@author' in content and '@since' in content:
                    logger.info(f"Class {class_name} already has good documentation")
                    continue
                
                # Ask AI to generate documentation
                logger.info(f"Generating documentation for {class_name}")
                
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to generate comprehensive JavaDoc documentation."},
                        {"role": "user", "content": f"Generate comprehensive JavaDoc documentation for this Java class, including class-level docs, method docs, and parameter descriptions:\n\n```java\n{content}\n```\n\nProvide only the fully documented code without explanations."}
                    ]
                )
                
                documented_code = response.choices[0].message['content'].strip()
                
                # Extract just the code (remove markdown formatting if present)
                if documented_code.startswith('```java'):
                    documented_code = documented_code.split('```java')[1]
                    if '```' in documented_code:
                        documented_code = documented_code.split('```')[0]
                elif documented_code.startswith('```'):
                    documented_code = documented_code.split('```')[1]
                    if '```' in documented_code:
                        documented_code = documented_code.split('```')[0]
                
                # Write the documented code back to the file
                with open(file_path, 'w') as f:
                    f.write(documented_code)
                    
                self.changes_made.append(f"Added documentation to {class_name}")
                logger.info(f"Added documentation to {class_name}")
                
            except Exception as e:
                logger.error(f"Failed to generate documentation for {class_name}: {e}")
                
    def add_feature(self, codebase_info):
        """Add a new feature to the codebase"""
        # For "add feature", we'll need to be more creative
        # We'll generate a random improvement idea based on the codebase analysis
        
        # First, get a deep understanding of the project
        try:
            # Summarize the project structure
            packages = list(codebase_info['package_structure'].keys())
            package_summary = '\n'.join([f"- {pkg}: {', '.join(classes[:5])}" for pkg, classes in codebase_info['package_structure'].items()])
            
            # Ask AI for feature suggestions
            logger.info("Generating feature idea based on codebase analysis")
            
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to suggest a valuable, self-contained feature to add to the ModForge IntelliJ plugin."},
                    {"role": "user", "content": f"Based on the following package structure of a ModForge IntelliJ Plugin that helps with Minecraft mod development, suggest ONE specific, well-defined feature to implement that would add value to the plugin. The feature should be small enough to implement in one pass.\n\nPackage structure:\n{package_summary}\n\nRespond with a JSON object with these fields: 'feature_name', 'description', 'implementation_plan' (with specific files to modify), and 'priority' (1-5, with 5 being highest)."}
                ]
            )
            
            feature_suggestion = response.choices[0].message['content'].strip()
            
            # Parse the JSON response
            try:
                feature_json = json.loads(feature_suggestion.strip('```json').strip('```').strip())
                feature_name = feature_json['feature_name']
                description = feature_json['description']
                implementation_plan = feature_json['implementation_plan']
                priority = feature_json['priority']
                
                logger.info(f"Feature idea: {feature_name} (Priority: {priority})")
                logger.info(f"Description: {description}")
                
                # Now let's implement the feature
                self._implement_feature(feature_json, codebase_info)
                
            except json.JSONDecodeError:
                logger.error("Could not parse feature suggestion as JSON")
                logger.info(f"Raw suggestion: {feature_suggestion}")
                
        except Exception as e:
            logger.error(f"Failed to generate feature idea: {e}")
            
    def _implement_feature(self, feature, codebase_info):
        """Implement a specific feature based on the AI suggestion"""
        # This is a simplified implementation - in a real system, this would be more sophisticated
        
        for file_path in feature.get('implementation_plan', {}).get('files_to_modify', []):
            if not os.path.exists(file_path):
                # Check if it's a new file to create
                if file_path.endswith('.java'):
                    # Generate the new file content
                    class_name = os.path.basename(file_path).replace('.java', '')
                    package_path = os.path.dirname(file_path).replace('/', '.').replace('src.main.java.', '')
                    
                    logger.info(f"Creating new class {class_name} in package {package_path}")
                    
                    try:
                        response = openai.ChatCompletion.create(
                            model="gpt-4",
                            messages=[
                                {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to create a new Java class."},
                                {"role": "user", "content": f"Create a new Java class for the ModForge IntelliJ plugin:\n\nClass name: {class_name}\nPackage: {package_path}\nFeature to implement: {feature['description']}\n\nProvide only the complete Java code with proper package declaration, imports, and comprehensive JavaDoc."}
                            ]
                        )
                        
                        new_class_code = response.choices[0].message['content'].strip()
                        
                        # Extract just the code (remove markdown formatting if present)
                        if new_class_code.startswith('```java'):
                            new_class_code = new_class_code.split('```java')[1]
                            if '```' in new_class_code:
                                new_class_code = new_class_code.split('```')[0]
                        elif new_class_code.startswith('```'):
                            new_class_code = new_class_code.split('```')[1]
                            if '```' in new_class_code:
                                new_class_code = new_class_code.split('```')[0]
                        
                        # Create directory if needed
                        os.makedirs(os.path.dirname(file_path), exist_ok=True)
                        
                        # Write the new class to file
                        with open(file_path, 'w') as f:
                            f.write(new_class_code)
                            
                        self.changes_made.append(f"Created new class {class_name} for feature: {feature['feature_name']}")
                        logger.info(f"Created new class {class_name}")
                        
                    except Exception as e:
                        logger.error(f"Failed to create new class {class_name}: {e}")
            else:
                # Modify existing file
                try:
                    with open(file_path, 'r') as f:
                        content = f.read()
                        
                    logger.info(f"Modifying existing file {file_path}")
                    
                    response = openai.ChatCompletion.create(
                        model="gpt-4",
                        messages=[
                            {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to modify a Java file to implement a new feature."},
                            {"role": "user", "content": f"Modify this Java file to implement the following feature:\n\nFeature: {feature['description']}\n\nCurrent code:\n```java\n{content}\n```\n\nProvide only the complete modified code without explanations."}
                        ]
                    )
                    
                    modified_code = response.choices[0].message['content'].strip()
                    
                    # Extract just the code (remove markdown formatting if present)
                    if modified_code.startswith('```java'):
                        modified_code = modified_code.split('```java')[1]
                        if '```' in modified_code:
                            modified_code = modified_code.split('```')[0]
                    elif modified_code.startswith('```'):
                        modified_code = modified_code.split('```')[1]
                        if '```' in modified_code:
                            modified_code = modified_code.split('```')[0]
                    
                    # Write the modified code back to the file
                    with open(file_path, 'w') as f:
                        f.write(modified_code)
                        
                    self.changes_made.append(f"Modified {file_path} for feature: {feature['feature_name']}")
                    logger.info(f"Modified {file_path}")
                    
                except Exception as e:
                    logger.error(f"Failed to modify {file_path}: {e}")
                    
        # Update plugin.xml if needed
        if feature.get('implementation_plan', {}).get('update_plugin_xml', False):
            plugin_xml_path = RESOURCES_DIR / 'META-INF' / 'plugin.xml'
            if plugin_xml_path.exists():
                try:
                    with open(plugin_xml_path, 'r') as f:
                        plugin_xml = f.read()
                        
                    logger.info("Updating plugin.xml for the new feature")
                    
                    response = openai.ChatCompletion.create(
                        model="gpt-4",
                        messages=[
                            {"role": "system", "content": "You are an expert Java developer specializing in IntelliJ plugin development. Your task is to update the plugin.xml file to register a new feature."},
                            {"role": "user", "content": f"Update this plugin.xml file to include the new feature:\n\nFeature: {feature['description']}\nFeature name: {feature['feature_name']}\n\nCurrent plugin.xml:\n```xml\n{plugin_xml}\n```\n\nProvide only the complete modified XML without explanations."}
                        ]
                    )
                    
                    modified_xml = response.choices[0].message['content'].strip()
                    
                    # Extract just the XML (remove markdown formatting if present)
                    if modified_xml.startswith('```xml'):
                        modified_xml = modified_xml.split('```xml')[1]
                        if '```' in modified_xml:
                            modified_xml = modified_xml.split('```')[0]
                    elif modified_xml.startswith('```'):
                        modified_xml = modified_xml.split('```')[1]
                        if '```' in modified_xml:
                            modified_xml = modified_xml.split('```')[0]
                    
                    # Write the modified XML back to the file
                    with open(plugin_xml_path, 'w') as f:
                        f.write(modified_xml)
                        
                    self.changes_made.append(f"Updated plugin.xml for feature: {feature['feature_name']}")
                    logger.info("Updated plugin.xml")
                    
                except Exception as e:
                    logger.error(f"Failed to update plugin.xml: {e}")

# Run the automated development process
if __name__ == "__main__":
    developer = ModForgeAutoDeveloper(args.task)
    developer.run()