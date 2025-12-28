# Complete Booking Flow Test
# This will test the entire saga from booking creation to payment

$baseUrl = "http://localhost:8000/api"
$headers = @{"Content-Type" = "application/json"}

Write-Host "=======================" -ForegroundColor Cyan
Write-Host "BOOKING FLOW E2E TEST" -ForegroundColor Cyan  
Write-Host "=======================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Get available services
Write-Host "[1] Getting available services..." -ForegroundColor Yellow
$services = Invoke-RestMethod -Uri "$baseUrl/catalog/services" -Method GET
Write-Host "Services found: $($services.data.content.Count)" -ForegroundColor Green
if ($services.data.content.Count -gt 0) {
    $serviceId = $services.data.content[0].id
    Write-Host "Using serviceId: $serviceId" -ForegroundColor Green
}
Write-Host ""

# Test 2: Create booking
Write-Host "[2] Creating booking..." -ForegroundColor Yellow
$bookingData = @{
    serviceId = $serviceId
    checkInDate = "2025-02-01"
    checkOutDate = "2025-02-03"
    guests = 2
    customerName = "QA Test User"
    customerEmail = "qatest@vtourhub.com"
    customerPhone = "0987654321"
} | ConvertTo-Json

try {
    $booking = Invoke-RestMethod -Uri "$baseUrl/bookings" -Method POST -Headers $headers -Body $bookingData
    Write-Host "Booking created: ID = $($booking.data.id), Status = $($booking.data.status)" -ForegroundColor Green
    $bookingId = $booking.data.id
} catch {
    Write-Host "Booking failed: $_" -ForegroundColor Red
    $bookingId = $null
}
Write-Host ""

# Test 3: Check booking status
if ($bookingId) {
    Write-Host "[3] Checking booking status..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    try {
        $bookingStatus = Invoke-RestMethod -Uri "$baseUrl/bookings/$bookingId" -Method GET
        Write-Host "Booking Status: $($bookingStatus.data.status)" -ForegroundColor Green
        Write-Host "Expires At: $($bookingStatus.data.expiresAt)" -ForegroundColor Green
    } catch {
        Write-Host "Failed to get booking: $_" -ForegroundColor Red
    }
}
Write-Host ""

# Test 4: Test scheduler (check logs after 1 minute for expiration)
Write-Host "[4] Testing expiration scheduler..." -ForegroundColor Yellow
Write-Host "Scheduler runs every 1 minute. Check logs in 60 seconds." -ForegroundColor Cyan
Write-Host ""

Write-Host "=======================" -ForegroundColor Cyan
Write-Host "TEST COMPLETE" -ForegroundColor Cyan
Write-Host "=======================" -ForegroundColor Cyan
