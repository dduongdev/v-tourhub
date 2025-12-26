# Script test Review Service và Analytics Service
# Chạy: .\test-services.ps1

$baseUrl = "http://localhost:8000"  # API Gateway
$reviewUrl = "$baseUrl/api/reviews"
$analyticsUrl = "$baseUrl/api/analytics"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "TEST REVIEW SERVICE & ANALYTICS SERVICE" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check - Kiểm tra services có chạy không
Write-Host "1. Testing Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$reviewUrl/destinations/1" -Method GET -UseBasicParsing -ErrorAction SilentlyContinue
    Write-Host "   ✓ Review Service is running" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Review Service is NOT running or not accessible" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "$analyticsUrl/reviews/destinations/1" -Method GET -UseBasicParsing -ErrorAction SilentlyContinue
    Write-Host "   ✓ Analytics Service is running" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Analytics Service is NOT running or not accessible" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Tạo Review mới
Write-Host "2. Testing Create Review..." -ForegroundColor Yellow
$reviewBody = @{
    userId = 1
    destinationId = 1
    reviewType = "DESTINATION"
    rating = 5
    comment = "Địa điểm tuyệt vời! Rất đáng để ghé thăm."
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$reviewUrl" -Method POST -Body $reviewBody -ContentType "application/json" -ErrorAction Stop
    $reviewId = $response.data.id
    Write-Host "   ✓ Review created successfully! ID: $reviewId" -ForegroundColor Green
    Write-Host "   Rating: $($response.data.rating), Comment: $($response.data.comment)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Failed to create review" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Response: $responseBody" -ForegroundColor Red
    }
}
Write-Host ""

# Đợi một chút để RabbitMQ xử lý event
Write-Host "   Waiting 3 seconds for RabbitMQ to process event..." -ForegroundColor Gray
Start-Sleep -Seconds 3
Write-Host ""

# Test 3: Kiểm tra Analytics sau khi tạo review
Write-Host "3. Testing Analytics after Review Creation..." -ForegroundColor Yellow
try {
    $analytics = Invoke-RestMethod -Uri "$analyticsUrl/reviews/destinations/1" -Method GET -ErrorAction Stop
    Write-Host "   ✓ Analytics retrieved successfully!" -ForegroundColor Green
    Write-Host "   Average Rating: $($analytics.data.averageRating)" -ForegroundColor Cyan
    Write-Host "   Total Reviews: $($analytics.data.totalReviews)" -ForegroundColor Cyan
    Write-Host "   Rating Distribution:" -ForegroundColor Cyan
    Write-Host "     - 5 stars: $($analytics.data.rating5Count)" -ForegroundColor Gray
    Write-Host "     - 4 stars: $($analytics.data.rating4Count)" -ForegroundColor Gray
    Write-Host "     - 3 stars: $($analytics.data.rating3Count)" -ForegroundColor Gray
    Write-Host "     - 2 stars: $($analytics.data.rating2Count)" -ForegroundColor Gray
    Write-Host "     - 1 star:  $($analytics.data.rating1Count)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Failed to get analytics" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Tạo thêm reviews để test
Write-Host "4. Creating additional reviews for testing..." -ForegroundColor Yellow
$reviews = @(
    @{ rating = 4; comment = "Tốt nhưng cần cải thiện dịch vụ" },
    @{ rating = 5; comment = "Xuất sắc!" },
    @{ rating = 3; comment = "Ổn" }
)

foreach ($r in $reviews) {
    $reviewBody = @{
        userId = (Get-Random -Minimum 2 -Maximum 10)
        destinationId = 1
        reviewType = "DESTINATION"
        rating = $r.rating
        comment = $r.comment
        status = "ACTIVE"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$reviewUrl" -Method POST -Body $reviewBody -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   ✓ Created review with rating $($r.rating)" -ForegroundColor Green
    } catch {
        Write-Host "   ✗ Failed to create review with rating $($r.rating)" -ForegroundColor Red
    }
    Start-Sleep -Seconds 1
}
Write-Host ""

# Đợi RabbitMQ xử lý
Write-Host "   Waiting 3 seconds for RabbitMQ to process events..." -ForegroundColor Gray
Start-Sleep -Seconds 3
Write-Host ""

# Test 5: Kiểm tra Analytics sau nhiều reviews
Write-Host "5. Testing Analytics with Multiple Reviews..." -ForegroundColor Yellow
try {
    $analytics = Invoke-RestMethod -Uri "$analyticsUrl/reviews/destinations/1" -Method GET -ErrorAction Stop
    Write-Host "   ✓ Final Analytics:" -ForegroundColor Green
    Write-Host "   Average Rating: $($analytics.data.averageRating)" -ForegroundColor Cyan
    Write-Host "   Total Reviews: $($analytics.data.totalReviews)" -ForegroundColor Cyan
} catch {
    Write-Host "   ✗ Failed to get analytics" -ForegroundColor Red
}
Write-Host ""

# Test 6: Lấy danh sách reviews
Write-Host "6. Testing Get Reviews List..." -ForegroundColor Yellow
try {
    $reviews = Invoke-RestMethod -Uri "$reviewUrl/destinations/1?page=0&size=10" -Method GET -ErrorAction Stop
    Write-Host "   ✓ Retrieved $($reviews.data.content.Count) reviews" -ForegroundColor Green
    foreach ($r in $reviews.data.content) {
        Write-Host "   - Review #$($r.id): Rating $($r.rating) - $($r.comment.Substring(0, [Math]::Min(50, $r.comment.Length)))..." -ForegroundColor Gray
    }
} catch {
    Write-Host "   ✗ Failed to get reviews list" -ForegroundColor Red
}
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "TEST COMPLETED" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

