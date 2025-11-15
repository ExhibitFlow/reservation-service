-- PostgreSQL Database Setup Script for Reservation Service
-- Database: reservation_db
-- Created: 2025-11-13 (Migrated from MySQL)

-- Create database if it doesn't exist
-- Run this as postgres superuser or database admin
-- CREATE DATABASE reservation_db WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';

-- Connect to the database
\c reservation_db;

-- Drop existing table if exists (for clean setup)
DROP TABLE IF EXISTS reservations CASCADE;

-- Create reservations table
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stall_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_base64 TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    payment_expires_at TIMESTAMP NULL,
    payment_completed_at TIMESTAMP NULL,
    
    -- Constraint to ensure status is valid
    CONSTRAINT chk_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_user_id ON reservations(user_id);
CREATE INDEX idx_stall_id ON reservations(stall_id);
CREATE INDEX idx_status ON reservations(status);
CREATE INDEX idx_created_at ON reservations(created_at);
CREATE INDEX idx_payment_expires ON reservations(payment_expires_at);

-- Composite indexes for common queries
CREATE INDEX idx_user_stall ON reservations(user_id, stall_id);
CREATE INDEX idx_status_stall ON reservations(status, stall_id);

-- Create a user for the application (optional - update credentials as needed)
-- CREATE USER reservation_user WITH PASSWORD 'your_password';
-- GRANT ALL PRIVILEGES ON DATABASE reservation_db TO reservation_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO reservation_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO reservation_user;

-- Display table structure
\d+ reservations;

-- Show initial record count
SELECT COUNT(*) as total_reservations FROM reservations;
