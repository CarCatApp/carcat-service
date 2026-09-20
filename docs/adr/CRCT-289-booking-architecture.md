# ADR: CRCT-289 Booking architecture

Status: Accepted (spike close-out)  
Date: 2026-09-20  
Ticket: [CRCT-289](https://nemat-mirzayev.atlassian.net/browse/CRCT-289)  
Parent: CRCT-279  
Next: CRCT-280 (tables — no Java in this spike)

Jira: Aziz **Devam Ediyor**’a alır. Agent Jira’ya gitmez.

This spike produces decisions, not endpoints. Flutter / partner-panel UI is out of scope.

---

## Context

PO Sprint-6 tickets (CRCT-280…287) describe UUID `partner` / `branch` / `slot`, Hyper Admin Panel (Group A) as calendar producer, and `branch = autoService`.

CarCat already has:

- `partners` + `branches` (bigint identity, Hyper integration + booking HQ)
- `booking_staff` (PARTNER_ADMIN / BRANCH_ADMIN, one-time password)
- Operator panel (`+994500000000`) — not a SuperAdmin table
- `calendars` / `ranges` / `appointments` keyed by `branch_id`
- `ratings`, `device_tokens`, Hyper visit webhook (separate from booking)

Rewriting identity tables would undo the org work. The spike maps PO contracts onto what exists.

---

## Decision

### 1. CarCat is the only orchestrator

Owner-app JWT only. Flutter never calls Hyper Group A. Calendar/slots are written by CarCat staff (or our operator panel), not by a Hyper admin calendar feed in this slice.

### 2. Keep bigint Partner / Branch

No UUID rewrite. API fields `partnerId` / `branchId` are the existing Long ids (JSON number). `APP_USER` in the PO ERD is `customers` / auth `users` — do not recreate.

### 3. Slot = `ranges`

`slotId` in CRCT-283/284 = `ranges.range_id`.  
`capacity` = `worker_count`.  
`booked_count` = number of active bookings on that range (computed or column added in 280).  
`calendars` = day + optional service filter shell.  
Do **not** add a parallel `slot` table.

### 4. Catalog is new (280)

There is no package/service/tier/brand table today. Add them on `branch_id` (Long FK). Keys: `pkg:…`, `svc:…`, `dir:repair|inspection|trade`.

### 5. New `booking` + `booking_item`

`appointments` is one `serviceCategory` per row. PO needs many `serviceKeys`, public `ref` (`CC-######`), price min/max in qəpik. Do not inflate `appointments`. Leave it unused for the owner-app flow.

### 6. Money, time, errors, flag

Integer minor units (qəpik), `currency: AZN`. Timestamps Asia/Baku (`+04:00`). Errors `{ "error", "message", "details" }`. Feature flag `booking` on owner-app booking routes.

### 7. Chat and push

Chat lives in CarCat (`booking_message`). There is no Lovable Cloud store to migrate.  
Push reuses `device_tokens` (CRCT-287 may later allow multiple devices per user; today `user_id` is unique).

### 8. Reviews

`ratings` already belongs to `branches`. Do not create a second `review` table in 280 unless PO fields cannot fit. Average calc stays later (existing PO note).

---

## Booking mode (staff-set)

Staff chooses mode **when creating the slot (range)**:

| Mode | After customer `POST /bookings` | Staff | Customer |
|------|----------------------------------|-------|----------|
| `instant` | `confirmed` | — | can cancel (285 rules) |
| `approval` | `pending` | accept or reject | can cancel |

Both values live on the range. Not a global partner switch.

Accept/reject is **CarCat** (staff API / panel). No Hyper callback and no CarCat poll of Hyper.

---

## Six PO questions

| # | Question | Lock |
|---|----------|------|
| 1 | Accept/reject: callback vs poll? | Neither toward Hyper. Status changes in CarCat. |
| 2 | Bundle: one range or several? | **One range / one `slotId`**. Many `serviceKeys` as `booking_item` rows. |
| 3 | Pending timeout? | Optional column in 280. No invented TTL until PO gives hours. |
| 4 | Cancel cut-off? | 285. Until PO gives hours: block cancel when completed or start is past. |
| 5 | `serviceCategory` 1:1? | Legacy calendar field. Owner-app taxonomy is `serviceKey`. Range: `*` or one key; availability filter is CSV. |
| 6 | `universalServiceId` (23 + other)? | Hyper **visit** webhook line ids. Booking catalog uses `serviceKey`. 280 is not blocked. |

---

## What we reuse vs ignore

Reuse:

- Partner / Branch / BookingStaff / panel login
- Calendar + Range + `branch_id`
- Customer (`customers.user_id` = JWT `X-User-Id`)
- `device_tokens`, existing push send path
- `ratings` for branch reviews
- Webhook HMAC for **visits** (not for owner booking)
- Feature-flag machinery (new flag name `booking`)

Do not reuse as-is:

- `appointments` as the owner-app booking aggregate
- PO UUID entity ids
- Group A Hyper calendar producer
- Ticket wording `autoService` = branch
- Lovable Cloud message store
- `IMPL-*` / `SEC-*` from the Hyper booking spec as mandatory IDs until mapped one-by-one in 284+

---

## Sequence (unchanged)

289 (this ADR) → 280 schema → 281 discovery → 282 catalog → 283 availability → 284 quote/create → 285 manage → 286 chat → 287 push.

See `.cursor/booking-sprint6-sira.md`.

---

## Consequences

- CRCT-280 **alters** partners/branches/ranges; **creates** catalog + booking tables. It does not `CREATE TABLE partners`.
- Owner-app `slotId` is `rangeId`. Staff calendar create must persist `booking_mode`.
- Seed (Hyper + two branches + Hyper Extra) fills catalog on **existing** Hyper partner / branches where possible; do not insert a second Hyper row.
