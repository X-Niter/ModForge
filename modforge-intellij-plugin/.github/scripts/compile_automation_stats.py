#!/usr/bin/env python3
"""
Compile statistics about the autonomous development system.

This script analyzes workflow runs, commits, and other metrics to generate
statistics about the ModForge autonomous development system.
"""

import os
import json
import re
import logging
from datetime import datetime, timedelta
import subprocess
from pathlib import Path
import glob

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ModForge-AutomationStats')

# Paths
DOCS_DIR = Path('docs')
DATA_DIR = DOCS_DIR / 'src' / 'data'
WORKFLOWS_DIR = DATA_DIR / 'workflows'

def load_workflow_data():
    """Load and analyze workflow run data"""
    workflow_stats = {
        "total_runs": 0,
        "successful_runs": 0,
        "workflow_counts": {},
        "recent_activity": []
    }
    
    # Load workflow list
    try:
        with open(WORKFLOWS_DIR / 'workflow_list.json', 'r') as f:
            workflows = json.load(f)
            if isinstance(workflows, list):
                # If it's a list of workflows in an array
                workflow_map = {str(wf['id']): wf for wf in workflows}
            else:
                # If it's a single workflow object
                workflow_map = {str(workflows['id']): workflows}
    except Exception as e:
        logger.error(f"Error loading workflow list: {e}")
        workflow_map = {}
    
    # Process each workflow's runs
    for workflow_file in glob.glob(str(WORKFLOWS_DIR / 'workflow_*_runs.json')):
        try:
            with open(workflow_file, 'r') as f:
                data = json.load(f)
                
                if 'workflow_runs' not in data:
                    logger.warning(f"No workflow_runs in {workflow_file}")
                    continue
                    
                runs = data['workflow_runs']
                
                for run in runs:
                    workflow_id = str(run.get('workflow_id', ''))
                    workflow_name = workflow_map.get(workflow_id, {}).get('name', 'Unknown')
                    
                    # Count total and successful runs
                    workflow_stats['total_runs'] += 1
                    if run.get('conclusion') == 'success':
                        workflow_stats['successful_runs'] += 1
                    
                    # Count by workflow type
                    if workflow_name not in workflow_stats['workflow_counts']:
                        workflow_stats['workflow_counts'][workflow_name] = {
                            'total': 0,
                            'success': 0,
                            'last_run': None
                        }
                    
                    workflow_stats['workflow_counts'][workflow_name]['total'] += 1
                    if run.get('conclusion') == 'success':
                        workflow_stats['workflow_counts'][workflow_name]['success'] += 1
                    
                    # Track recent activity
                    created_at = run.get('created_at')
                    if created_at:
                        if (not workflow_stats['workflow_counts'][workflow_name]['last_run'] or 
                            created_at > workflow_stats['workflow_counts'][workflow_name]['last_run']):
                            workflow_stats['workflow_counts'][workflow_name]['last_run'] = created_at
                            
                        workflow_stats['recent_activity'].append({
                            'workflow': workflow_name,
                            'status': run.get('conclusion', 'unknown'),
                            'created_at': created_at,
                            'html_url': run.get('html_url', '')
                        })
        
        except Exception as e:
            logger.error(f"Error processing workflow file {workflow_file}: {e}")
    
    # Sort recent activity by date (newest first)
    workflow_stats['recent_activity'] = sorted(
        workflow_stats['recent_activity'], 
        key=lambda x: x['created_at'] if x['created_at'] else '', 
        reverse=True
    )[:20]  # Keep only the 20 most recent activities
    
    return workflow_stats

def analyze_git_history():
    """Analyze Git commit history for autonomous contributions"""
    try:
        # Get all commits in the last 30 days
        thirty_days_ago = (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d')
        
        result = subprocess.run(
            ['git', 'log', f'--since={thirty_days_ago}', '--format=%H|%an|%ae|%ad|%s', '--date=iso'],
            capture_output=True,
            text=True,
            check=True
        )
        
        commits = []
        auto_commit_count = 0
        manual_commit_count = 0
        commit_by_type = {
            'fix': 0,
            'improve': 0,
            'document': 0,
            'feature': 0,
            'other': 0
        }
        
        for line in result.stdout.strip().split('\n'):
            if not line:
                continue
                
            parts = line.split('|')
            if len(parts) < 5:
                continue
                
            commit_hash, author, email, date, message = parts
            
            # Check if it's an automated commit
            is_auto = (
                'ModForge Automation' in author or 
                'automation@' in email or 
                '[ModForge AI]' in message or
                'Automated' in message
            )
            
            commit_type = 'other'
            if re.search(r'\bfix\b|\bfixed\b|\bfixing\b', message, re.IGNORECASE):
                commit_type = 'fix'
            elif re.search(r'\bimprove\b|\bimproved\b|\bimproving\b', message, re.IGNORECASE):
                commit_type = 'improve'
            elif re.search(r'\bdocument\b|\bdocumentation\b', message, re.IGNORECASE):
                commit_type = 'document'
            elif re.search(r'\badd\b|\bfeature\b|\bimplemented\b', message, re.IGNORECASE):
                commit_type = 'feature'
                
            if is_auto:
                auto_commit_count += 1
            else:
                manual_commit_count += 1
                
            commit_by_type[commit_type] += 1
            
            commits.append({
                'hash': commit_hash,
                'author': author,
                'date': date,
                'message': message,
                'is_auto': is_auto,
                'type': commit_type
            })
        
        return {
            'total_commits': len(commits),
            'auto_commits': auto_commit_count,
            'manual_commits': manual_commit_count,
            'commit_by_type': commit_by_type,
            'recent_commits': commits[:20]  # Keep only the 20 most recent commits
        }
        
    except Exception as e:
        logger.error(f"Error analyzing git history: {e}")
        return {
            'total_commits': 0,
            'auto_commits': 0,
            'manual_commits': 0,
            'commit_by_type': {
                'fix': 0,
                'improve': 0,
                'document': 0,
                'feature': 0,
                'other': 0
            },
            'recent_commits': []
        }

def count_files_and_lines():
    """Count the number of files and lines of code in the repository"""
    try:
        result = {}
        
        # Count Java files
        java_files = subprocess.run(
            ['find', '.', '-name', '*.java', '-type', 'f', '-not', '-path', '*/\\.*'],
            capture_output=True,
            text=True,
            check=True
        )
        
        java_file_list = java_files.stdout.strip().split('\n')
        java_file_count = len([f for f in java_file_list if f])
        
        # Count lines in Java files
        java_lines = 0
        for file in java_file_list:
            if not file:
                continue
                
            with open(file, 'r', encoding='utf-8', errors='ignore') as f:
                java_lines += sum(1 for _ in f)
        
        # Count XML files
        xml_files = subprocess.run(
            ['find', '.', '-name', '*.xml', '-type', 'f', '-not', '-path', '*/\\.*'],
            capture_output=True,
            text=True,
            check=True
        )
        
        xml_file_list = xml_files.stdout.strip().split('\n')
        xml_file_count = len([f for f in xml_file_list if f])
        
        # Total file count
        total_files = subprocess.run(
            ['find', '.', '-type', 'f', '-not', '-path', '*/\\.*', '-not', '-path', '*/build/*', '-not', '-path', '*/out/*'],
            capture_output=True,
            text=True,
            check=True
        )
        
        total_file_list = total_files.stdout.strip().split('\n')
        total_file_count = len([f for f in total_file_list if f])
        
        result = {
            'total_files': total_file_count,
            'java_files': java_file_count,
            'xml_files': xml_file_count,
            'java_lines': java_lines
        }
        
        return result
        
    except Exception as e:
        logger.error(f"Error counting files and lines: {e}")
        return {
            'total_files': 0,
            'java_files': 0,
            'xml_files': 0,
            'java_lines': 0
        }

def compile_statistics():
    """Compile all statistics into a single JSON file"""
    try:
        # Make sure the data directory exists
        os.makedirs(DATA_DIR, exist_ok=True)
        
        # Load workflow statistics
        workflow_stats = load_workflow_data()
        
        # Analyze git history
        git_stats = analyze_git_history()
        
        # Count files and lines
        file_stats = count_files_and_lines()
        
        # Compile all statistics
        stats = {
            'generated_at': datetime.now().isoformat(),
            'workflow_stats': workflow_stats,
            'git_stats': git_stats,
            'file_stats': file_stats
        }
        
        # Write to JSON file
        with open(DATA_DIR / 'automation_stats.json', 'w') as f:
            json.dump(stats, f, indent=2)
            
        logger.info("Successfully compiled automation statistics")
        
    except Exception as e:
        logger.error(f"Error compiling statistics: {e}")

if __name__ == '__main__':
    compile_statistics()