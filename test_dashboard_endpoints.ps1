# Test Admin Dashboard API Endpoints
# Run this after logging in to get a valid token

Write-Host "Testing Admin Dashboard API Endpoints..." -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost/api"

# Test endpoints
$endpoints = @(
    "/masterdata/workstations",
    "/users",
    "/production-control-orders",
    "/assembly-control-orders",
    "/supply-orders/warehouse",
    "/customer-orders",
    "/stock/alerts/low",
    "/masterdata/product-variants"
)

foreach ($endpoint in $endpoints) {
    Write-Host "Testing: $endpoint" -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl$endpoint" -Method GET -UseBasicParsing 2>&1
        if ($response.StatusCode -eq 200) {
            $data = $response.Content | ConvertFrom-Json
            $count = if ($data -is [Array]) { $data.Count } else { 1 }
            Write-Host "  ✓ Success - $count items" -ForegroundColor Green
        }
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 401) {
            Write-Host "  ✗ 401 Unauthorized - Login required" -ForegroundColor Red
        }
        elseif ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "  ✗ 404 Not Found" -ForegroundColor Red
        }
        else {
            Write-Host "  ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    Write-Host ""
}

Write-Host "Note: Most endpoints require authentication. Login first at http://localhost" -ForegroundColor Cyan
