-- PostgreSQL Database Setup Script for Reservation Service
-- Database: reservation_db
-- Created: 2025-11-13 (Migrated from MySQL)
-- Updated: 2025-11-22 (Removed stalls table - managed by separate Stall service)

-- Create database if it doesn't exist
-- Run this as postgres superuser or database admin
-- CREATE DATABASE reservation_db WITH ENCODING 'UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';

-- Connect to the database
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
-- Example: Creating a 10x10 grid of stalls
-- Each stall is approximately 5m x 5m
-- Coordinates are in WGS 84 (SRID 4326) - latitude/longitude format

-- Zone A - Ground Floor (Stalls A1-A10)
INSERT INTO stalls (stall_code, size, price, is_reserved, location, boundary, zone, floor_number, description) VALUES
('A1', 'Small', 500.00, FALSE, 
    ST_SetSRID(ST_MakePoint(-0.001000, 0.000000), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.001025 -0.000025, -0.000975 -0.000025, -0.000975 0.000025, -0.001025 0.000025, -0.001025 -0.000025)')), 4326),
    'Zone A', 1, 'Corner stall with good visibility'),
('A2', 'Small', 500.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000900, 0.000000), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000925 -0.000025, -0.000875 -0.000025, -0.000875 0.000025, -0.000925 0.000025, -0.000925 -0.000025)')), 4326),
    'Zone A', 1, 'Central location'),
('A3', 'Medium', 750.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000800, 0.000000), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000825 -0.000040, -0.000775 -0.000040, -0.000775 0.000040, -0.000825 0.000040, -0.000825 -0.000040)')), 4326),
    'Zone A', 1, 'Medium sized stall'),
('A4', 'Small', 500.00, TRUE,
    ST_SetSRID(ST_MakePoint(-0.000700, 0.000000), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000725 -0.000025, -0.000675 -0.000025, -0.000675 0.000025, -0.000725 0.000025, -0.000725 -0.000025)')), 4326),
    'Zone A', 1, 'Already reserved'),
('A5', 'Large', 1000.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000600, 0.000000), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000625 -0.000050, -0.000575 -0.000050, -0.000575 0.000050, -0.000625 0.000050, -0.000625 -0.000050)')), 4326),
    'Zone A', 1, 'Large premium stall');

-- Zone B - Ground Floor (Stalls B1-B5)
INSERT INTO stalls (stall_code, size, price, is_reserved, location, boundary, zone, floor_number, description) VALUES
('B1', 'Small', 500.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.001000, 0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.001025 0.000075, -0.000975 0.000075, -0.000975 0.000125, -0.001025 0.000125, -0.001025 0.000075)')), 4326),
    'Zone B', 1, 'North side location'),
('B2', 'Medium', 750.00, TRUE,
    ST_SetSRID(ST_MakePoint(-0.000900, 0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000925 0.000060, -0.000875 0.000060, -0.000875 0.000140, -0.000925 0.000140, -0.000925 0.000060)')), 4326),
    'Zone B', 1, 'Already reserved'),
('B3', 'Small', 500.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000800, 0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000825 0.000075, -0.000775 0.000075, -0.000775 0.000125, -0.000825 0.000125, -0.000825 0.000075)')), 4326),
    'Zone B', 1, 'Available stall'),
('B4', 'Large', 1000.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000700, 0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000725 0.000050, -0.000675 0.000050, -0.000675 0.000150, -0.000725 0.000150, -0.000725 0.000050)')), 4326),
    'Zone B', 1, 'Large corner stall'),
('B5', 'Medium', 750.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000600, 0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000625 0.000060, -0.000575 0.000060, -0.000575 0.000140, -0.000625 0.000140, -0.000625 0.000060)')), 4326),
    'Zone B', 1, 'Good traffic flow');

-- Zone C - First Floor (Stalls C1-C5)
INSERT INTO stalls (stall_code, size, price, is_reserved, location, boundary, zone, floor_number, description) VALUES
('C1', 'Small', 450.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.001000, -0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.001025 -0.000125, -0.000975 -0.000125, -0.000975 -0.000075, -0.001025 -0.000075, -0.001025 -0.000125)')), 4326),
    'Zone C', 2, 'First floor economy'),
('C2', 'Small', 450.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000900, -0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000925 -0.000125, -0.000875 -0.000125, -0.000875 -0.000075, -0.000925 -0.000075, -0.000925 -0.000125)')), 4326),
    'Zone C', 2, 'Available'),
('C3', 'Medium', 700.00, TRUE,
    ST_SetSRID(ST_MakePoint(-0.000800, -0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000825 -0.000140, -0.000775 -0.000140, -0.000775 -0.000060, -0.000825 -0.000060, -0.000825 -0.000140)')), 4326),
    'Zone C', 2, 'Already reserved'),
('C4', 'Small', 450.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000700, -0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000725 -0.000125, -0.000675 -0.000125, -0.000675 -0.000075, -0.000725 -0.000075, -0.000725 -0.000125)')), 4326),
    'Zone C', 2, 'Quiet area'),
('C5', 'Large', 900.00, FALSE,
    ST_SetSRID(ST_MakePoint(-0.000600, -0.000100), 4326),
    ST_SetSRID(ST_MakePolygon(ST_GeomFromText('LINESTRING(-0.000625 -0.000150, -0.000575 -0.000150, -0.000575 -0.000050, -0.000625 -0.000050, -0.000625 -0.000150)')), 4326),
    'Zone C', 2, 'Premium first floor location');

    'Zone C', 2, 'Premium first floor location');

-- Create a user for the application (optional - update credentials as needed)
-- CREATE USER reservation_user WITH PASSWORD 'your_password';
-- GRANT ALL PRIVILEGES ON DATABASE reservation_db TO reservation_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO reservation_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO reservation_user;

-- Display table structures
\d+ stalls;
\d+ reservations;

-- Show initial record counts
SELECT COUNT(*) as total_stalls FROM stalls;
SELECT COUNT(*) as total_reservations FROM reservations;

-- Show spatial reference system info
SELECT * FROM spatial_ref_sys WHERE srid = 4326;
