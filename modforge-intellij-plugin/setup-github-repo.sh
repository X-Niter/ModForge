#!/bin/bash
# Setup script for ModForge GitHub repository

echo "ModForge GitHub Repository Setup"
echo "================================"
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "Error: git is not installed. Please install git first."
    exit 1
fi

# Get repository information
read -p "Enter your GitHub username: " GITHUB_USERNAME
read -p "Enter the repository name [modforge-intellij-plugin]: " REPO_NAME
REPO_NAME=${REPO_NAME:-modforge-intellij-plugin}

# Initialize git repository
echo ""
echo "Initializing git repository..."
git init
git add .
git commit -m "Initial commit"

# Add remote
echo ""
echo "Adding GitHub remote..."
git remote add origin "https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"

# Reminder about API key
echo ""
echo "IMPORTANT: Before pushing to GitHub, remember to:"
echo "1. Create the repository on GitHub: https://github.com/new"
echo "2. Set up the repository secret for OPENAI_API_KEY"
echo "   - Go to Settings > Secrets and variables > Actions"
echo "   - Add a new repository secret named 'OPENAI_API_KEY' with your OpenAI API key"
echo ""
echo "After setting up the repository, run:"
echo "  git push -u origin master"
echo ""
echo "Setup completed!"