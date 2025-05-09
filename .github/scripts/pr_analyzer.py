#!/usr/bin/env python3
"""
Pull Request Analyzer for ModForge

This script analyzes pull requests to provide intelligent feedback and suggestions
for Minecraft mod development. It integrates with the OpenAI API to provide
AI-powered code reviews and improvement suggestions.
"""

import os
import sys
import json
import re
import subprocess
import logging
from pathlib import Path
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ModForge-PRAnalyzer')

# Initialize OpenAI API
openai.api_key = os.environ.get('OPENAI_API_KEY')
if not openai.api_key:
    logger.error("OPENAI_API_KEY not found in environment variables")
    sys.exit(1)

# GitHub setup
github_token = os.environ.get('GITHUB_TOKEN')
if not github_token:
    logger.error("GITHUB_TOKEN not found in environment variables")
    sys.exit(1)

repo_full_name = os.environ.get('GITHUB_REPOSITORY')
pr_number = int(os.environ.get('PR_NUMBER'))

# GitHub API base URL
api_url = f"https://api.github.com/repos/{repo_full_name}"

# Headers for GitHub API requests
headers = {
    'Authorization': f'token {github_token}',
    'Accept': 'application/vnd.github.v3+json'
}

def get_pr_files():
    """Get the list of files modified in the PR"""
    url = f"{api_url}/pulls/{pr_number}/files"
    response = requests.get(url, headers=headers)
    
    if response.status_code != 200:
        logger.error(f"Failed to get PR files: {response.status_code}")
        logger.error(response.text)
        return []
    
    return response.json()

def get_file_content(file_path):
    """Get the content of a file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        logger.error(f"Error reading file {file_path}: {str(e)}")
        return None

def get_diff_for_file(file_path):
    """Get the diff for a specific file"""
    try:
        # Get the diff for this specific file
        result = subprocess.run(
            ['git', 'diff', 'origin/main', '--', file_path],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout
    except subprocess.CalledProcessError as e:
        logger.error(f"Error getting diff for {file_path}: {str(e)}")
        return None

def analyze_java_file(file_path, file_content, file_diff):
    """Analyze a Java file using OpenAI for mod development feedback"""
    try:
        # Extract package and class name
        package_match = re.search(r'package\s+([\w.]+);', file_content)
        package = package_match.group(1) if package_match else "unknown"
        
        class_match = re.search(r'(public|private)\s+(class|interface|enum)\s+(\w+)', file_content)
        class_name = class_match.group(3) if class_match else Path(file_path).stem
        
        # Prepare prompt for OpenAI
        prompt = f"""
You are an expert Minecraft mod developer specializing in IntelliJ plugin development.
You're reviewing a pull request that modifies a file in the ModForge project.

File: {file_path}
Package: {package}
Class: {class_name}

Diff:
```diff
{file_diff}
```

Full file content:
```java
{file_content}
```

Provide a thorough code review with the following sections:

1. SUMMARY: A brief overview of the changes and their purpose.

2. CODE QUALITY: Analyze the code quality, focusing on:
   - Readability
   - Maintainability
   - Adhering to Java coding standards
   - Use of appropriate design patterns

3. MINECRAFT MOD SPECIFIC: Evaluate if the code follows Minecraft mod development best practices:
   - Proper use of Minecraft/Forge/Fabric APIs
   - Performance considerations specific to Minecraft
   - Compatibility with different mod loaders

4. INTELLIJ PLUGIN SPECIFIC: Assess the code for IntelliJ plugin development standards:
   - Proper use of IntelliJ Platform APIs
   - UI/UX considerations for the IDE
   - Thread safety in IDE operations

5. SUGGESTIONS: Provide specific, actionable suggestions for improvements. Include code snippets where appropriate.

6. POTENTIAL ISSUES: Identify any potential bugs, performance issues, or other concerns.

Format your response as a structured review. Be thorough but concise. Focus on being helpful and educational.
"""

        # Call OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": "You are an expert Minecraft mod developer and code reviewer."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,
            max_tokens=1500
        )
        
        review_content = response.choices[0].message['content'].strip()
        return review_content
        
    except Exception as e:
        logger.error(f"Error analyzing Java file: {str(e)}")
        return f"Error analyzing Java file: {str(e)}"

def analyze_gradle_file(file_path, file_content, file_diff):
    """Analyze a Gradle file for build configuration best practices"""
    try:
        # Prepare prompt for OpenAI
        prompt = f"""
You are an expert build engineer specializing in Gradle for Minecraft mod development.
You're reviewing a pull request that modifies a Gradle build file in the ModForge project.

File: {file_path}

Diff:
```diff
{file_diff}
```

Full file content:
```groovy
{file_content}
```

Provide a thorough review with the following sections:

1. SUMMARY: A brief overview of the build configuration changes and their purpose.

2. BUILD SETUP: Evaluate the overall build configuration:
   - Dependency management
   - Plugin configuration
   - Task definitions and customizations

3. MINECRAFT MOD SPECIFIC: Assess Minecraft mod related build configurations:
   - Minecraft version compatibility
   - Forge/Fabric/Quilt setup
   - Cross-loader configurations (if applicable)

4. INTELLIJ PLUGIN SPECIFIC: Evaluate IntelliJ plugin build configuration:
   - Plugin descriptor
   - Dependency setup for IntelliJ Platform
   - Plugin deployment configurations

5. SUGGESTIONS: Provide specific, actionable suggestions for improvements.

6. POTENTIAL ISSUES: Identify any potential build problems or compatibility issues.

Format your response as a structured review. Be thorough but concise. Focus on being helpful and educational.
"""

        # Call OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": "You are an expert build engineer for Minecraft mods."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,
            max_tokens=1500
        )
        
        review_content = response.choices[0].message['content'].strip()
        return review_content
        
    except Exception as e:
        logger.error(f"Error analyzing Gradle file: {str(e)}")
        return f"Error analyzing Gradle file: {str(e)}"

def analyze_xml_file(file_path, file_content, file_diff):
    """Analyze an XML file (plugin descriptor, configurations, etc.)"""
    try:
        # Prepare prompt for OpenAI
        prompt = f"""
You are an expert IntelliJ plugin developer specializing in XML configurations.
You're reviewing a pull request that modifies an XML file in the ModForge project.

File: {file_path}

Diff:
```diff
{file_diff}
```

Full file content:
```xml
{file_content}
```

Provide a thorough review with the following sections:

1. SUMMARY: A brief overview of the XML configuration changes and their purpose.

2. STRUCTURE: Evaluate the XML structure and organization:
   - Proper element nesting
   - Attribute usage
   - XML standards compliance

3. INTELLIJ PLUGIN SPECIFIC: If this is an IntelliJ plugin descriptor or configuration:
   - Extension points
   - Action definitions
   - Service declarations
   - UI component configurations

4. MINECRAFT MOD SPECIFIC: If this relates to Minecraft mod configuration:
   - Proper mod metadata
   - Resource configurations
   - Data file structures

5. SUGGESTIONS: Provide specific, actionable suggestions for improvements.

6. POTENTIAL ISSUES: Identify any potential problems or compatibility issues.

Format your response as a structured review. Be thorough but concise. Focus on being helpful and educational.
"""

        # Call OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": "You are an expert XML configuration reviewer for IntelliJ plugins."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,
            max_tokens=1500
        )
        
        review_content = response.choices[0].message['content'].strip()
        return review_content
        
    except Exception as e:
        logger.error(f"Error analyzing XML file: {str(e)}")
        return f"Error analyzing XML file: {str(e)}"

def post_review_comment(file_path, review_content):
    """Post a review comment on the PR"""
    try:
        # Create a review comment
        url = f"{api_url}/pulls/{pr_number}/reviews"
        
        # Prepare the comment data
        data = {
            "body": f"## ModForge AI Review for `{file_path}`\n\n{review_content}\n\n_This review was automatically generated by ModForge AI._",
            "event": "COMMENT"
        }
        
        # Post the comment
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 200 or response.status_code == 201:
            logger.info(f"Successfully posted review for {file_path}")
        else:
            logger.error(f"Error posting review: {response.status_code}")
            logger.error(response.text)
            
    except Exception as e:
        logger.error(f"Error posting review comment: {str(e)}")

def summarize_changes(pr_files):
    """Generate a summary of the PR changes"""
    try:
        # Extract file types and counts
        file_types = {}
        for file in pr_files:
            ext = Path(file['filename']).suffix.lower().lstrip('.')
            file_types[ext] = file_types.get(ext, 0) + 1
            
        # Prepare file summary for OpenAI
        file_summary = "\n".join([f"- {count} {ext} files" for ext, count in file_types.items()])
        
        # Generate summary using OpenAI
        prompt = f"""
You are an expert Minecraft mod developer analyzing a pull request.
Summarize the following changes in a pull request to the ModForge project:

Files changed:
{file_summary}

Individual files:
{json.dumps([f['filename'] for f in pr_files], indent=2)}

Based on these files, provide:
1. A concise summary of what this PR likely aims to accomplish
2. Key areas of the codebase being modified
3. Potential impact on the overall project

Format your response as a structured summary suitable for a PR review comment.
Keep it concise but informative, focusing on the big picture.
"""

        # Call OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": "You are an expert Minecraft mod developer."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,
            max_tokens=800
        )
        
        summary_content = response.choices[0].message['content'].strip()
        return summary_content
        
    except Exception as e:
        logger.error(f"Error generating summary: {str(e)}")
        return "Error generating summary. Please check the workflow logs for details."

def post_summary_comment(summary_content, review_count):
    """Post a summary comment on the PR"""
    try:
        # Create an issue comment (PR comment)
        url = f"{api_url}/issues/{pr_number}/comments"
        
        # Prepare the comment data
        data = {
            "body": f"""# ModForge AI PR Analysis

{summary_content}

---

**{review_count} files have been individually reviewed by ModForge AI.**

_This analysis was automatically generated. The AI may not have full context of your project's goals or requirements._
"""
        }
        
        # Post the comment
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 201:
            logger.info("Successfully posted summary comment")
        else:
            logger.error(f"Error posting summary comment: {response.status_code}")
            logger.error(response.text)
            
    except Exception as e:
        logger.error(f"Error posting summary comment: {str(e)}")

def suggest_improvements(pr_files):
    """Suggest general improvements for the PR"""
    try:
        # Prepare file list for OpenAI
        file_list = "\n".join([f"- {file['filename']}" for file in pr_files])
        
        # Generate suggestions using OpenAI
        prompt = f"""
You are an expert Minecraft mod developer reviewing code for a pull request.
Based on the following list of files being changed in a PR to the ModForge project:

{file_list}

Provide 3-5 specific suggestions for how this PR could be improved. Consider:

1. Code organization and architecture
2. Best practices for Minecraft mod development
3. Best practices for IntelliJ plugin development
4. Performance optimizations
5. Testing strategies

For each suggestion:
1. Identify a specific area for improvement
2. Explain why it matters
3. Provide a concrete, actionable recommendation
4. If possible, include a brief code example

Focus on being constructive and educational. Provide suggestions that would be valuable
regardless of the specific implementation details.
"""

        # Call OpenAI API
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": "You are an expert code reviewer for Minecraft mod development."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.3,
            max_tokens=1000
        )
        
        suggestion_content = response.choices[0].message['content'].strip()
        
        # Post suggestions as a comment
        url = f"{api_url}/issues/{pr_number}/comments"
        data = {
            "body": f"""## ModForge AI Suggestions for Improvement

{suggestion_content}

_These suggestions are provided automatically based on an analysis of the changed files, without reviewing the actual code. Take them as general best practices rather than specific fixes for your implementation._
"""
        }
        
        # Post the comment
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 201:
            logger.info("Successfully posted improvement suggestions")
        else:
            logger.error(f"Error posting improvement suggestions: {response.status_code}")
            logger.error(response.text)
            
    except Exception as e:
        logger.error(f"Error suggesting improvements: {str(e)}")

def main():
    logger.info(f"Analyzing PR #{pr_number} for repository {repo_full_name}")
    
    # Get the list of files modified in the PR
    pr_files = get_pr_files()
    
    if not pr_files:
        logger.error("No files found in the PR")
        sys.exit(1)
        
    logger.info(f"Found {len(pr_files)} files in the PR")
    
    # Analyze each file
    review_count = 0
    for file in pr_files:
        file_path = file['filename']
        logger.info(f"Analyzing {file_path}...")
        
        # Skip files that are too large or deleted
        if file['status'] == 'removed' or int(file.get('size', 0)) > 50000:
            logger.info(f"Skipping {file_path} - file is removed or too large")
            continue
            
        # Get file content
        file_content = get_file_content(file_path)
        if not file_content:
            logger.warning(f"Could not read content for {file_path}")
            continue
            
        # Get file diff
        file_diff = get_diff_for_file(file_path)
        if not file_diff:
            logger.warning(f"Could not get diff for {file_path}")
            continue
            
        # Analyze based on file type
        review_content = None
        extension = Path(file_path).suffix.lower()
        
        if extension == '.java':
            review_content = analyze_java_file(file_path, file_content, file_diff)
        elif extension == '.gradle' or extension == '.kts':
            review_content = analyze_gradle_file(file_path, file_content, file_diff)
        elif extension == '.xml':
            review_content = analyze_xml_file(file_path, file_content, file_diff)
        else:
            logger.info(f"Skipping {file_path} - unsupported file type")
            continue
            
        # Post review comment
        if review_content:
            post_review_comment(file_path, review_content)
            review_count += 1
    
    # Generate and post a summary
    if review_count > 0:
        summary_content = summarize_changes(pr_files)
        post_summary_comment(summary_content, review_count)
        
        # Suggest general improvements
        suggest_improvements(pr_files)
    
    logger.info("PR analysis completed successfully")

if __name__ == "__main__":
    main()