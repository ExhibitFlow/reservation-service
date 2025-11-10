-- MySQL Database Setup Script for Reservation Service
-- Database: reservation_db
-- Created: 2025-11-09

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS reservation_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE reservation_db;

-- Drop existing table if exists (for clean setup)
DROP TABLE IF EXISTS reservations;

-- Create reservations table
CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stall_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_base64 LONGTEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    payment_expires_at TIMESTAMP NULL,
    payment_completed_at TIMESTAMP NULL,
    
    -- Indexes for better query performance
    INDEX idx_user_id (user_id),
    INDEX idx_stall_id (stall_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_payment_expires (payment_expires_at),
    
    -- Composite index for common queries
    INDEX idx_user_stall (user_id, stall_id),
    INDEX idx_status_stall (status, stall_id),
    
    -- Constraint to ensure status is valid
    CONSTRAINT chk_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create a user for the application (optional - update credentials as needed)
-- GRANT ALL PRIVILEGES ON reservation_db.* TO 'reservation_user'@'localhost' IDENTIFIED BY 'your_password';
-- FLUSH PRIVILEGES;

-- Display table structure
DESCRIBE reservations;

-- Show initial record count
SELECT COUNT(*) as total_reservations FROM reservations;
