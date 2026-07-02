# Partner Webhook Security Proposal — PO Brief

**Document type:** Product & security proposal  
**Project:** Carland Service  
**Audience:** Product Owner / stakeholders  
**Status:** Proposed approach (for alignment before implementation)

---

## 1. Executive summary

We propose securing multi-partner service-visit webhooks using **partner onboarding controlled by Carland**, **one secret key per partner**, and **HMAC signature over the full JSON body** (including a Carland-assigned `partnerId`).

This model is **appropriate for our data sensitivity** (vehicle service history — useful but not highly classified). It matches common B2B webhook practice and is **stronger than** a single shared secret or IP-only identification.

**Recommendation:** Approve this as the standard integration security model for Hyper and future partners (e.g. Toyota Absheron).

---

## 2. Problem we are solving

| Today | Risk |
|-------|------|
| Single `WEBHOOK_SECRET` for all partners | One leak affects everyone |
| Partner identity not bound to authentication | Hard to scale to multiple partners safely |
| Visit idempotency key = `car + recordId` only | ID collision across partners (future risk) |

We need a model that scales to **many partners**, allows **secret rotation**, and gives Carland **control over who can send data**.

---

## 3. Proposed model (overview)

```text
1. Carland registers partner (manual / API / future admin UI)
2. Carland assigns: partnerId + secret key (+ optional metadata)
3. Partner sends POST/PUT with:
      - JSON body including partnerId
      - X-Signature = HMAC-SHA256(secret, raw body bytes)
4. Webhook gateway verifies signature with THAT partner's secret
5. Carland processes visit if partner is active and payload is valid
```

**Trust rule:** We do **not** trust `partnerId` in the body alone. We trust it **only after** the signature verifies with that partner's secret.

---

## 4. Partner onboarding

Partners are created **only on our side**:

| Phase | Method |
|-------|--------|
| Now | Manual DB insert or internal Postman/admin API |
| Next | Admin UI — create partner, view status, rotate secret |

Each partner record includes at minimum:

- `partnerId` (stable Carland id — sent in webhook body)
- `name`, `source` / slug (e.g. `hyper`, `toyota-absheron`)
- `active` flag
- `secret` (stored securely; never returned in full after creation)

Hyper and future partners receive **`partnerId` + `secret`** through a secure channel (not email in plain text if avoidable).

---

## 5. Secret key management

### 5.1 Storage

| Stage | Where |
|-------|--------|
| Initial (few partners) | `.env` / server secrets — e.g. `HYPER_WEBHOOK_SECRET` for Hyper; later `TOYOTA_WEBHOOK_SECRET`, etc. |
| Scale (many partners) | Database (encrypted) + admin UI — rotate without redeploy |

### 5.2 Rotation policy

| Trigger | Action |
|---------|--------|
| Secret lost or suspected compromise | Issue new secret immediately; disable old after grace period |
| Planned policy (optional) | Rotate every week or month per partner agreement |

**Best practice:** During rotation, accept **both old and new secret** for 7–14 days so partners can update without downtime.

---

## 6. Request format & verification

### 6.1 Body

Same canonical JSON as today (single visit in `serviceHistory`), plus:

```json
{
  "partnerId": 1,
  "plate": "...",
  "vin": "...",
  "serviceHistory": [ { "recordId": ..., "services": [...] } ]
}
```

`partnerId` must match the partner whose secret signed the request.

### 6.2 Signature

- Header: `X-Signature`
- Algorithm: **HMAC-SHA256** over **exact raw HTTP body bytes** (compact JSON; whitespace changes break verification)
- Verification at **webhook gateway** (internet edge), before Carland internal processing

### 6.3 Processing flow

```text
Partner → nginx → webhook-service
                    ├─ Verify X-Signature (partner secret)
                    ├─ Validate partnerId active in registry
                    └─ Forward to carland-service (internal token)
                              └─ Business validation + ingest/update
```

---

## 7. Security assessment (PO-friendly)

### 7.1 What this protects

| Threat | Mitigated? |
|--------|------------|
| Random attackers sending fake visits | Yes — no valid signature → 401 |
| Body tampering in transit | Yes — signature fails if bytes change |
| One partner's secret leak | Partially — other partners unaffected |
| Wrong partner impersonation | Yes — without victim's secret, cannot sign |

### 7.2 Residual risks (accepted for our use case)

| Threat | Notes |
|--------|--------|
| Replay of same signed request | Mitigated by idempotent `recordId`; not financial-grade replay window needed |
| Stolen secret | Attacker can send valid requests as that partner → **rotation + monitoring** |
| DDoS | Separate concern (rate limits / nginx); auth does not stop volume attacks |
| Malicious insider at partner | Inherent B2B trust boundary |

**Conclusion:** For **service history data**, this is **industry-standard and sufficient**. We do not need IP-only identification or heavier schemes unless compliance requirements change.

### 7.3 Optional add-on (not required for v1)

- IP allowlist per partner at nginx — extra layer, not replacement for HMAC
- Useful when partner can provide stable egress IPs

---

## 8. Comparison with alternatives

| Approach | Partner scale | Rotation | Security | Ops effort |
|----------|---------------|----------|----------|------------|
| Single shared secret | Poor | All partners at once | Low | Low |
| IP whitelist only | Medium | N/A | Medium (fragile) | High (IP changes) |
| **Per-partner secret + HMAC (proposed)** | **Good** | **Per partner** | **Good** | **Medium** |
| mTLS per partner | Good | Medium | Very high | High |

**Proposed approach** balances security, cost, and time-to-market.

---

## 9. Product decisions needed from PO

| # | Decision | Options | Proposal |
|---|----------|---------|----------|
| 1 | Approve per-partner HMAC model | Yes / No | **Yes** |
| 2 | `partnerId` in webhook body | Required field | **Required** |
| 3 | Secret storage v1 | `.env` vs DB | **`.env` for first 2–5 partners**, then admin UI + DB |
| 4 | Mandatory rotation schedule | None / monthly / quarterly | **Optional policy; mandatory on compromise** |
| 5 | IP allowlist | Required / optional / no | **Optional add-on per partner** |
| 6 | Admin UI priority | Now / later | **Later** (manual/API sufficient for Hyper + 1–2 pilots) |

---

## 10. Implementation phases (high level)

| Phase | Deliverable | Partner impact |
|-------|-------------|----------------|
| **1** | Partner registry + `partnerId` in payload spec | Hyper adds `partnerId` to body |
| **2** | Per-partner secret in env + gateway verification | Hyper gets dedicated secret |
| **3** | DB unique key `(car_id, partner_id, record_id)` | None (internal) |
| **4** | Secret rotation procedure + dual-secret grace window | Partners update secret on schedule |
| **5** | Admin UI for partner + secret management | Self-service onboarding |

---

## 11. Success criteria

- [ ] Each active partner has unique `partnerId` and secret known only to Carland and that partner  
- [ ] Requests without valid signature are rejected (401)  
- [ ] Requests with valid signature but unknown/inactive `partnerId` are rejected (400/403)  
- [ ] Compromised partner secret can be rotated without affecting other partners  
- [ ] PO and integration docs describe onboarding and rotation in one page for partners  

---

## 12. Summary for PO sign-off

**We recommend:** Carland-owned partner registry, **one secret per partner**, partners include **`partnerId` in the signed JSON body**, verification via **HMAC on raw body** at the webhook gateway.

This is **simple for partners**, **scalable for us**, and **adequate security** for service-history webhook data. IP whitelisting remains optional, not core.

---

*Carland Service — Partner webhook security proposal for Product Owner review.*
