# Sign and POST partner webhook — body bytes must match wire exactly.
# Set $secret to partners.webhook_secret for partnerId=1 (NOT CARLAND_INTERNAL_TOKEN).

$bodyPath = Join-Path $env:TEMP "webhook-body.json"
$secret = "plLEKxrF4wRHICWyW1PenoxF6hGCcVwWIRrwpSzo"  # DB: SELECT webhook_secret FROM partners WHERE id=1

$body = '{"partnerId":1,"plate":"55-BB-666","vin":"HHGHHHJHGHHHHHGGG","brand":"Ford","model":"Fusion","year":2019,"engineVolume":1.5,"engineType":"Benzin","bodyType":"Sedan","trim":"Titanium","currentMileage":121000,"serviceHistory":[{"recordId":19388,"serviceType":"Mühərrik xidməti","serviceGroups":["Mühərrik xidməti"],"lastServiceDate":"2026-05-25","lastServiceMileage":121000,"services":[{"serviceCode":8,"serviceName":"EXTRA Mühərrik yağının dəyişdirilməsi","serviceGroups":["Mühərrik xidməti"],"universalServiceId":"Engine oil & filter","cost":{"amount":35.0,"currency":"AZN"},"nextServiceDate":"2027-05-25","nextServiceMileage":141000}],"parts":[{"name":"5W-20","qty":0.8,"unit":"L"}],"cost":{"amount":35.0,"currency":"AZN"},"finalCost":{"amount":35.0,"currency":"AZN"},"nextServiceDate":"2027-05-25","nextServiceMileage":141000,"invoiceNumber":"INV-2026-0019388","dealer":"Babək Ekspress"}]}'

[System.IO.File]::WriteAllText($bodyPath, $body, [System.Text.UTF8Encoding]::new($false))

$bodyBytes = [System.IO.File]::ReadAllBytes($bodyPath)
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$sig = -join ($hmac.ComputeHash($bodyBytes) | ForEach-Object { $_.ToString("x2") })

$sha = [System.Security.Cryptography.SHA256]::Create()
$shaHex = -join ($sha.ComputeHash($bodyBytes) | ForEach-Object { $_.ToString("x2") })
$shaPrefix = $shaHex.Substring(0, 16)

Write-Host "Body file : $bodyPath"
Write-Host "Body bytes: $($bodyBytes.Length)"
Write-Host "SHA256 prefix (compare with carland log): $shaPrefix"
Write-Host "Signature : $sig"
Write-Host ""

curl.exe -v -X POST "https://digital-innovation.agency/webhook/partner/new-service-visit" `
  -H "Content-Type: application/json" `
  -H "X-Signature: $sig" `
  --data-binary "@$bodyPath"
