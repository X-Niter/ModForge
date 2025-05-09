#!/usr/bin/env python3
"""
Generate project metrics and statistics for the GitHub Pages site.

This script analyzes the ModForge codebase, calculates various metrics,
and generates content for the GitHub Pages site.
"""

import os
import re
import json
import subprocess
from pathlib import Path
from datetime import datetime
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ModForge-Metrics')

# Root directory of the project
ROOT_DIR = Path(__file__).parent.parent.parent
SRC_DIR = ROOT_DIR / 'src'
JAVA_SRC_DIR = SRC_DIR / 'main' / 'java' / 'com' / 'modforge' / 'intellij' / 'plugin'
DOCS_DIR = ROOT_DIR / 'docs'

# Ensure docs directory exists
DOCS_DIR.mkdir(exist_ok=True)
DOCS_SRC_DIR = DOCS_DIR / 'src'
DOCS_SRC_DIR.mkdir(exist_ok=True)
DOCS_DATA_DIR = DOCS_SRC_DIR / 'data'
DOCS_DATA_DIR.mkdir(exist_ok=True)

def get_git_history():
    """Get the git commit history"""
    try:
        result = subprocess.run(
            ["git", "log", "--format=%h|%an|%ad|%s", "--date=short", "-n", "50"],
            capture_output=True,
            text=True,
            check=True
        )
        
        commits = []
        for line in result.stdout.strip().split("\n"):
            if not line:
                continue
            parts = line.split("|")
            if len(parts) < 4:
                continue
            
            commits.append({
                "hash": parts[0],
                "author": parts[1],
                "date": parts[2],
                "message": parts[3]
            })
        
        return commits
    except subprocess.CalledProcessError as e:
        logger.error(f"Failed to get git history: {e}")
        return []

def count_code_lines():
    """Count lines of code by type"""
    line_counts = {
        "java": 0,
        "xml": 0,
        "gradle": 0,
        "python": 0,
        "markdown": 0,
        "other": 0,
        "total": 0
    }
    
    file_counts = {
        "java": 0,
        "xml": 0,
        "gradle": 0, 
        "python": 0,
        "markdown": 0,
        "other": 0,
        "total": 0
    }
    
    for root, _, files in os.walk(ROOT_DIR):
        # Skip build, .git, and other non-source directories
        if any(skip in root for skip in ["/build/", "/.git/", "/.github/", "/dist/", "/out/"]):
            continue
            
        for file in files:
            file_path = os.path.join(root, file)
            _, ext = os.path.splitext(file)
            ext = ext.lstrip(".").lower()
            
            # Map the extension to a category
            if ext in ["java"]:
                category = "java"
            elif ext in ["xml"]:
                category = "xml"
            elif ext in ["gradle", "groovy"]:
                category = "gradle"
            elif ext in ["py"]:
                category = "python"
            elif ext in ["md", "markdown"]:
                category = "markdown"
            else:
                category = "other"
                
            # Count the file
            file_counts[category] += 1
            file_counts["total"] += 1
            
            # Count the lines
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    line_count = sum(1 for _ in f)
                    line_counts[category] += line_count
                    line_counts["total"] += line_count
            except Exception as e:
                logger.warning(f"Error counting lines in {file_path}: {e}")
    
    return {"line_counts": line_counts, "file_counts": file_counts}

def analyze_java_classes():
    """Analyze Java classes in the project"""
    class_metrics = []
    
    # Find all Java files
    java_files = list(JAVA_SRC_DIR.glob("**/*.java"))
    
    for java_file in java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            package = package_match.group(1) if package_match else "unknown"
            
            # Extract class name and type
            class_match = re.search(r'(public|private)\s+(final\s+)?(class|interface|enum)\s+(\w+)', content)
            if not class_match:
                continue
                
            class_name = class_match.group(4)
            class_type = class_match.group(3)
            
            # Count methods
            method_count = len(re.findall(r'(public|private|protected)\s+\w+\s+\w+\s*\([^)]*\)\s*(\{|throws)', content))
            
            # Check if it has JavaDoc
            has_javadoc = bool(re.search(r'/\*\*\s.*?\*/\s*(public|private)\s+(class|interface|enum)', content, re.DOTALL))
            
            # Check complexity (simple heuristic based on conditionals)
            complexity = len(re.findall(r'if|else|for|while|switch|catch', content))
            
            # Count lines
            line_count = content.count('\n') + 1
            
            # Add to metrics
            class_metrics.append({
                "name": class_name,
                "package": package,
                "type": class_type,
                "methods": method_count,
                "has_javadoc": has_javadoc,
                "complexity": complexity,
                "lines": line_count,
                "path": str(java_file.relative_to(ROOT_DIR))
            })
            
        except Exception as e:
            logger.error(f"Error analyzing {java_file}: {e}")
    
    return class_metrics

def get_automated_changes():
    """Get statistics about automated changes"""
    try:
        # Count commits by automation
        result = subprocess.run(
            ["git", "log", "--format=%an|%s", "--shortstat"],
            capture_output=True,
            text=True,
            check=True
        )
        
        auto_commits = 0
        manual_commits = 0
        auto_changes = {"files": 0, "insertions": 0, "deletions": 0}
        
        current_author = None
        
        for line in result.stdout.strip().split("\n"):
            if "|" in line:  # This is an author/message line
                parts = line.split("|")
                current_author = parts[0]
                if "ModForge Automation" in current_author or "Automated" in parts[1]:
                    auto_commits += 1
                else:
                    manual_commits += 1
            elif "file" in line and "insertion" in line:  # This is a stats line
                # Parse stats like " 1 file changed, 2 insertions(+), 3 deletions(-)"
                if current_author and ("ModForge Automation" in current_author):
                    # Extract numbers using regex
                    file_match = re.search(r'(\d+) file', line)
                    insertion_match = re.search(r'(\d+) insertion', line)
                    deletion_match = re.search(r'(\d+) deletion', line)
                    
                    if file_match:
                        auto_changes["files"] += int(file_match.group(1))
                    if insertion_match:
                        auto_changes["insertions"] += int(insertion_match.group(1))
                    if deletion_match:
                        auto_changes["deletions"] += int(deletion_match.group(1))
        
        return {
            "auto_commits": auto_commits,
            "manual_commits": manual_commits,
            "auto_changes": auto_changes
        }
        
    except subprocess.CalledProcessError as e:
        logger.error(f"Failed to get automated changes: {e}")
        return {
            "auto_commits": 0,
            "manual_commits": 0,
            "auto_changes": {"files": 0, "insertions": 0, "deletions": 0}
        }

def generate_package_structure():
    """Generate a hierarchical package structure for visualization"""
    package_structure = {}
    
    # Find all Java files
    java_files = list(JAVA_SRC_DIR.glob("**/*.java"))
    
    for java_file in java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            if not package_match:
                continue
                
            package = package_match.group(1)
            
            # Extract class name
            class_match = re.search(r'(public|private)\s+(final\s+)?(class|interface|enum)\s+(\w+)', content)
            if not class_match:
                continue
                
            class_name = class_match.group(4)
            class_type = class_match.group(3)
            
            # Build package hierarchy
            parts = package.split('.')
            current = package_structure
            for part in parts:
                if part not in current:
                    current[part] = {}
                current = current[part]
                
            # Add class to the package
            if "_classes" not in current:
                current["_classes"] = []
                
            current["_classes"].append({
                "name": class_name,
                "type": class_type,
                "path": str(java_file.relative_to(ROOT_DIR))
            })
            
        except Exception as e:
            logger.error(f"Error processing {java_file}: {e}")
    
    return package_structure

def generate_metrics():
    """Generate metrics and JSON data files for the dashboard"""
    logger.info("Generating project metrics")
    
    # Get basic metrics
    code_counts = count_code_lines()
    class_metrics = analyze_java_classes()
    git_history = get_git_history()
    auto_changes = get_automated_changes()
    package_structure = generate_package_structure()
    
    # Calculate aggregate metrics
    total_classes = len(class_metrics)
    documented_classes = sum(1 for c in class_metrics if c["has_javadoc"])
    avg_methods = sum(c["methods"] for c in class_metrics) / max(1, total_classes)
    avg_complexity = sum(c["complexity"] for c in class_metrics) / max(1, total_classes)
    
    # Get current date
    current_date = datetime.now().strftime("%Y-%m-%d")
    
    # Create summary metrics
    summary = {
        "generated_at": current_date,
        "code_stats": {
            "total_lines": code_counts["line_counts"]["total"],
            "java_lines": code_counts["line_counts"]["java"],
            "total_files": code_counts["file_counts"]["total"],
            "java_files": code_counts["file_counts"]["java"]
        },
        "class_stats": {
            "total_classes": total_classes,
            "documented_classes": documented_classes,
            "documentation_coverage": round((documented_classes / max(1, total_classes)) * 100, 2),
            "avg_methods_per_class": round(avg_methods, 2),
            "avg_complexity": round(avg_complexity, 2)
        },
        "git_stats": {
            "total_commits": len(git_history),
            "auto_commits": auto_changes["auto_commits"],
            "manual_commits": auto_changes["manual_commits"],
            "auto_changes": auto_changes["auto_changes"]
        }
    }
    
    # Write metrics to JSON files
    try:
        with open(DOCS_DATA_DIR / 'summary.json', 'w') as f:
            json.dump(summary, f, indent=2)
            
        with open(DOCS_DATA_DIR / 'class_metrics.json', 'w') as f:
            json.dump(class_metrics, f, indent=2)
            
        with open(DOCS_DATA_DIR / 'git_history.json', 'w') as f:
            json.dump(git_history, f, indent=2)
            
        with open(DOCS_DATA_DIR / 'package_structure.json', 'w') as f:
            json.dump(package_structure, f, indent=2)
            
        logger.info("Metrics generation completed")
        
    except Exception as e:
        logger.error(f"Error writing metrics files: {e}")
        
    # Create React app for GitHub Pages if it doesn't exist
    if not (DOCS_DIR / 'package.json').exists():
        try:
            logger.info("Setting up React app for GitHub Pages")
            setup_react_app()
        except Exception as e:
            logger.error(f"Error setting up React app: {e}")

def setup_react_app():
    """Set up a basic React app for GitHub Pages"""
    # Create package.json
    package_json = {
        "name": "modforge-dashboard",
        "version": "1.0.0",
        "private": True,
        "dependencies": {
            "react": "^18.2.0",
            "react-dom": "^18.2.0",
            "react-scripts": "5.0.1",
            "react-router-dom": "^6.10.0",
            "recharts": "^2.5.0",
            "d3": "^7.8.4",
            "@mui/material": "^5.12.0",
            "@mui/icons-material": "^5.11.16",
            "@emotion/react": "^11.10.6",
            "@emotion/styled": "^11.10.6"
        },
        "scripts": {
            "start": "react-scripts start",
            "build": "react-scripts build",
            "test": "react-scripts test",
            "eject": "react-scripts eject"
        },
        "eslintConfig": {
            "extends": [
                "react-app",
                "react-app/jest"
            ]
        },
        "browserslist": {
            "production": [
                ">0.2%",
                "not dead",
                "not op_mini all"
            ],
            "development": [
                "last 1 chrome version",
                "last 1 firefox version",
                "last 1 safari version"
            ]
        },
        "homepage": "."
    }
    
    with open(DOCS_DIR / 'package.json', 'w') as f:
        json.dump(package_json, f, indent=2)
    
    # Create public directory
    public_dir = DOCS_DIR / 'public'
    public_dir.mkdir(exist_ok=True)
    
    # Create index.html
    with open(public_dir / 'index.html', 'w') as f:
        f.write('''<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <link rel="icon" href="%PUBLIC_URL%/favicon.ico" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="theme-color" content="#000000" />
    <meta
      name="description"
      content="ModForge Project Dashboard"
    />
    <link rel="apple-touch-icon" href="%PUBLIC_URL%/logo192.png" />
    <link rel="manifest" href="%PUBLIC_URL%/manifest.json" />
    <title>ModForge Dashboard</title>
  </head>
  <body>
    <noscript>You need to enable JavaScript to run this app.</noscript>
    <div id="root"></div>
  </body>
</html>
''')
    
    # Create manifest.json
    with open(public_dir / 'manifest.json', 'w') as f:
        f.write('''
{
  "short_name": "ModForge",
  "name": "ModForge Dashboard",
  "icons": [
    {
      "src": "favicon.ico",
      "sizes": "64x64 32x32 24x24 16x16",
      "type": "image/x-icon"
    }
  ],
  "start_url": ".",
  "display": "standalone",
  "theme_color": "#000000",
  "background_color": "#ffffff"
}
''')
    
    # Create src directory if not exists
    src_dir = DOCS_DIR / 'src'
    src_dir.mkdir(exist_ok=True)
    
    # Create index.js
    with open(src_dir / 'index.js', 'w') as f:
        f.write('''
import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
''')
    
    # Create index.css
    with open(src_dir / 'index.css', 'w') as f:
        f.write('''
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

code {
  font-family: source-code-pro, Menlo, Monaco, Consolas, 'Courier New',
    monospace;
}
''')
    
    # Create App.js
    with open(src_dir / 'App.js', 'w') as f:
        f.write('''
import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import Dashboard from './components/Dashboard';
import CodeMetrics from './components/CodeMetrics';
import GitHistory from './components/GitHistory';
import AutomationStats from './components/AutomationStats';
import CommandCenter from './components/CommandCenter';
import NotFound from './components/NotFound';
import './App.css';

// Create a dark theme
const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#3f51b5',
    },
    secondary: {
      main: '#f50057',
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={darkTheme}>
      <CssBaseline />
      <Router>
        <AppBar position="static">
          <Toolbar>
            <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
              ModForge Dashboard
            </Typography>
            <Button color="inherit" component={Link} to="/">Dashboard</Button>
            <Button color="inherit" component={Link} to="/code">Code Metrics</Button>
            <Button color="inherit" component={Link} to="/git">Git History</Button>
            <Button color="inherit" component={Link} to="/automation">Automation</Button>
            <Button color="inherit" component={Link} to="/command">Command Center</Button>
          </Toolbar>
        </AppBar>
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/code" element={<CodeMetrics />} />
            <Route path="/git" element={<GitHistory />} />
            <Route path="/automation" element={<AutomationStats />} />
            <Route path="/command" element={<CommandCenter />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Container>
      </Router>
    </ThemeProvider>
  );
}

export default App;
''')
    
    # Create App.css
    with open(src_dir / 'App.css', 'w') as f:
        f.write('''
.dashboard-card {
  padding: 16px;
  margin-bottom: 16px;
  height: 100%;
}

.dashboard-title {
  margin-bottom: 16px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
}

.metric-label {
  color: #666;
  margin-top: 4px;
}

.chart-container {
  height: 300px;
  width: 100%;
}
''')
    
    # Create components directory
    components_dir = src_dir / 'components'
    components_dir.mkdir(exist_ok=True)
    
    # Create Dashboard.js (main component)
    with open(components_dir / 'Dashboard.js', 'w') as f:
        f.write('''
import React, { useState, useEffect } from 'react';
import { Grid, Paper, Typography, Box, CircularProgress } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Load summary data
    fetch('./data/summary.json')
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        setSummary(data);
        setLoading(false);
      })
      .catch(err => {
        console.error("Error loading summary data:", err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', m: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ m: 3 }}>
        <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column' }}>
          <Typography variant="h6" color="error">
            Error loading data: {error}
          </Typography>
        </Paper>
      </Box>
    );
  }

  if (!summary) {
    return (
      <Box sx={{ m: 3 }}>
        <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column' }}>
          <Typography variant="h6">
            No data available. Please generate metrics first.
          </Typography>
        </Paper>
      </Box>
    );
  }

  // Prepare data for charts
  const codeDistribution = [
    { name: 'Java', value: summary.code_stats.java_lines },
    { name: 'Other', value: summary.code_stats.total_lines - summary.code_stats.java_lines }
  ];

  const commitDistribution = [
    { name: 'Automated', value: summary.git_stats.auto_commits },
    { name: 'Manual', value: summary.git_stats.manual_commits }
  ];

  const documentationStatus = [
    { name: 'Documented', value: summary.class_stats.documented_classes },
    { name: 'Undocumented', value: summary.class_stats.total_classes - summary.class_stats.documented_classes }
  ];

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Project Dashboard
      </Typography>
      <Typography variant="subtitle1" gutterBottom>
        Generated on: {summary.generated_at}
      </Typography>

      <Grid container spacing={3} sx={{ mt: 1 }}>
        {/* Code Stats */}
        <Grid item xs={12} md={6} lg={3}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 140 }}>
            <Typography variant="h6" gutterBottom>
              Code Size
            </Typography>
            <Typography variant="h3" component="div">
              {summary.code_stats.total_lines.toLocaleString()}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Total lines of code
            </Typography>
          </Paper>
        </Grid>

        {/* File Stats */}
        <Grid item xs={12} md={6} lg={3}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 140 }}>
            <Typography variant="h6" gutterBottom>
              Files
            </Typography>
            <Typography variant="h3" component="div">
              {summary.code_stats.total_files.toLocaleString()}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Total source files
            </Typography>
          </Paper>
        </Grid>

        {/* Class Stats */}
        <Grid item xs={12} md={6} lg={3}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 140 }}>
            <Typography variant="h6" gutterBottom>
              Java Classes
            </Typography>
            <Typography variant="h3" component="div">
              {summary.class_stats.total_classes.toLocaleString()}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Total Java classes
            </Typography>
          </Paper>
        </Grid>

        {/* Commit Stats */}
        <Grid item xs={12} md={6} lg={3}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 140 }}>
            <Typography variant="h6" gutterBottom>
              Git Commits
            </Typography>
            <Typography variant="h3" component="div">
              {summary.git_stats.total_commits.toLocaleString()}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Total git commits
            </Typography>
          </Paper>
        </Grid>

        {/* Code Distribution */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 300 }}>
            <Typography variant="h6" gutterBottom>
              Code Distribution
            </Typography>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={codeDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {codeDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => value.toLocaleString()} />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Commit Distribution */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 300 }}>
            <Typography variant="h6" gutterBottom>
              Commit Distribution
            </Typography>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={commitDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {commitDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => value.toLocaleString()} />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Documentation Status */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: 300 }}>
            <Typography variant="h6" gutterBottom>
              Documentation Status
            </Typography>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={documentationStatus}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {documentationStatus.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => value.toLocaleString()} />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Autonomous Development Stats */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column' }}>
            <Typography variant="h6" gutterBottom>
              Autonomous Development Statistics
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <Box textAlign="center" p={2}>
                  <Typography variant="body1" color="text.secondary">
                    Automated Commits
                  </Typography>
                  <Typography variant="h4" component="div">
                    {summary.git_stats.auto_commits}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} md={4}>
                <Box textAlign="center" p={2}>
                  <Typography variant="body1" color="text.secondary">
                    Files Changed by Automation
                  </Typography>
                  <Typography variant="h4" component="div">
                    {summary.git_stats.auto_changes.files}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} md={4}>
                <Box textAlign="center" p={2}>
                  <Typography variant="body1" color="text.secondary">
                    Lines Added by Automation
                  </Typography>
                  <Typography variant="h4" component="div">
                    {summary.git_stats.auto_changes.insertions}
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}

export default Dashboard;
''')
    
    # Create other components (simplified placeholders)
    component_files = {
        'CodeMetrics.js': '''
import React, { useState, useEffect } from 'react';
import { Typography, Paper, Box, CircularProgress } from '@mui/material';

function CodeMetrics() {
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // Simulate loading data
    const timer = setTimeout(() => {
      setLoading(false);
    }, 1000);
    return () => clearTimeout(timer);
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', m: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Code Metrics
      </Typography>
      <Paper sx={{ p: 2, m: 2 }}>
        <Typography variant="h6">
          Coming Soon: Detailed code metrics and analysis
        </Typography>
        <Typography variant="body1" paragraph>
          This page will show detailed metrics about the codebase, including complexity analysis,
          module dependencies, and quality metrics.
        </Typography>
      </Paper>
    </div>
  );
}

export default CodeMetrics;
''',
        'GitHistory.js': '''
import React, { useState, useEffect } from 'react';
import { Typography, Paper, Box, CircularProgress, List, ListItem, ListItemText, Divider } from '@mui/material';

function GitHistory() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch('./data/git_history.json')
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        setHistory(data);
        setLoading(false);
      })
      .catch(err => {
        console.error("Error loading git history:", err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', m: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ m: 3 }}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" color="error">
            Error loading data: {error}
          </Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Git Commit History
      </Typography>
      <Paper sx={{ p: 2, m: 2 }}>
        <List>
          {history.map((commit, index) => (
            <React.Fragment key={commit.hash}>
              <ListItem alignItems="flex-start">
                <ListItemText
                  primary={commit.message}
                  secondary={
                    <>
                      <Typography
                        component="span"
                        variant="body2"
                        color="text.primary"
                      >
                        {commit.author}
                      </Typography>
                      {" — "}{commit.date} (#{commit.hash})
                    </>
                  }
                />
              </ListItem>
              {index < history.length - 1 && <Divider component="li" />}
            </React.Fragment>
          ))}
        </List>
      </Paper>
    </div>
  );
}

export default GitHistory;
''',
        'AutomationStats.js': '''
import React, { useState, useEffect } from 'react';
import { Typography, Paper, Box, CircularProgress } from '@mui/material';

function AutomationStats() {
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // Simulate loading data
    const timer = setTimeout(() => {
      setLoading(false);
    }, 1000);
    return () => clearTimeout(timer);
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', m: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Automation Statistics
      </Typography>
      <Paper sx={{ p: 2, m: 2 }}>
        <Typography variant="h6">
          Coming Soon: Detailed automation metrics
        </Typography>
        <Typography variant="body1" paragraph>
          This page will show statistics about the autonomous development system, including:
        </Typography>
        <ul>
          <li>Number of errors automatically fixed</li>
          <li>Code improvements made</li>
          <li>Documentation added</li>
          <li>Features implemented</li>
        </ul>
      </Paper>
    </div>
  );
}

export default AutomationStats;
''',
        'CommandCenter.js': '''
import React, { useState } from 'react';
import { Typography, Paper, Box, TextField, Button, Grid, MenuItem, Select, FormControl, InputLabel, Alert } from '@mui/material';

function CommandCenter() {
  const [command, setCommand] = useState('');
  const [commandType, setCommandType] = useState('fix');
  const [target, setTarget] = useState('');
  const [submitted, setSubmitted] = useState(false);
  
  const handleSubmit = (e) => {
    e.preventDefault();
    // In a real implementation, this would submit to the GitHub API
    // to create an issue with the command
    setSubmitted(true);
    
    // Reset form after 3 seconds
    setTimeout(() => {
      setCommand('');
      setTarget('');
      setSubmitted(false);
    }, 3000);
  };

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Command Center
      </Typography>
      <Paper sx={{ p: 2, m: 2 }}>
        <Typography variant="h6" gutterBottom>
          Send commands to the autonomous development system
        </Typography>
        
        <Typography variant="body1" paragraph>
          Use this form to create GitHub issues with special commands that will be processed by the system.
        </Typography>
        
        {submitted && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Command submitted! In a real implementation, this would create a GitHub issue.
          </Alert>
        )}
        
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth>
                <InputLabel id="command-type-label">Command Type</InputLabel>
                <Select
                  labelId="command-type-label"
                  value={commandType}
                  label="Command Type"
                  onChange={(e) => setCommandType(e.target.value)}
                >
                  <MenuItem value="fix">Fix</MenuItem>
                  <MenuItem value="add">Add</MenuItem>
                  <MenuItem value="improve">Improve</MenuItem>
                  <MenuItem value="document">Document</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Target File/Component"
                variant="outlined"
                value={target}
                onChange={(e) => setTarget(e.target.value)}
                placeholder="E.g., GenerateCodeAction.java"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Command Details"
                variant="outlined"
                value={command}
                onChange={(e) => setCommand(e.target.value)}
                placeholder="E.g., Add support for Python code generation"
              />
            </Grid>
            
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                <Typography variant="body2" sx={{ flexGrow: 1, fontStyle: 'italic' }}>
                  This will create a GitHub issue with: /command {commandType} {target} {command}
                </Typography>
                <Button type="submit" variant="contained" color="primary">
                  Send Command
                </Button>
              </Box>
            </Grid>
          </Grid>
        </Box>
      </Paper>
      
      <Paper sx={{ p: 2, m: 2 }}>
        <Typography variant="h6" gutterBottom>
          Command Reference
        </Typography>
        
        <Typography variant="body1" paragraph>
          Here are the available commands you can use:
        </Typography>
        
        <Box component="ul" sx={{ pl: 4 }}>
          <li><Typography><code>/command fix [target]</code> - Fix errors or issues in a specific file or component</Typography></li>
          <li><Typography><code>/command add [feature] to [target]</code> - Add a new feature to a component</Typography></li>
          <li><Typography><code>/command improve [target]</code> - Improve code quality in a file</Typography></li>
          <li><Typography><code>/command document [target]</code> - Generate documentation for a file</Typography></li>
        </Box>
      </Paper>
    </div>
  );
}

export default CommandCenter;
''',
        'NotFound.js': '''
import React from 'react';
import { Typography, Paper, Box } from '@mui/material';
import { Link } from 'react-router-dom';

function NotFound() {
  return (
    <Box sx={{ m: 3 }}>
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          404 - Page Not Found
        </Typography>
        <Typography variant="body1" paragraph>
          The page you are looking for does not exist.
        </Typography>
        <Typography variant="body1">
          <Link to="/">Return to Dashboard</Link>
        </Typography>
      </Paper>
    </Box>
  );
}

export default NotFound;
'''
    }
    
    for filename, content in component_files.items():
        with open(components_dir / filename, 'w') as f:
            f.write(content)
    
    logger.info("React app for GitHub Pages set up successfully")

if __name__ == "__main__":
    generate_metrics()