$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    basicInfo = @{
        name = "test"
        startAge = 25
        location = "City"
        educationLevel = "Bachelor"
        profession = "Coder"
    }
    economicStatus = @{
        savings = 50000
        debt = 0
    }
    healthStatus = @{
        energyLevel = 80
    }
    familyBackground = @{
        parentsStatus = "Parents Alive"
    }
} | ConvertTo-Json -Depth 5

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/sim/init" -Method Post -Headers $headers -Body $body
    Write-Host "Success!"
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}
