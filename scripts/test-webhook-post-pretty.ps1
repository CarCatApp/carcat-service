$bodyPath = Join-Path $env:TEMP "webhook-body-pretty.json"
$secret = "Y2FybGFuZC1zZWNyZXQta2V5LXNoYXJlZC13aXRoLWh5cGVy"

$body = @'
{
    "partnerId": 1,
    "plate": "55-BB-666",
    "vin": "HHGHHHJHGHHHHHGGG",
    "brand": "Ford",
    "model": "Fusion",
    "year": 2019,
    "engineVolume": 1.5,
    "engineType": "Benzin",
    "bodyType": "Sedan",
    "trim": "Titanium",
    "currentMileage": 121000,
    "serviceHistory": [
        {
            "recordId": 19387,
            "serviceType": "Mühərrik xidməti",
            "serviceGroups": [
                "Mühərrik xidməti",
                "Salon xidməti"
            ],
            "lastServiceDate": "2026-05-25",
            "lastServiceMileage": 121000,
            "services": [
                {
                    "serviceCode": 7,
                    "serviceName": "EXTRA Mühərrik yağının dəyişdirilməsi",
                    "serviceGroups": [
                        "Mühərrik xidməti"
                    ],
                    "universalServiceId": "Engine oil & filter",
                    "cost": {
                        "amount": 35.0,
                        "currency": "AZN"
                    },
                    "nextServiceDate": "2027-05-25",
                    "nextServiceMileage": 141000
                },
                {
                    "serviceCode": 12,
                    "serviceName": "Hava filtri dəyişdirilməsi",
                    "serviceGroups": [
                        "Mühərrik xidməti"
                    ],
                    "universalServiceId": "Air filter",
                    "cost": {
                        "amount": 12.4,
                        "currency": "AZN"
                    },
                    "nextServiceDate": "2027-05-25",
                    "nextServiceMileage": 141000
                }
            ],
            "parts": [
                {
                    "name": "5W-20",
                    "qty": 0.8,
                    "unit": "L"
                },
                {
                    "name": "Yağ filtri",
                    "qty": 1,
                    "unit": "pc"
                }
            ],
            "cost": {
                "amount": 47.4,
                "currency": "AZN"
            },
            "finalCost": {
                "amount": 47.4,
                "currency": "AZN"
            },
            "nextServiceDate": "2027-05-25",
            "nextServiceMileage": 141000,
            "invoiceNumber": "INV-2026-0019387",
            "dealer": "Babək Ekspress"
        }
    ]
}
'@

[System.IO.File]::WriteAllText($bodyPath, $body, [System.Text.UTF8Encoding]::new($false))

$bodyBytes = [System.IO.File]::ReadAllBytes($bodyPath)
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
$sig = -join ($hmac.ComputeHash($bodyBytes) | ForEach-Object { $_.ToString("x2") })

Write-Host "Body file : $bodyPath"
Write-Host "Body bytes: $($bodyBytes.Length)"
Write-Host "Signature : $sig"
Write-Host ""

curl.exe -v -X POST "https://digital-innovation.agency/webhook/partner/new-service-visit" `
  -H "Content-Type: application/json" `
  -H "X-Signature: $sig" `
  --data-binary "@$bodyPath"
