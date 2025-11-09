# Database Setup Guide

## Overview
This directory contains SQL scripts for setting up the MySQL database for the Reservation Service.

## Prerequisites
- MySQL Server 8.0 or higher installed
- MySQL client or MySQL Workbench

## Database Configuration
- **Database Name**: `reservation_db`
- **Default Port**: 3306
- **Username**: `root` (as configured in application.properties)
- **Password**: `root` (as configured in application.properties)

## Setup Instructions

### Option 1: Automatic Setup (Recommended)
The application is configured with `spring.jpa.hibernate.ddl-auto=update`, which means:
- The database will be created automatically when you run the application
- Tables will be created/updated based on JPA entities

Simply start the application and the database will be initialized automatically.

### Option 2: Manual Setup
If you prefer to create the database manually:

1. **Start MySQL Server**
   ```bash
   # On Windows (if MySQL is in PATH)
   net start MySQL80
   
   # Or start via MySQL Workbench or XAMPP/WAMP
   ```

2. **Connect to MySQL**
   ```bash
   mysql -u root -p
   ```

3. **Run the setup script**
   ```sql
   source d:/Github_repos/Architecture-assignment/Reservation-service/database/setup.sql
   ```
   
   Or using MySQL Workbench:
   - Open MySQL Workbench
   - Connect to your local MySQL instance
   - File > Run SQL Script
   - Select `setup.sql`

4. **Verify the setup**
   ```sql
   USE reservation_db;
   SHOW TABLES;
   DESCRIBE reservations;
   ```

### Option 3: Command Line (Windows)
```bash
# Navigate to the database directory
cd d:\Github_repos\Architecture-assignment\Reservation-service\database

# Execute the script
mysql -u root -p < setup.sql
```

## Database Schema

### Table: `reservations`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique reservation identifier |
| user_id | BIGINT | NOT NULL | Reference to user (from User Service) |
| stall_id | BIGINT | NOT NULL | Reference to stall (from Stall Service) |
| created_at | TIMESTAMP | NOT NULL | Timestamp when reservation was created |
| qr_code_base64 | LONGTEXT | NULL | Base64 encoded QR code image |
| status | VARCHAR(20) | NOT NULL | Reservation status (CONFIRMED/CANCELLED) |

### Indexes
- `idx_user_id`: Fast lookups by user
- `idx_stall_id`: Fast lookups by stall
- `idx_status`: Filter by status
- `idx_created_at`: Sort by creation date
- `idx_user_stall`: Composite index for user-stall queries

## Updating Database Credentials

If you want to use different credentials, update the following file:
`src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/reservation_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Troubleshooting

### Connection Issues
- Ensure MySQL server is running
- Check that port 3306 is not blocked by firewall
- Verify username and password in application.properties

### Permission Issues
```sql
-- Grant permissions to user
GRANT ALL PRIVILEGES ON reservation_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### Reset Database
```sql
DROP DATABASE reservation_db;
-- Then run setup.sql again
```

## Maintenance Scripts

### Clear all data (keep structure)
```sql
USE reservation_db;
TRUNCATE TABLE reservations;
```

### View recent reservations
```sql
USE reservation_db;
SELECT * FROM reservations ORDER BY created_at DESC LIMIT 10;
```

### Count reservations by status
```sql
USE reservation_db;
SELECT status, COUNT(*) as count 
FROM reservations 
GROUP BY status;
```
