-- 1. Identity Service (Keycloak)
CREATE DATABASE IF NOT EXISTS keycloak_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Catalog Service (Master Data)
CREATE DATABASE IF NOT EXISTS catalog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Booking Service (Transactional)
CREATE DATABASE IF NOT EXISTS booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. User Profile Service
CREATE DATABASE IF NOT EXISTS user_profile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 7. Payment Service
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 8. Notification Service
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: Create a common user for services (Security Best Practice)
-- Thay vì dùng root, các service nên dùng user này
CREATE USER IF NOT EXISTS 'soa_user'@'%' IDENTIFIED BY 'soa_password';
GRANT ALL PRIVILEGES ON *.* TO 'soa_user'@'%';
FLUSH PRIVILEGES;