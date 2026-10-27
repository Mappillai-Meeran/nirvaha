# Nirvaha Clean Retake Reset Script
# Use this script to reset the database state before recording a video take.
# It ensures Officer and Citizen accounts exist and the 2 baseline clustering incidents are active.

$backendUrl = "http://localhost:8080"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  NIRVAHA DEMO RETAKE RESET (Boulevard Ward 5)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

try {
    # 1. Authenticate as Citizen
    $loginResponse = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"citizen@puducherry.gov.in","password":"Citizen123!"}'
    $token = $loginResponse.token
    $citizenId = $loginResponse.user.id
    $headers = @{ 'Authorization' = "Bearer $token"; 'Content-Type' = 'application/json' }
    Write-Host "[OK] Citizen authenticated: $($loginResponse.user.email) (ID: $citizenId)" -ForegroundColor Green

    # 2. Authenticate as Officer
    $officerLogin = Invoke-RestMethod -Uri "$backendUrl/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"officer@puducherry.gov.in","password":"Officer123!"}'
    $officerHeaders = @{ 'Authorization' = "Bearer $($officerLogin.token)" }
    Write-Host "[OK] Officer authenticated: $($officerLogin.user.email)" -ForegroundColor Green

    # 3. Check existing incidents
    $allIncidents = Invoke-RestMethod -Uri "$backendUrl/api/incidents?includeSimulated=true" -Method Get -Headers $officerHeaders
    Write-Host "[INFO] Total active incidents in database: $($allIncidents.Count)" -ForegroundColor Yellow

    # Check for Baseline Report 1
    $base1 = $allIncidents | Where-Object { $_.title -like "*Blocked Storm Drain*" } | Select-Object -First 1
    if (-not $base1) {
        $body1 = @{
            reporterId = $citizenId
            title = "[DEMO] Blocked Storm Drain - Heritage Blvd"
            category = "DRAINAGE"
            description = "Stormwater drain blocked by construction debris, water pooling."
            address = "Heritage Boulevard, Ward 5, Puducherry"
            latitude = 11.9353
            longitude = 79.8302
            severity = "HIGH"
            originalLanguage = "EN"
        } | ConvertTo-Json
        $base1 = Invoke-RestMethod -Uri "$backendUrl/api/incidents" -Method Post -Headers $headers -Body $body1
        Write-Host "[+] Seeded Baseline Incident 1: ID $($base1.id)" -ForegroundColor Green
    } else {
        Write-Host "[OK] Baseline Incident 1 active: ID $($base1.id) ($($base1.title))" -ForegroundColor Green
    }

    # Check for Baseline Report 2
    $base2 = $allIncidents | Where-Object { $_.title -like "*Waterlogging at Culvert Junction*" } | Select-Object -First 1
    if (-not $base2) {
        $body2 = @{
            reporterId = $citizenId
            title = "[DEMO] Waterlogging at Culvert Junction"
            category = "DRAINAGE"
            description = "Stagnant stormwater overflowing onto walkway near Heritage Blvd."
            address = "Heritage Boulevard West, Ward 5, Puducherry"
            latitude = 11.9346
            longitude = 79.8296
            severity = "HIGH"
            originalLanguage = "EN"
        } | ConvertTo-Json
        $base2 = Invoke-RestMethod -Uri "$backendUrl/api/incidents" -Method Post -Headers $headers -Body $body2
        Write-Host "[+] Seeded Baseline Incident 2: ID $($base2.id)" -ForegroundColor Green
    } else {
        Write-Host "[OK] Baseline Incident 2 active: ID $($base2.id) ($($base2.title))" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  READY FOR CLEAN TAKE!" -ForegroundColor Green
    Write-Host "  1. Open http://localhost:5173/login" -ForegroundColor White
    Write-Host "  2. 1-Click 'Citizen' quick-fill -> Sign In" -ForegroundColor White
    Write-Host "  3. Go to /citizen/report, toggle Tamil (TA)" -ForegroundColor White
    Write-Host "  4. Submit Heritage Boulevard drainage issue with photo" -ForegroundColor White
    Write-Host "  5. Next incident will corroborate with Baseline IDs and show +2 cluster and 70.0 (HIGH) priority score in Officer Queue!" -ForegroundColor White
    Write-Host "==========================================================" -ForegroundColor Cyan

} catch {
    Write-Host "[ERROR] Failed resetting demo state: $_" -ForegroundColor Red
}
