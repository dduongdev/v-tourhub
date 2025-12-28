@echo off
REM V-TourHub API Testing Script
REM Testing all main APIs through API Gateway (port 8000)

echo ========================================
echo V-TourHub E2E API Testing
echo ========================================
echo.

echo [TEST 1] GET Destinations
curl.exe -s -X GET http://localhost:8000/api/catalog/destinations
echo.
echo.

echo [TEST 2] GET Tourism Services
curl.exe -s -X GET http://localhost:8000/api/catalog/services
echo.
echo.

echo [TEST 3] GET Service Detail (assuming ID 1 exists)
curl.exe -s -X GET http://localhost:8000/api/catalog/services/1
echo.
echo.

echo [TEST 4] Create Booking (will likely fail without auth token)
curl.exe -s -X POST http://localhost:8000/api/bookings ^
-H "Content-Type: application/json" ^
-d "{\"serviceId\":1,\"checkInDate\":\"2025-01-15\",\"checkOutDate\":\"2025-01-17\",\"guests\":2,\"customerName\":\"Test User\",\"customerEmail\":\"test@example.com\",\"customerPhone\":\"0123456789\"}"
echo.
echo.

echo [TEST 5] Health Check - Booking Service
curl.exe -s http://localhost:8081/actuator/health
echo.
echo.

echo ========================================
echo Testing Complete
echo ========================================
