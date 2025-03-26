-- Create database
CREATE DATABASE IF NOT EXISTS spotify_auth_db;
USE spotify_auth_db;

-- Create tokens table
CREATE TABLE IF NOT EXISTS spotify_tokens (
    user_id VARCHAR(255) PRIMARY KEY,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    access_token_expiry TIMESTAMP NOT NULL,
    scope TEXT,
    issued_at TIMESTAMP NOT NULL
);