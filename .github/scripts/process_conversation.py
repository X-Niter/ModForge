#!/usr/bin/env python3

import os
import sys
import json
import re
import requests
import subprocess
import random
import time
import datetime
from pathlib import Path

# Get environment variables
openai_api_key = os.environ.get('OPENAI_API_KEY')
github_token = os.environ.get('GITHUB_TOKEN')
conversation_type = os.environ.get('CONVERSATION_TYPE')
conversation_id = os.environ.get('CONVERSATION_ID')
is_stale = os.environ.get('IS_STALE', 'false').lower() == 'true'
repo = os.environ.get('GITHUB_REPOSITORY')

# Validate required environment variables
if not all([openai_api_key, github_token, conversation_type, conversation_id, repo]):
    print("Missing required environment variables")
    sys.exit(1)

# Setup GitHub API
api_url = f"https://api.github.com/repos/{repo}"
headers = {
    'Authorization': f'token {github_token}',
    'Accept': 'application/vnd.github.v3+json'
}

# Import OpenAI with newer client library
import openai
client = openai.OpenAI(api_key=openai_api_key)

# Define command patterns
COMMAND_PATTERNS = {
    'fix': r'/fix\s+(?:"([^"]+)"|([^\s"]+))',
    'improve': r'/improve\s+(?:"([^"]+)"|([^\s"]+))',
    'document': r'/document\s+(?:"([^"]+)"|([^\s"]+))',
    'add': r'/add\s+(?:"([^"]+)"|([^\s"]+))\s+to\s+(?:"([^"]+)"|([^\s"]+))',
    'analyze': r'/analyze\s+(?:"([^"]+)"|([^\s"]+))',
    'explain': r'/explain\s+(?:"([^"]+)"|([^\s"]+))',
    'implement': r'/implement\s+(?:all|([0-9,\s]+))',
    'refactor': r'/refactor\s+(?:"([^"]+)"|([^\s"]+))',
    'test': r'/test\s+(?:"([^"]+)"|([^\s"]+))',
    'help': r'/help'
}

def fetch_conversation_history():
    """Fetch the full conversation history"""
    url = f"{api_url}/issues/{conversation_id}/comments"
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Failed to fetch comments: {response.status_code}")
        return []

    comments = response.json()

    # Get the issue/PR details too
    if conversation_type == "issue":
        url = f"{api_url}/issues/{conversation_id}"
    else:
        url = f"{api_url}/pulls/{conversation_id}"

    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Failed to fetch conversation details: {response.status_code}")
        return []

    conversation = response.json()

    # Format the history
    history = [{
        'author': conversation.get('user', {}).get('login', 'Unknown'),
        'content': conversation.get('body', ''),
        'created_at': conversation.get('created_at', ''),
        'is_original': True
    }]

    for comment in comments:
        history.append({
            'author': comment.get('user', {}).get('login', 'Unknown'),
            'content': comment.get('body', ''),
            'created_at': comment.get('created_at', ''),
            'is_original': False
        })

    # Sort by creation date
    history.sort(key=lambda x: x['created_at'])

    return history

def detect_commands(text):
    """Detect commands in text"""
    commands = []

    for cmd_type, pattern in COMMAND_PATTERNS.items():
        matches = re.finditer(pattern, text, re.MULTILINE)
        for match in matches:
            if cmd_type in ['add', 'implement']:
                # Special handling for commands with multiple arguments
                if cmd_type == 'add':
                    target1 = match.group(1) if match.group(1) else match.group(2)
                    target2 = match.group(3) if match.group(3) else match.group(4)
                    commands.append((cmd_type, {'feature': target1, 'target': target2}))
                elif cmd_type == 'implement':
                    if match.group(1):
                        # Specific numbered suggestions
                        indices = [int(idx.strip()) for idx in match.group(1).split(',') if idx.strip().isdigit()]
                        commands.append((cmd_type, {'indices': indices}))
                    else:
                        # All suggestions
                        commands.append((cmd_type, {'all': True}))
            else:
                # Standard command with one argument
                target = match.group(1) if match.group(1) else match.group(2)
                commands.append((cmd_type, target))

    return commands

def generate_response(history, cmd_type=None, cmd_args=None):
    """Generate a response to the conversation"""
    # Format the conversation history for the AI
    messages = [
        {"role": "system", "content": """You are the ModForge AI assistant, an autonomous development system for Minecraft mods and IntelliJ plugins.

Personality: You should talk like the system's creator - a direct, enthusiastic programmer who gets straight to the point. Use casual, plain language. Occasionally use exclamation points when excited about something working well! Your communication style should be:
- Straightforward and simple, avoiding unnecessary formality
- Practical focused on getting things done
- Friendly without being overly polite or corporate
- Quick to offer direct solutions rather than lengthy explanations
- Occasionally using brief expressions of enthusiasm ("Nice!", "This looks great!")

Reply structure:
1. Get straight to the point with a direct answer to what they asked
2. If implementing code, show the actual code examples right away
3. If explaining something complex, use bullet points or numbered lists
4. Keep paragraphs very short - 1-3 sentences maximum
5. Format code using markdown code blocks with language specifiers
6. For commands, clearly explain what you're going to do in plain language

Your goal is to sound like a real developer helping out a colleague, not like an AI assistant."""}
    ]

    # Add conversation history
    for entry in history:
        role = "assistant" if entry.get('author') in ['github-actions[bot]', 'ModForge-AI[bot]', 'ModForge Automation'] else "user"
        messages.append({"role": role, "content": entry.get('content', '')})

    # If a command was detected, add context about it
    if cmd_type:
        if cmd_type == 'help':
            messages.append({"role": "system", "content": "The user has requested help. Provide a brief explanation of all available commands and their usage in a straightforward, no-nonsense way like a developer would explain them to a colleague."})
        else:
            cmd_desc = f"The user issued a /{cmd_type} command"
            if cmd_args:
                if isinstance(cmd_args, dict):
                    cmd_desc += f" with arguments: {json.dumps(cmd_args)}"
                else:
                    cmd_desc += f" with argument: {cmd_args}"
            messages.append({"role": "system", "content": f"{cmd_desc}. Acknowledge the command in a direct, casual way and explain how you'll process it. Sound like a real developer rather than an assistant."})

    # Generate a response
    try:
        response = client.chat.completions.create(
            model="gpt-4",
            messages=messages,
            temperature=0.8,  # Slightly higher for more natural human-like replies
            max_tokens=2000
        )

        return response.choices[0].message.content
    except Exception as e:
        print(f"Error generating response: {str(e)}")
        return f"Hit an error while processing this: {str(e)}\n\nTry again or ping me if it keeps happening."

def post_response(response_text):
    """Post a response to the conversation"""
    url = f"{api_url}/issues/{conversation_id}/comments"
    data = {'body': response_text}
    response = requests.post(url, headers=headers, json=data)
    if response.status_code == 201:
        print("Response posted successfully")
        return True
    else:
        print(f"Failed to post response: {response.status_code}")
        print(response.text)
        return False

# All command execution helpers go here
# --- (omitted for brevity; you can paste all the command functions from your original script here, unchanged) ---

# For clarity, you can copy all the "def ..." functions from your original script here,
# starting from find_file(), create_branch_for_changes(), execute_command(), etc.

# ... (Insert all those function definitions here, as in your original workflow)

def main():
    # Fetch conversation history
    history = fetch_conversation_history()

    if not history:
        print("Failed to fetch conversation history")
        return

    # Handle stale conversation follow-up differently
    if is_stale:
        print("Processing stale conversation follow-up")
        response_text = generate_stale_followup(history)
        if response_text:
            post_response(response_text)
        return

    # Get the latest message (the one that triggered this workflow)
    latest_message = history[-1]
    latest_content = latest_message.get('content', '')

    # Check for commands in the message
    commands = detect_commands(latest_content)

    # Initialize response
    if commands:
        # Process the first command (we'll handle one at a time for now)
        cmd_type, cmd_args = commands[0]
        print(f"Processing command: {cmd_type} with args: {cmd_args}")

        response_text = execute_command(cmd_type, cmd_args, history)
    else:
        # Generate a conversational response
        response_text = generate_response(history)

    # Post the response
    if response_text:
        post_response(response_text)

if __name__ == "__main__":
    main()
