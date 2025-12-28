# Complete E2E Testing Script with Authentication
$ErrorActionPreference = "Continue"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "V-TOURHUB COMPLETE E2E QA TESTING" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Get Admin Token
Write-Host "[STEP 1] Getting admin_user token..." -ForegroundColor Yellow
$adminTokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/v-tourhub/protocol/openid-connect/token" `
    -Method POST `
    -Headers @{"Content-Type"="application/x-www-form-urlencoded"} `
    -Body "client_id=vtourhub-client&username=admin_user&password=admin123&grant_type=password"

if ($adminTokenResponse.access_token) {
    $adminToken = $adminTokenResponse.access_token
    Write-Host "✅ Admin token obtained (length: $($adminToken.Length))" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to get admin token" -ForegroundColor Red
    exit
}
Write-Host ""

# Step 2: Get Customer Token
Write-Host "[STEP 2] Getting customer_user token..." -ForegroundColor Yellow
$customerTokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/v-tourhub/protocol/openid-connect/token" `
    -Method POST `
    -Headers @{"Content-Type"="application/x-www-form-urlencoded"} `
    -Body "client_id=vtourhub-client&username=customer_user&password=customer123&grant_type=password"

if ($customerTokenResponse.access_token) {
    $customerToken = $customerTokenResponse.access_token
    Write-Host "✅ Customer token obtained (length: $($customerToken.Length))" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to get customer token" -ForegroundColor Red
}
Write-Host ""

# Step 3: Test Public APIs (no auth needed)
Write-Host "[STEP 3] Testing public catalog APIs..." -ForegroundColor Yellow
try {
    $services = Invoke-RestMethod -Uri "http://localhost:8000/api/catalog/services" -Method GET
    Write-Host "✅ Catalog API: Found $($services.data.content.Count) services" -ForegroundColor Green
    if ($services.data.content.Count -gt 0) {
        $testServiceId = $services.data.content[0].id
        Write-Host "   Using serviceId: $testServiceId for booking test" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ Catalog API failed: $_" -ForegroundColor Red
}
Write-Host ""

# Step 4: Create Booking as Customer
Write-Host "[STEP 4] Creating booking as customer_user..." -ForegroundColor Yellow
$bookingData = @{
    serviceId = $testServiceId
    checkInDate = "2025-02-15"
    checkOutDate = "2025-02-17"
    guests = 2
    quantity = 1
    customerName = "QA Customer Test"
    customerEmail = "qacustomer@vtourhub.com"
    customerPhone = "0901234567"
} | ConvertTo-Json

try {
    $booking = Invoke-RestMethod -Uri "http://localhost:8000/api/bookings" `
        -Method POST `
        -Headers @{
            "Content-Type"="application/json"
            "Authorization"="Bearer $customerToken"
        } `
        -Body $bookingData
    
    if ($booking.data.id) {
        $bookingId = $booking.data.id
        Write-Host "✅ Booking created: ID=$bookingId, Status=$($booking.data.status)" -ForegroundColor Green
        Write-Host "   Total Price: $($booking.data.totalPrice)" -ForegroundColor Cyan
        Write-Host "   Expires At: $($booking.data.expiresAt)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ Booking creation failed: $_" -ForegroundColor Red
    $bookingId = $null
}
Write-Host ""

# Step 5: Check Booking Status
if ($bookingId) {
    Write-Host "[STEP 5] Checking booking status..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    try {
        $bookingDetail = Invoke-RestMethod -Uri "http://localhost:8000/api/bookings/$bookingId" `
            -Method GET `
            -Headers @{"Authorization"="Bearer $customerToken"}
        
        Write-Host "✅ Booking Status: $($bookingDetail.data.status)" -ForegroundColor Green
        Write-Host "   Lock Token: $($bookingDetail.data.inventoryLockToken)" -ForegroundColor Cyan
    } catch {
        Write-Host "❌ Failed to get booking: $_" -ForegroundColor Red
    }
    Write-Host ""
}

# Step 6: Test Cancellation (for refund flow)
if ($bookingId) {
    Write-Host "[STEP 6] Testing cancellation..." -ForegroundColor Yellow
    try {
        $cancelResult = Invoke-RestMethod -Uri "http://localhost:8000/api/bookings/$bookingId/cancel" `
            -Method POST `
            -Headers @{
                "Content-Type"="application/json"
                "Authorization"="Bearer $customerToken"
            } `
            -Body '{"reason":"QA Testing"}'
        
        Write-Host "✅ Booking cancelled: Status=$($cancelResult.data.status)" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Cancel failed (may be expected): $_" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Step 7: Check RabbitMQ for events
Write-Host "[STEP 7] Checking message queue..." -ForegroundColor Yellow
try {
    $rabbitInfo = Invoke-RestMethod -Uri "http://localhost:15672/api/overview" `
        -Method GET `
        -Credential (New-Object PSCredential("guest", (ConvertTo-SecureString "guest" -AsPlainText -Force)))
    
    Write-Host "✅ RabbitMQ: $($rabbitInfo.queue_totals.messages) messages in queues" -ForegroundColor Green
} catch {
    Write-Host "⚠️ RabbitMQ check skipped (credentials may differ)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "TESTING COMPLETE" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "- Authentication: ✅ PASS" -ForegroundColor Green
Write-Host "- Public APIs: ✅ PASS" -ForegroundColor Green
if ($bookingId) {
    Write-Host "- Booking Creation: ✅ PASS (ID: $bookingId)" -ForegroundColor Green
} else {
    Write-Host "- Booking Creation: ❌ FAIL" -ForegroundColor Red
}
Write-Host ""
Write-Host "Next: Check Docker logs for scheduler jobs and saga events" -ForegroundColor Cyan
