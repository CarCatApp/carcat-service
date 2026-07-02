# Carland × Hyper Partner Webhook API

**Version:** 1.0  
**Base URL:** `https://digital-innovation.agency`  
**Audience:** Hyper integration team  
**Last updated:** 2026-06-30

---

## 1. Overview

Hyper sends service-visit data to Carland through three partner endpoints. All traffic goes through the public webhook gateway; Carland is not called directly.

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/webhook/partner/car/find` | Check whether a VIN exists in Carland |
| `POST` | `/webhook/partner/new-service-visit` | Create a new service visit (idempotent) |
| `PUT` | `/webhook/partner/edit/service-visit` | Update an existing visit or its lines/parts |

---

## 2. Authentication — `X-Signature`

Every request under `/webhook/partner/*` must include:

| Header | Value |
|--------|--------|
| `X-Signature` | HMAC-SHA256 hex digest (lowercase, no prefix) |
| `Content-Type` | `application/json` (POST and PUT only) |

**Shared secret:** Per-partner HMAC secret stored in Carland (`partners.webhook_secret`). Set via admin API or SQL when onboarding a partner.

**Validation (carland-service, two layers):**
1. **Service trust** — `X-Internal-Token` (webhook-service → carland only; Hyper never sends this).
2. **Partner trust** — `X-Signature` verified with that partner's `webhook_secret` from DB, using `partnerId` from the request.

The webhook gateway forwards the partner's raw body/query and original `X-Signature` unchanged; it does not validate or re-sign.

### What gets signed

| Request type | Bytes signed |
|--------------|--------------|
| `GET` (query only) | Raw UTF-8 query string **exactly as sent**, e.g. `partnerId=1&vin=HHGHHHJHGHHHHHGGG` |
| `POST` / `PUT` | Raw HTTP body bytes **exactly as sent** |

Algorithm:

```
signature = HMAC-SHA256(secret, payload).hex()
```

### Important rules

1. Sign the **exact bytes** on the wire — not a re-serialized JSON object.
2. Pretty-print vs minified JSON produces **different signatures**.
3. For GET, URL-encode the VIN if it contains special characters; sign the encoded query string.
4. Missing or invalid signature → **401** from carland-service (`{"error":"Invalid or missing signature"}`).

**401 example:**

```json
{"error":"Invalid or missing signature"}
```

---

## 3. JSON body — single-line (compact) format

For **POST** and **PUT**, send JSON as **one compact line** (no line breaks, no extra spaces).

**Correct:**

```json
{"vin":"HHGHHHJHGHHHHHGGG","source":"hyper","items":[{"partnerRecordId":33333,"type":"Maintenance","date":"2026-06-15","mileage":52000,"services":[{"serviceCode":55555,"serviceName":"Engine oil & filter","cost":{"amount":85.00,"currency":"AZN"}}]}]}
```

**Incorrect** (different byte length → signature mismatch → 401):

```json
{
  "vin": "HHGHHHJHGHHHHHGGG",
  "source": "hyper"
}
```

**Why:** HMAC is computed on raw body bytes. Whitespace changes the signature even when JSON is semantically identical.

**Recommendation:** Serialize programmatically to compact JSON, then sign those bytes. Do not manually pretty-print in Postman unless the Pre-request Script signs `pm.request.body.raw` exactly.

---

## 4. Common error response format

Business/validation errors from Carland (forwarded by webhook):

```json
{
  "error": "Missed required fields",
  "message": "vin is required",
  "timeStamp": "2026-06-30T14:22:10.123",
  "status": 400
}
```

| Field | Description |
|-------|-------------|
| `error` | Error category |
| `message` | Human-readable detail |
| `timeStamp` | Server time (ISO-8601) |
| `status` | HTTP status code (duplicate of response status) |

---

## 5. GET — Check VIN existence

### Request

```
GET /webhook/partner/car/find?vin=HHGHHHJHGHHHHHGGG
```

| Item | Value |
|------|--------|
| Query param | `vin` (required, string) |
| Headers | `X-Signature: <hmac of "vin=HHGHHHJHGHHHHHGGG">` |
| Body | None |

### Responses

| Status | Meaning | Body |
|--------|---------|------|
| **200 OK** | VIN registered in Carland | Empty |
| **404 Not Found** | VIN missing or blank | Empty |
| **400 Bad Request** | `vin` query param missing | Error JSON |
| **401 Unauthorized** | Invalid/missing signature | `{"error":"Invalid or missing signature"}` |
| **502 / 503 / 504** | Carland temporarily unavailable | Gateway/upstream error (no queue for GET) |

### Example — VIN found

```http
GET /webhook/partner/car/find?vin=HHGHHHJHGHHHHHGGG
X-Signature: a1b2c3d4e5f6...
```

```http
HTTP/1.1 200 OK
Content-Length: 0
```

### Example — VIN not found

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

---

## 6. POST — Create service visit

### Request

```
POST /webhook/partner/new-service-visit
Content-Type: application/json
X-Signature: <hmac of raw body bytes>
```

### Body schema

Hyper native vehicle payload — same shape as Hyper’s VIN history API and what Carland uses internally for `service-history/v2` ingest.

Root object:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `partnerId` | long | **Yes** | Carland partner id (must exist and be active in `partners` table; Hyper = `1`) |
| `vin` | string | **Yes** | Vehicle VIN (must exist in Carland) |
| `plate` | string | **Yes** | License plate; updates car |
| `brand` | string | No | Car brand |
| `model` | string | No | Car model |
| `year` | integer | No | Model year |
| `engineVolume` | number | No | Engine volume in litres (e.g. `1.5`) |
| `engineType` | string | No | Engine type |
| `bodyType` | string | No | Body type |
| `trim` | string | No | Trim level |
| `currentMileage` | integer | No | Current odometer; updates car if sent |
| `serviceHistory` | array | **Yes** | **Exactly one** visit record (real-time webhook; not bulk history) |

Each element in `serviceHistory[]`:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `recordId` | long | **Yes** | Hyper's unique visit ID (idempotency key) |
| `serviceType` | string | No | e.g. `"Mühərrik xidməti"` |
| `serviceGroups` | string[] | No | Visit-level service groups |
| `lastServiceDate` | string (date) | **Yes** | `YYYY-MM-DD` |
| `lastServiceMileage` | integer | **Yes** | Odometer at service |
| `dealer` | string | No | Dealer name |
| `invoiceNumber` | string | No | Invoice reference |
| `cost` | money | No | Pre-discount visit cost |
| `finalCost` | money | No | Final visit cost (falls back to `cost` if omitted) |
| `nextServiceDate` | string (date) | No | Visit-level next due date (informational) |
| `nextServiceMileage` | integer | No | Visit-level next due mileage (informational) |
| `services` | array | **Yes** | At least one service line |
| `parts` | array | No | Parts used |

`services[]` line:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serviceCode` | integer | **Yes** | Hyper line identifier (stored as partner line key) |
| `serviceName` | string | No | Line description |
| `serviceGroups` | string[] | No | Line-level service groups |
| `universalServiceId` | string | **Yes** | Universal service catalog ID (`"other"` → stored as empty) |
| `cost` | money | No | Line cost |
| `nextServiceDate` | string (date) | No | Next due date (used for percentage sync) |
| `nextServiceMileage` | integer | No | Next due mileage (used for percentage sync) |

`parts[]`:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Part name |
| `qty` | decimal | No | Quantity |
| `unit` | string | No | e.g. `"pc"`, `"L"` |

`money` object:

```json
{"amount": 85.00, "currency": "AZN"}
```

### Idempotency behavior

| Scenario | HTTP | Notes |
|----------|------|-------|
| New visit created | **200** | `visitsCreated > 0` |
| Visit exists; new lines/parts added | **200** | `linesCreated > 0` or `partsCreated > 0` |
| Visit + all lines/parts already exist | **409** | No new data |
| VIN not in Carland | **404** | |
| Missing required field (`partnerId`, `vin`, `plate`, `serviceHistory`, `recordId`, `lastServiceDate`, `lastServiceMileage`, `services`, `serviceCode`, `universalServiceId`) | **400** | Message: `{fieldName} is required` |
| Unknown or inactive `partnerId` | **404** | `Partner not found for partnerId: ...` |
| `serviceHistory` length ≠ 1 | **400** | `serviceHistory must contain exactly one visit` |
| Invalid JSON | **400** | |
| Invalid signature | **401** | |
| Carland down | **202 Accepted** | Queued for retry (see §8) |

Items **without** `recordId` are rejected with **400** (`recordId is required`).

Duplicate line match key: `serviceCode + universalServiceId + serviceName`.  
Duplicate part match key: `name + qty + unit`.

### Example request body

```json
{
    "plate": "99-FH-032",
    "vin": "3FA6P0HDXKR168752",
    "brand": "Ford",
    "model": "Fusion",
    "year": 2019,
    "engineVolume": 1.5,
    "currentMileage": 121000,
    "serviceHistory": [
        {
            "recordId": 19387,
            "serviceType": "Mühərrik xidməti",
            "serviceGroups": ["Mühərrik xidməti", "Salon xidməti"],
            "lastServiceDate": "2026-05-25",
            "lastServiceMileage": 121000,
            "services": [
                {
                    "serviceCode": 7,
                    "serviceName": "EXTRA Mühərrik yağının dəyişdirilməsi",
                    "serviceGroups": ["Mühərrik xidməti"],
                    "universalServiceId": "Engine oil & filter",
                    "cost": {"amount": 0, "currency": "AZN"}
                }
            ],
            "parts": [{"name": "5W-20", "qty": 0.8}],
            "cost": {"amount": 47.4, "currency": "AZN"},
            "finalCost": {"amount": 47.4, "currency": "AZN"},
            "dealer": "Babək Ekspress"
        }
    ]
}
```

### Example — 200 Created

```json
{
  "vin": "HHGHHHJHGHHHHHGGG",
  "message": "Visit and service lines created",
  "visitsCreated": 1,
  "visitsSkipped": 0,
  "linesCreated": 1,
  "linesSkipped": 0,
  "partsCreated": 1,
  "partsSkipped": 0,
  "visits": [
    {
      "partnerRecordId": 33333,
      "visitId": 9876,
      "visitCreated": true,
      "lines": [
        {"serviceCode": 55555, "lineId": 4412, "created": true}
      ]
    }
  ]
}
```

### Example — 200 Partial append (visit existed, new line added)

```json
{
  "vin": "HHGHHHJHGHHHHHGGG",
  "message": "Visit updated with new service lines or parts",
  "visitsCreated": 0,
  "visitsSkipped": 1,
  "linesCreated": 1,
  "linesSkipped": 0,
  "partsCreated": 0,
  "partsSkipped": 0,
  "visits": [
    {
      "partnerRecordId": 33333,
      "visitId": 9876,
      "visitCreated": false,
      "lines": [
        {"serviceCode": 55556, "lineId": 4413, "created": true}
      ]
    }
  ]
}
```

### Example — 409 Already exists

```json
{
  "vin": "HHGHHHJHGHHHHHGGG",
  "message": "Visit and service lines already exist",
  "visitsCreated": 0,
  "visitsSkipped": 1,
  "linesCreated": 0,
  "linesSkipped": 1,
  "partsCreated": 0,
  "partsSkipped": 1,
  "visits": [
    {
      "partnerRecordId": 33333,
      "visitId": 9876,
      "visitCreated": false,
      "lines": [
        {"serviceCode": 55555, "lineId": 4412, "created": false}
      ]
    }
  ]
}
```

### Example — 404 VIN not found

```json
{
  "error": "Resource not found error",
  "message": "Car not found for vin: UNKNOWNVIN123",
  "timeStamp": "2026-06-30T14:22:10.123",
  "status": 404
}
```

### Example — 400 Missing field

```json
{
  "error": "Missed required fields",
  "message": "items is required",
  "timeStamp": "2026-06-30T14:22:10.123",
  "status": 400
}
```

---

## 7. PUT — Update service visit

### Request

```
PUT /webhook/partner/edit/service-visit
Content-Type: application/json
X-Signature: <hmac of raw body bytes>
```

### Body schema

**Same JSON shape as POST** (§6). Hyper sends the corrected full visit snapshot.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `vin` | string | **Yes** | Vehicle VIN |
| `plate` | string | **Yes** | License plate |
| `serviceHistory` | array | **Yes** | **Exactly one** visit — the visit being corrected |
| _(vehicle fields)_ | | No | Same as POST (`brand`, `model`, `currentMileage`, …) |
| _(visit fields)_ | | | Same as POST (`recordId`, `lastServiceDate`, `lastServiceMileage`, `services`, …) |

`recordId` must already exist for this VIN (created via POST). Otherwise **404**.

### Update semantics

- Visit-level fields from the payload **overwrite** the stored visit.
- Service lines matched by `serviceCode`: updated if present, **added** if new.
- Parts matched by `name + qty + unit`: updated if present, **added** if new.
- Lines/parts not included in the payload are **left unchanged** (not deleted).
- Car metadata (`plate`, `brand`, `currentMileage`, …) updated like POST.
- Percentages refreshed only when this visit is at least as recent as all other visits on the car.

### Responses

| Status | Meaning |
|--------|---------|
| **200 OK** | At least one field/line/part changed |
| **409 Conflict** | Visit found but payload matches current data (no changes) |
| **404 Not Found** | VIN or `recordId` not found |
| **400 Bad Request** | Validation error (same rules as POST) |
| **401 Unauthorized** | Invalid signature |
| **202 Accepted** | Carland down; request queued (§8) |

### Example request body

```json
{
    "plate": "99-FH-032",
    "vin": "3FA6P0HDXKR168752",
    "currentMileage": 121000,
    "serviceHistory": [
        {
            "recordId": 19387,
            "serviceType": "Mühərrik xidməti",
            "lastServiceDate": "2026-05-25",
            "lastServiceMileage": 121000,
            "services": [
                {
                    "serviceCode": 7,
                    "serviceName": "EXTRA Mühərrik yağının dəyişdirilməsi",
                    "universalServiceId": "Engine oil & filter",
                    "cost": {"amount": 50.0, "currency": "AZN"},
                    "nextServiceDate": "2027-05-25",
                    "nextServiceMileage": 141000
                }
            ],
            "finalCost": {"amount": 50.0, "currency": "AZN"},
            "dealer": "Babək Ekspress"
        }
    ]
}
```

### Example — 200 Updated

```json
{
  "vin": "3FA6P0HDXKR168752",
  "message": "Visit and service lines updated",
  "partnerRecordId": 19387,
  "visitId": 9876,
  "visitFieldsUpdated": 1,
  "linesUpdated": 1,
  "partsUpdated": 0,
  "lines": [
    {"serviceCode": 7, "lineId": 4412, "updated": true}
  ],
  "parts": []
}
```

### Example — 409 No changes

```json
{
  "vin": "3FA6P0HDXKR168752",
  "message": "Visit and service lines already up to date",
  "partnerRecordId": 19387,
  "visitId": 9876,
  "visitFieldsUpdated": 0,
  "linesUpdated": 0,
  "partsUpdated": 0,
  "lines": [
    {"serviceCode": 7, "lineId": 4412, "updated": false}
  ],
  "parts": []
}
```

### Example — 404 Visit not found

```json
{
  "error": "Resource not found error",
  "message": "Visit not found for recordId=99999 and vin=3FA6P0HDXKR168752",
  "timeStamp": "2026-06-30T14:22:10.123",
  "status": 404
}
```

### Example — 400 Validation

```json
{
  "error": "Missed required fields",
  "message": "serviceHistory must contain exactly one visit",
  "timeStamp": "2026-06-30T14:22:10.123",
  "status": 400
}
```

---

## 8. Async queue (POST & PUT only)

When Carland is temporarily unreachable, POST and PUT are **accepted and queued** instead of failing immediately.

| Status | Body |
|--------|------|
| **202 Accepted** | `{"status":"queued","message":"Carland unavailable, request queued for delivery"}` |

- Queued requests are replayed automatically when Carland recovers.
- **GET** is not queued; use retry on 502/503/504.
- On successful replay, Carland returns the normal 200/409/404 response.
- **409** on replay means the visit was already ingested — treat as success for idempotency.

---

## 9. Status code summary

| Code | When |
|------|------|
| **200** | Success (GET: VIN exists; POST: created/updated; PUT: changes applied) |
| **202** | POST/PUT queued (Carland down) |
| **400** | Invalid/missing fields, malformed JSON |
| **401** | Invalid or missing `X-Signature` |
| **404** | VIN, visit, line, or part not found |
| **409** | POST/PUT idempotent no-op (data already exists / unchanged) |
| **502/503/504** | Upstream unavailable (GET — retry) |

---

## 10. Integration checklist for Hyper

- [ ] Store partner webhook secret securely (Carland `partners.webhook_secret`; share out-of-band with partner only)
- [ ] Always serialize JSON to **single compact line** before signing
- [ ] Sign **raw body bytes** for POST/PUT; sign **query string** for GET
- [ ] Use stable `partnerRecordId` per visit for idempotency
- [ ] Treat **409** on POST/PUT as success when data is already present
- [ ] On **202**, optionally poll GET `/car/find` or retry POST later
- [ ] Use `serviceCode` to target specific lines on PUT
- [ ] Ensure VIN is registered in Carland before POST

---

## 11. cURL examples

### GET — VIN check

```bash
VIN="HHGHHHJHGHHHHHGGG"
QUERY="vin=${VIN}"
SIG=$(printf '%s' "$QUERY" | openssl dgst -sha256 -hmac "$HYPER_WEBHOOK_SECRET" | awk '{print $2}')

curl -s -o /dev/null -w "%{http_code}" \
  "https://digital-innovation.agency/webhook/partner/car/find?${QUERY}" \
  -H "X-Signature: ${SIG}"
```

### POST — new visit

```bash
BODY='{"vin":"3FA6P0HDXKR168752","currentMileage":121000,"serviceHistory":[{"recordId":19387,"serviceType":"Mühərrik xidməti","lastServiceDate":"2026-05-25","lastServiceMileage":121000,"services":[{"serviceCode":7,"serviceName":"Engine oil change","universalServiceId":"Engine oil & filter","cost":{"amount":47.4,"currency":"AZN"}}],"finalCost":{"amount":47.4,"currency":"AZN"},"dealer":"Babək Ekspress"}]}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$HYPER_WEBHOOK_SECRET" | awk '{print $2}')

curl -X POST "https://digital-innovation.agency/webhook/partner/new-service-visit" \
  -H "Content-Type: application/json" \
  -H "X-Signature: ${SIG}" \
  --data-binary "$BODY"
```

### PUT — update visit

```bash
BODY='{"plate":"99-FH-032","vin":"3FA6P0HDXKR168752","currentMileage":121000,"serviceHistory":[{"recordId":19387,"lastServiceDate":"2026-05-25","lastServiceMileage":121000,"services":[{"serviceCode":7,"serviceName":"Engine oil change","universalServiceId":"Engine oil & filter","cost":{"amount":50.0,"currency":"AZN"},"nextServiceMileage":141000}],"finalCost":{"amount":50.0,"currency":"AZN"},"dealer":"Babək Ekspress"}]}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$HYPER_WEBHOOK_SECRET" | awk '{print $2}')

curl -X PUT "https://digital-innovation.agency/webhook/partner/edit/service-visit" \
  -H "Content-Type: application/json" \
  -H "X-Signature: ${SIG}" \
  --data-binary "$BODY"
```
