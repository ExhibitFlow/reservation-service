-- Initial schema for reservation service
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    stall_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    qr_code_base64 TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    payment_expires_at TIMESTAMP NULL,
    payment_completed_at TIMESTAMP NULL,
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_id ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_stall_id ON reservations(stall_id);
CREATE INDEX IF NOT EXISTS idx_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_created_at ON reservations(created_at);
CREATE INDEX IF NOT EXISTS idx_payment_expires ON reservations(payment_expires_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_user_stall ON reservations(user_id, stall_id);
CREATE INDEX IF NOT EXISTS idx_status_stall ON reservations(status, stall_id);
