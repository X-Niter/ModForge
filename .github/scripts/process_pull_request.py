#!/usr/bin/env python3
"""
Pull Request Processor for ModForge

This script analyzes pull requests, makes suggestions, improvements,
and can automatically fix issues in the code.
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
logger = logging.getLogger('ModForge-PRProcessor')

# Initialize OpenAI API
openai.api_key = os.environ.get('OPENAI_API_KEY')
if not openai.api_key:
    logger.error("OPENAI_API_KEY not found in environment variables")
    sys.exit(1)

# Get PR details from environment variables
PR_TITLE = os.environ.get('PR_TITLE', '')
PR_BODY = os.environ.get('PR_BODY', '')
PR_NUMBER = os.environ.get('PR_NUMBER', '0')

# Root directory of the project
ROOT_DIR = Path(__file__).parent.parent.parent
SRC_DIR = ROOT_DIR / 'src'
JAVA_SRC_DIR = SRC_DIR / 'main' / 'java' / 'com' / 'modforge' / 'intellij' / 'plugin'
RESOURCES_DIR = SRC_DIR / 'main' / 'resources'
RESPONSE_FILE = Path('pr_response.txt')

class PullRequestProcessor:
    def __init__(self, pr_title, pr_body, pr_number):
        self.pr_title = pr_title
        self.pr_body = pr_body
        self.pr_number = pr_number
        self.changes_made = []
        self.suggestions = []
        
    def process(self):
        """Process the pull request"""
        logger.info(f"Processing PR #{self.pr_number}: {self.pr_title}")
        
        # Get the changed files in this PR
        changed_files = self._get_changed_files()
        
        if not changed_files:
            self._respond("No changed files found in this pull request.")
            return
            
        # Analyze the changes
        self._analyze_changes(changed_files)
        
        # Generate improvements and fix issues
        self._improve_code(changed_files)
        
        # Report back with suggestions and changes
        self._generate_report()
        
    def _get_changed_files(self):
        """Get the list of files changed in this PR"""
        try:
            # Get the files changed in this PR using git
            result = subprocess.run(
                ["git", "diff", "--name-only", "origin/main...HEAD"],
                capture_output=True,
                text=True,
                check=True
            )
            
            files = result.stdout.strip().split("\n")
            
            # Filter out non-source files and empty entries
            source_files = [f for f in files if f and (f.endswith('.java') or f.endswith('.xml') or f.endswith('.gradle'))]
            
            logger.info(f"Found {len(source_files)} changed source files")
            return source_files
            
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to get changed files: {e}")
            return []
            
    def _analyze_changes(self, changed_files):
        """Analyze the changes in the PR"""
        logger.info("Analyzing changes in pull request")
        
        pr_analysis = {}
        
        try:
            # Combine PR title and body for context
            pr_context = f"PR Title: {self.pr_title}\nPR Body: {self.pr_body}"
            
            # For each changed file, get a diff and analyze it
            for file_path in changed_files:
                if not os.path.exists(file_path):
                    logger.warning(f"File {file_path} does not exist")
                    continue
                
                # Get the diff for this file
                diff_result = subprocess.run(
                    ["git", "diff", "origin/main", "--", file_path],
                    capture_output=True,
                    text=True,
                    check=True
                )
                
                diff = diff_result.stdout
                
                # Read the current content of the file
                with open(file_path, 'r') as f:
                    content = f.read()
                
                # Analyze the changes using AI
                try:
                    response = openai.ChatCompletion.create(
                        model="gpt-4",
                        messages=[
                            {"role": "system", "content": "You are a Java developer specialized in IntelliJ plugin development. Analyze code changes in pull requests and provide insights."},
                            {"role": "user", "content": f"Analyze these changes in file {file_path}:\n\nPR Context:\n{pr_context}\n\nDiff:\n```diff\n{diff}\n```\n\nCurrent content:\n```\n{content}\n```\n\nProvide your analysis in JSON format with these fields: 'potential_issues' (array), 'suggestions' (array), 'code_quality' (1-5), and 'needs_improvement' (boolean)."}
                        ]
                    )
                    
                    # Extract and parse the response
                    analysis_text = response.choices[0].message['content']
                    
                    # Try to extract JSON from the response
                    json_match = re.search(r'```json\n(.*?)\n```', analysis_text, re.DOTALL)
                    if json_match:
                        analysis_json = json.loads(json_match.group(1))
                    else:
                        # Try without markdown formatting
                        analysis_json = json.loads(analysis_text)
                    
                    # Store the analysis for this file
                    pr_analysis[file_path] = analysis_json
                    
                    # Collect suggestions
                    if 'suggestions' in analysis_json:
                        for suggestion in analysis_json['suggestions']:
                            self.suggestions.append(f"{file_path}: {suggestion}")
                    
                    logger.info(f"Completed analysis of {file_path}")
                    
                except Exception as e:
                    logger.error(f"Failed to analyze {file_path}: {e}")
                    continue
                    
        except Exception as e:
            logger.error(f"Failed to analyze PR changes: {e}")
            
        return pr_analysis
        
    def _improve_code(self, changed_files):
        """Make improvements to the code based on analysis"""
        logger.info("Applying automated improvements to code")
        
        for file_path in changed_files:
            if not os.path.exists(file_path):
                continue
                
            try:
                # Read the current content
                with open(file_path, 'r') as f:
                    content = f.read()
                    
                file_ext = file_path.split('.')[-1].lower()
                
                # Only process certain file types
                if file_ext not in ['java', 'xml', 'gradle']:
                    continue
                    
                # Get suggested improvements from AI
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages=[
                        {"role": "system", "content": "You are a Java developer specialized in IntelliJ plugin development. Make improvements to code while preserving functionality."},
                        {"role": "user", "content": f"Improve this code in {file_path} while preserving its functionality. Focus on code quality, style, and best practices.\n\nCurrent content:\n```\n{content}\n```\n\nProvide only the improved code without any explanation."}
                    ]
                )
                
                improved_code = response.choices[0].message['content']
                
                # Extract just the code if it's wrapped in markdown
                code_match = re.search(r'```(?:\w+)?\n(.*?)\n```', improved_code, re.DOTALL)
                if code_match:
                    improved_code = code_match.group(1)
                
                # Compare the improved code with the original
                if improved_code != content:
                    # Write the improved code
                    with open(file_path, 'w') as f:
                        f.write(improved_code)
                        
                    # Stage the changes
                    subprocess.run(["git", "add", file_path], check=True)
                    
                    self.changes_made.append(f"Improved code quality in {file_path}")
                    logger.info(f"Improved code in {file_path}")
                    
            except Exception as e:
                logger.error(f"Failed to improve {file_path}: {e}")
    
    def _generate_report(self):
        """Generate a report of changes and suggestions"""
        logger.info("Generating report")
        
        report = []
        report.append(f"## ModForge AI Analysis: PR #{self.pr_number}")
        report.append("")
        
        if self.changes_made:
            report.append("### 🤖 Automated Improvements Made")
            report.append("")
            for change in self.changes_made:
                report.append(f"- ✅ {change}")
            report.append("")
        
        if self.suggestions:
            report.append("### 💡 Suggestions")
            report.append("")
            for suggestion in self.suggestions:
                report.append(f"- 📌 {suggestion}")
            report.append("")
        
        if not self.changes_made and not self.suggestions:
            report.append("### ✨ Code Quality Review")
            report.append("")
            report.append("The code in this PR looks good! No issues or suggestions found.")
            report.append("")
        
        report.append("### 🔍 What's Next?")
        report.append("")
        report.append("- If you agree with the automated improvements, no action needed.")
        report.append("- For additional changes, update your PR as normal.")
        report.append("- Need help? Add a comment with `/help [your question]`.")
        
        # Write the report to the response file
        self._respond("\n".join(report))
    
    def _respond(self, message):
        """Write response to the pr_response.txt file for the GitHub Action to comment on the PR"""
        with open(RESPONSE_FILE, 'w') as f:
            f.write(message)
            
        logger.info(f"Response prepared with {len(message)} characters")

# Run the pull request processor
if __name__ == "__main__":
    processor = PullRequestProcessor(PR_TITLE, PR_BODY, PR_NUMBER)
    processor.process()