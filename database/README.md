# Database Setup Guide

## Overview
This directory contains SQL scripts for setting up the PostgreSQL database for the Reservation Service.

## Prerequisites
- PostgreSQL Server 12 or higher installed
- psql command-line tool or pgAdmin

## Database Configuration
- **Database Name**: `reservation_db`
- **Default Port**: 5432
- **Username**: `postgres` (as configured in application.properties)
- **Password**: `postgres` (as configured in application.properties)

## Setup Instructions

### Option 1: Automatic Setup (Recommended)
The application is configured with `spring.jpa.hibernate.ddl-auto=update`, which means:
- Tables will be created/updated automatically based on JPA entities
- No manual SQL execution required

**Steps:**
1. Create the database first:
   ```bash
   psql -U postgres -c "CREATE DATABASE reservation_db WITH ENCODING 'UTF8';"
   ```

2. Start the application and tables will be initialized automatically.

### Option 2: Manual Setup
If you prefer to create the database and tables manually:

1. **Start PostgreSQL Server**
   ```bash
   # On Windows (if PostgreSQL is in PATH)
   pg_ctl -D "C:\Program Files\PostgreSQL\15\data" start
   
   # On Linux/macOS
   sudo systemctl start postgresql
   # or
   brew services start postgresql
   ```

2. **Create Database**
   ```bash
   psql -U postgres -c "CREATE DATABASE reservation_db WITH ENCODING 'UTF8';"
   ```

3. **Run the setup script**
   ```bash
   psql -U postgres -d reservation_db -f setup.sql
   ```
   
   Or using pgAdmin:
   - Open pgAdmin
   - Connect to your local PostgreSQL instance
   - Right-click on reservation_db > Query Tool
   - Open and execute `setup.sql`

## Verify Installation

### Check Database Exists
```bash
psql -U postgres -l
```

### Connect to Database
```bash
psql -U postgres -d reservation_db
```

### View Tables
```sql
\dt
```

### View Table Structure
```sql
\d+ reservations
```

### Exit psql
```sql
\q
```

## Table Schema

The `reservations` table has the following structure:

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PRIMARY KEY |
| user_id | BIGINT | NOT NULL |
| stall_id | BIGINT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| qr_code_base64 | TEXT | NULL |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'CONFIRMED' |
| payment_expires_at | TIMESTAMP | NULL |
| payment_completed_at | TIMESTAMP | NULL |

**Valid Status Values:**
- PENDING_PAYMENT
- CONFIRMED
- CANCELLED
- EXPIRED

**Indexes:**
- idx_user_id (user_id)
- idx_stall_id (stall_id)
- idx_status (status)
- idx_created_at (created_at)
- idx_payment_expires (payment_expires_at)
- idx_user_stall (user_id, stall_id)
- idx_status_stall (status, stall_id)

## Connection Details

Update these in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/reservation_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## Troubleshooting

### Issue: "FATAL: database 'reservation_db' does not exist"
**Solution:** Create the database first using the psql command above.

### Issue: "FATAL: password authentication failed"
**Solution:** Verify your PostgreSQL password or reset it:
```bash
sudo -u postgres psql
ALTER USER postgres WITH PASSWORD 'postgres';
```

### Issue: "Connection refused"
**Solution:** Ensure PostgreSQL service is running:
```bash
# Windows
services.msc (check postgresql service)

# Linux
sudo systemctl status postgresql
```

## Migration from MySQL

If you have existing data in MySQL and need to migrate to PostgreSQL, please refer to `POSTGRESQL_MIGRATION.md` in the root directory for detailed migration instructions.

## PostgreSQL Useful Commands

```sql
-- List all databases
\l

-- Connect to database
\c reservation_db

-- List all tables
\dt

-- Describe table structure
\d+ reservations

-- Show all indexes
\di

-- View recent reservations
SELECT * FROM reservations ORDER BY created_at DESC LIMIT 10;

-- Count reservations by status
SELECT status, COUNT(*) FROM reservations GROUP BY status;

-- Drop database (careful!)
DROP DATABASE reservation_db;
```

## Performance Tips

1. **Regular Maintenance:**
   ```sql
   VACUUM ANALYZE reservations;
   ```

2. **Monitor Query Performance:**
   ```sql
   EXPLAIN ANALYZE SELECT * FROM reservations WHERE user_id = 1;
   ```

3. **Check Index Usage:**
   ```sql
   SELECT schemaname, tablename, indexname, idx_scan 
   FROM pg_stat_user_indexes 
   WHERE tablename = 'reservations';
   ```

## Additional Resources

- PostgreSQL Documentation: https://www.postgresql.org/docs/
- pgAdmin Download: https://www.pgadmin.org/download/
- PostgreSQL Tutorial: https://www.postgresqltutorial.com/
