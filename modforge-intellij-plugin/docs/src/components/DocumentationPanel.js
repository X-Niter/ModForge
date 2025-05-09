import React from 'react';
import { 
  Typography, 
  Paper, 
  Box, 
  Grid, 
  Card, 
  CardContent,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import BuildIcon from '@mui/icons-material/Build';
import ArchitectureIcon from '@mui/icons-material/Architecture';
import IntegrationInstructionsIcon from '@mui/icons-material/IntegrationInstructions';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import DeviceHubIcon from '@mui/icons-material/DeviceHub';

function DocumentationPanel() {
  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        System Documentation
      </Typography>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              Autonomous Development Platform Overview
            </Typography>
            
            <Typography variant="body1" paragraph>
              ModForge is a fully autonomous software development platform designed to streamline 
              Minecraft mod creation. The system continuously develops, tests, and improves mod code with 
              minimal human intervention.
            </Typography>
            
            <Box sx={{ my: 3 }}>
              <Typography variant="h6" gutterBottom>
                Key Components
              </Typography>
              
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="h6" color="primary" gutterBottom>
                        <AutoFixHighIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                        AI-Driven Development
                      </Typography>
                      <Typography variant="body2">
                        The heart of the system, using advanced AI models to generate, fix, 
                        and improve code based on patterns and user specifications.
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="h6" color="primary" gutterBottom>
                        <BuildIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                        Continuous Development
                      </Typography>
                      <Typography variant="body2">
                        Automatic cycles of compilation, error detection, and correction 
                        that run 24/7 to ensure code quality and functionality.
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="h6" color="primary" gutterBottom>
                        <DeviceHubIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                        Pattern Recognition
                      </Typography>
                      <Typography variant="body2">
                        Machine learning algorithms that identify and learn from successful code patterns,
                        reducing API costs and improving efficiency over time.
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                
                <Grid item xs={12} md={6}>
                  <Card variant="outlined" sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="h6" color="primary" gutterBottom>
                        <IntegrationInstructionsIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                        GitHub Integration
                      </Typography>
                      <Typography variant="body2">
                        Comprehensive integration with GitHub workflows, issues, pull requests, and
                        GitHub Pages for monitoring and control of autonomous operations.
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </Box>
            
            <Divider sx={{ my: 3 }} />
            
            <Typography variant="h6" gutterBottom>
              System Architecture
            </Typography>
            
            <Box sx={{ mb: 3 }}>
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography>
                    <ArchitectureIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    High-Level Architecture
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2" paragraph>
                    The ModForge system uses a multi-layered architecture:
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Core AI Services" 
                        secondary="OpenAI integration, pattern learning, and model selection" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Development Services" 
                        secondary="Code generation, error fixing, documentation, and feature addition" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="GitHub Integration Layer" 
                        secondary="Workflows, issue processing, and metrics collection" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="IntelliJ Platform" 
                        secondary="IDE integration, live tooling, and background services" 
                      />
                    </ListItem>
                  </List>
                </AccordionDetails>
              </Accordion>
              
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography>
                    <DeviceHubIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Workflow Systems
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2" paragraph>
                    ModForge uses various GitHub Actions workflows for different tasks:
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Continuous Development" 
                        secondary="Runs every 6 hours to check for errors and make improvements" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Issue Command Processor" 
                        secondary="Monitors new issues for command syntax and processes them" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Comment Command Processor" 
                        secondary="Processes commands in comments on issues and PRs" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Pull Request Processor" 
                        secondary="Analyzes and improves submitted PRs automatically" 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary="GitHub Pages" 
                        secondary="Updates metrics and dashboard visualizations" 
                      />
                    </ListItem>
                  </List>
                </AccordionDetails>
              </Accordion>
            </Box>
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h5" gutterBottom>
              Quick Start Guide
            </Typography>
            
            <Typography variant="body2" paragraph>
              Get started with the autonomous development system in a few simple steps:
            </Typography>
            
            <List>
              <ListItem>
                <ListItemIcon>
                  <CheckCircleIcon color="primary" />
                </ListItemIcon>
                <ListItemText 
                  primary="1. Create a GitHub Repository" 
                  secondary="Fork or clone the ModForge repository" 
                />
              </ListItem>
              <Divider variant="inset" component="li" />
              
              <ListItem>
                <ListItemIcon>
                  <CheckCircleIcon color="primary" />
                </ListItemIcon>
                <ListItemText 
                  primary="2. Set Up API Key" 
                  secondary="Add your OpenAI API key to repository secrets" 
                />
              </ListItem>
              <Divider variant="inset" component="li" />
              
              <ListItem>
                <ListItemIcon>
                  <CheckCircleIcon color="primary" />
                </ListItemIcon>
                <ListItemText 
                  primary="3. Enable GitHub Actions" 
                  secondary="Ensure workflows are enabled in your repository" 
                />
              </ListItem>
              <Divider variant="inset" component="li" />
              
              <ListItem>
                <ListItemIcon>
                  <CheckCircleIcon color="primary" />
                </ListItemIcon>
                <ListItemText 
                  primary="4. Install the Plugin" 
                  secondary="Install the ModForge plugin in IntelliJ IDEA" 
                />
              </ListItem>
              <Divider variant="inset" component="li" />
              
              <ListItem>
                <ListItemIcon>
                  <CheckCircleIcon color="primary" />
                </ListItemIcon>
                <ListItemText 
                  primary="5. Submit Your First Command" 
                  secondary="Create an issue with a /command to test the system" 
                />
              </ListItem>
            </List>
          </Paper>
          
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              FAQ
            </Typography>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="subtitle1">
                  How much does it cost to run?
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2">
                  The system uses OpenAI API calls, but includes pattern recognition to reduce costs over time.
                  Costs depend on usage but typically range from $5-20 per month for moderate development activity.
                </Typography>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="subtitle1">
                  Will it break my code?
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2">
                  The system is designed to be safe with automatic checks and balances.
                  All changes are made through Git, so you can always revert any changes that don't work as expected.
                  The system also runs tests before committing changes.
                </Typography>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="subtitle1">
                  What mod loaders are supported?
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2">
                  The system supports Forge, Fabric, and Quilt, with cross-loader development through the
                  Architectury framework. You can specify your preferred loader in the settings.
                </Typography>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="subtitle1">
                  How do I control what it does?
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2">
                  Use GitHub issues with special command syntax (/fix, /add, etc.) to direct the system.
                  You can also control settings through the IntelliJ plugin interface or by editing the
                  configuration files directly.
                </Typography>
              </AccordionDetails>
            </Accordion>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}

export default DocumentationPanel;