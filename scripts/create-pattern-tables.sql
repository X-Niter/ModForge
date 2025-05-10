-- Create tables for pattern learning system

-- Code Patterns table
CREATE TABLE IF NOT EXISTS code_patterns (
  id SERIAL PRIMARY KEY,
  pattern_type TEXT NOT NULL,
  prompt TEXT NOT NULL,
  mod_loader TEXT NOT NULL,
  minecraft_version TEXT NOT NULL,
  language TEXT NOT NULL DEFAULT 'java',
  input_pattern TEXT,
  output_code TEXT NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}',
  use_count INTEGER NOT NULL DEFAULT 0,
  success_rate INTEGER NOT NULL DEFAULT 100,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Error Patterns table
CREATE TABLE IF NOT EXISTS error_patterns (
  id SERIAL PRIMARY KEY,
  error_type TEXT NOT NULL,
  error_pattern TEXT NOT NULL,
  fix_strategy TEXT NOT NULL,
  mod_loader TEXT NOT NULL,
  success_count INTEGER NOT NULL DEFAULT 0,
  failure_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Idea Patterns table
CREATE TABLE IF NOT EXISTS idea_patterns (
  id SERIAL PRIMARY KEY,
  keywords TEXT[] NOT NULL,
  category TEXT NOT NULL,
  response_content JSONB NOT NULL,
  use_count INTEGER NOT NULL DEFAULT 0,
  success_rate INTEGER NOT NULL DEFAULT 100,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Idea Expansion Patterns table
CREATE TABLE IF NOT EXISTS idea_expansion_patterns (
  id SERIAL PRIMARY KEY,
  original_idea_title TEXT NOT NULL,
  original_idea_description TEXT NOT NULL,
  expansion_content JSONB NOT NULL,
  key_terms TEXT[] NOT NULL,
  use_count INTEGER NOT NULL DEFAULT 0,
  success_rate INTEGER NOT NULL DEFAULT 100,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Feature Patterns table
CREATE TABLE IF NOT EXISTS feature_patterns (
  id SERIAL PRIMARY KEY,
  feature_type TEXT NOT NULL,
  description TEXT NOT NULL,
  mod_loader TEXT NOT NULL,
  implementation JSONB NOT NULL,
  key_terms TEXT[] NOT NULL,
  success_count INTEGER NOT NULL DEFAULT 0,
  failure_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Documentation Patterns table
CREATE TABLE IF NOT EXISTS documentation_patterns (
  id SERIAL PRIMARY KEY,
  code_type TEXT NOT NULL,
  language TEXT NOT NULL DEFAULT 'java',
  style TEXT NOT NULL DEFAULT 'javadoc',
  original_code TEXT NOT NULL,
  generated_docs TEXT NOT NULL,
  use_count INTEGER NOT NULL DEFAULT 0,
  success_rate INTEGER NOT NULL DEFAULT 100,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);