import React, { useState } from 'react';
import { 
  Box, 
  Paper, 
  Typography, 
  TextField, 
  Button, 
  Grid, 
  MenuItem, 
  FormControl, 
  InputLabel, 
  Select, 
  Alert, 
  Snackbar,
  Card,
  CardContent,
  Accordion,
  AccordionSummary,
  AccordionDetails
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CodeIcon from '@mui/icons-material/Code';
import BugReportIcon from '@mui/icons-material/BugReport';
import DescriptionIcon from '@mui/icons-material/Description';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';

function CommandInterface() {
  const [commandType, setCommandType] = useState('fix');
  const [target, setTarget] = useState('');
  const [feature, setFeature] = useState('');
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [generatedIssue, setGeneratedIssue] = useState({
    title: '',
    body: ''
  });
  
  // Generate a GitHub issue from the form
  const generateIssue = () => {
    let title;
    let body;
    
    switch(commandType) {
      case 'fix':
        title = `[FIX] Fix issues in ${target}`;
        body = `/fix ${target}`;
        break;
      case 'improve':
        title = `[IMPROVE] Improve code quality in ${target}`;
        body = `/improve ${target}`;
        break;
      case 'document':
        title = `[DOCUMENT] Add documentation to ${target}`;
        body = `/document ${target}`;
        break;
      case 'add':
        title = `[ADD] Add "${feature}" to ${target}`;
        body = `/add "${feature}" to ${target}`;
        break;
      case 'explain':
        title = `[EXPLAIN] Explain how ${target} works`;
        body = `/explain ${target}`;
        break;
      default:
        title = '[HELP] Show command help';
        body = '/help';
        break;
    }
    
    setGeneratedIssue({ title, body });
    setOpenSnackbar(true);
  };
  
  // When the user clicks "Create Issue"
  const createGitHubIssue = () => {
    // This would redirect to GitHub's issue creation page in a real implementation
    // with pre-filled title and body
    alert("In a real implementation, this would open GitHub's issue creation page with the pre-filled fields. For demo purposes, we're just showing the generated content.");
  };
  
  const handleSnackbarClose = () => {
    setOpenSnackbar(false);
  };
  
  // Show different form fields based on command type
  const renderCommandFields = () => {
    switch(commandType) {
      case 'add':
        return (
          <>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Feature to Add"
                variant="outlined"
                value={feature}
                onChange={(e) => setFeature(e.target.value)}
                placeholder="E.g., Dark mode support"
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Target Component"
                variant="outlined"
                value={target}
                onChange={(e) => setTarget(e.target.value)}
                placeholder="E.g., MetricsPanel.java"
                required
              />
            </Grid>
          </>
        );
      case 'help':
        return null;
      default:
        return (
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Target File/Component"
              variant="outlined"
              value={target}
              onChange={(e) => setTarget(e.target.value)}
              placeholder="E.g., GenerateCodeAction.java"
              required={commandType !== 'help'}
            />
          </Grid>
        );
    }
  };
  
  // Command examples for each type
  const commandExamples = {
    fix: [
      '/fix GenerateCodeAction.java',
      '/fix com.modforge.intellij.plugin.actions.FixErrorsAction.java',
      '/fix settings/ModForgeSettings.java'
    ],
    improve: [
      '/improve AIAssistPanel.java',
      '/improve com.modforge.intellij.plugin.ui.toolwindow.MetricsPanel',
      '/improve services/ContinuousDevelopmentService.java'
    ],
    document: [
      '/document SettingsPanel.java',
      '/document ai/PatternRecognitionService.java',
      '/document ModForgeToolWindowFactory.java'
    ],
    add: [
      '/add "dark mode toggle" to MetricsPanel.java',
      '/add "code folding support" to AIAssistPanel.java',
      '/add "export metrics as CSV" to ui.toolwindow'
    ],
    explain: [
      '/explain ContinuousDevelopmentService.java',
      '/explain how pattern recognition works',
      '/explain ModForgeCompilationListener.java'
    ]
  };
  
  // Get icon for command type
  const getCommandIcon = (type) => {
    switch(type) {
      case 'fix': return <BugReportIcon />;
      case 'improve': return <CodeIcon />;
      case 'document': return <DescriptionIcon />;
      case 'add': return <AddCircleIcon />;
      case 'explain': 
      case 'help':
      default: return <HelpOutlineIcon />;
    }
  };

  return (
    <div>
      <Typography variant="h4" gutterBottom component="h1" sx={{ pt: 2 }}>
        Command Interface
      </Typography>
      
      <Grid container spacing={3}>
        {/* Command Builder Panel */}
        <Grid item xs={12} lg={7}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h5" gutterBottom>
              Build a Command
            </Typography>
            
            <Typography variant="body1" paragraph>
              Use this form to generate commands for the autonomous development system.
              The commands will be sent as GitHub issues with special formatting.
            </Typography>
            
            <Box component="form" sx={{ mt: 3 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel id="command-type-label">Command Type</InputLabel>
                    <Select
                      labelId="command-type-label"
                      value={commandType}
                      label="Command Type"
                      onChange={(e) => setCommandType(e.target.value)}
                    >
                      <MenuItem value="fix">Fix Issues</MenuItem>
                      <MenuItem value="improve">Improve Code</MenuItem>
                      <MenuItem value="document">Generate Documentation</MenuItem>
                      <MenuItem value="add">Add Feature</MenuItem>
                      <MenuItem value="explain">Explain Code</MenuItem>
                      <MenuItem value="help">Help</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                {renderCommandFields()}
                
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                    <Button 
                      variant="contained" 
                      color="primary"
                      onClick={generateIssue}
                      disabled={commandType !== 'help' && !target}
                      sx={{ mr: 1 }}
                    >
                      Preview Command
                    </Button>
                    <Button 
                      variant="contained" 
                      color="secondary"
                      onClick={createGitHubIssue}
                      disabled={!generatedIssue.title}
                    >
                      Create GitHub Issue
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          </Paper>
          
          {/* Preview Panel */}
          {generatedIssue.title && (
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Generated GitHub Issue
              </Typography>
              
              <Box sx={{ bgcolor: 'background.paper', p: 2, borderRadius: 1, mb: 2 }}>
                <Typography variant="subtitle1" fontWeight="bold">
                  Title: {generatedIssue.title}
                </Typography>
                <Typography variant="body1" component="pre" sx={{ mt: 2, p: 1, bgcolor: 'grey.900', borderRadius: 1 }}>
                  {generatedIssue.body}
                </Typography>
              </Box>
              
              <Typography variant="body2" color="text.secondary">
                This will create a GitHub issue that the autonomous system will process.
              </Typography>
            </Paper>
          )}
        </Grid>
        
        {/* Command References Panel */}
        <Grid item xs={12} lg={5}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              Command Reference
            </Typography>
            
            <Accordion defaultExpanded>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <BugReportIcon sx={{ mr: 1 }} />
                  <Typography>Fix Command</Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2" paragraph>
                  The <code>/fix</code> command identifies and fixes issues in the specified file or component.
                </Typography>
                <Typography variant="subtitle2">Examples:</Typography>
                <Box component="ul" sx={{ pl: 2, mt: 1 }}>
                  {commandExamples.fix.map((example, index) => (
                    <li key={index}><Typography variant="body2"><code>{example}</code></Typography></li>
                  ))}
                </Box>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <CodeIcon sx={{ mr: 1 }} />
                  <Typography>Improve Command</Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2" paragraph>
                  The <code>/improve</code> command enhances code quality while preserving functionality.
                </Typography>
                <Typography variant="subtitle2">Examples:</Typography>
                <Box component="ul" sx={{ pl: 2, mt: 1 }}>
                  {commandExamples.improve.map((example, index) => (
                    <li key={index}><Typography variant="body2"><code>{example}</code></Typography></li>
                  ))}
                </Box>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <DescriptionIcon sx={{ mr: 1 }} />
                  <Typography>Document Command</Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2" paragraph>
                  The <code>/document</code> command adds comprehensive JavaDoc to classes and methods.
                </Typography>
                <Typography variant="subtitle2">Examples:</Typography>
                <Box component="ul" sx={{ pl: 2, mt: 1 }}>
                  {commandExamples.document.map((example, index) => (
                    <li key={index}><Typography variant="body2"><code>{example}</code></Typography></li>
                  ))}
                </Box>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <AddCircleIcon sx={{ mr: 1 }} />
                  <Typography>Add Command</Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2" paragraph>
                  The <code>/add</code> command implements new features in the specified component.
                </Typography>
                <Typography variant="subtitle2">Examples:</Typography>
                <Box component="ul" sx={{ pl: 2, mt: 1 }}>
                  {commandExamples.add.map((example, index) => (
                    <li key={index}><Typography variant="body2"><code>{example}</code></Typography></li>
                  ))}
                </Box>
              </AccordionDetails>
            </Accordion>
            
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <HelpOutlineIcon sx={{ mr: 1 }} />
                  <Typography>Explain & Help Commands</Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Typography variant="body2" paragraph>
                  The <code>/explain</code> command provides a detailed explanation of how code works.
                  The <code>/help</code> command shows available commands and usage instructions.
                </Typography>
                <Typography variant="subtitle2">Examples:</Typography>
                <Box component="ul" sx={{ pl: 2, mt: 1 }}>
                  {commandExamples.explain.map((example, index) => (
                    <li key={index}><Typography variant="body2"><code>{example}</code></Typography></li>
                  ))}
                  <li><Typography variant="body2"><code>/help</code></Typography></li>
                </Box>
              </AccordionDetails>
            </Accordion>
          </Paper>
          
          <Card sx={{ mt: 3, bgcolor: 'primary.dark' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Comment Commands
              </Typography>
              <Typography variant="body2" paragraph>
                You can also use these commands directly in issue and PR comments.
                Just add comments like <code>/improve SettingsPanel.java</code> on any issue or pull request.
              </Typography>
              <Typography variant="body2">
                The autonomous system will process your command and respond with the result in a comment.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      <Snackbar
        open={openSnackbar}
        autoHideDuration={4000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={handleSnackbarClose} severity="success" sx={{ width: '100%' }}>
          Command generated successfully!
        </Alert>
      </Snackbar>
    </div>
  );
}

export default CommandInterface;