
\c reservation_db;

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS reservations CASCADE;


-- Create reservations table
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    stall_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_base64 TEXT,
    payment_expires_at TIMESTAMP NULL,
    payment_completed_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    
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


-- Display table structure
\d+ reservations;

-- Show initial record count
SELECT COUNT(*) as total_reservations FROM reservations;
