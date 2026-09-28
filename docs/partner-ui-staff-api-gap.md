# Partner-UI (Lovable) ↔ carland_service Staff API uyum raporu

## Context
Lovable prototipi (`prism-appointments.lovable.app`) yerelde `partner-ui` olarak yeniden yazılıyor. Kullanıcı, Lovable'daki tüm sayfa/component/davranışların `carland_service` staff API'leriyle ne kadar uyumlu olduğunu, hangi alanların eksik olduğunu ve eksikler için önerilen API + DTO örneklerini istiyor. **Kod değişikliği yapılmayacak.**

### Nasıl incelendi
- İlk olarak uygulamanın public JS bundle'ları (`/assets/*.js`: login, dashboard, slots, catalog, settings, BookingsManager, service-packages, maintenance-catalog, tabs, phone) indirilip okundu.
- 2026-09-27'de Lovable'a Chrome ile girilip tüm sayfalar gezildi: rezervasyon listesi + 3 sekme, müşteri/araç/hizmet popover'ları, onay/ret, "Xidməti tamamla", "Müştəri gəlmədi", slot yaratma drawer'ı (1 haftalık test slotu üretildi, sadece localStorage), slot tablosu, manuel rezervasyon, Xidmətlər (paket matrisi + fərdi 3 tier), Tənzimləmələr, header menüsü. Bundle okumasıyla çıkan bulgular doğrulandı; farklar aşağıda "Tarayıcı doğrulaması" notlarıyla işaretli.
- **Önemli bulgu:** Lovable'da login sahte: `admin@hyperservis.az / Hyper2026!` bundle'da hardcoded, `localStorage.hs_admin_auth=1` yapıyor. Hiçbir sayfa API çağırmıyor; tüm veri mock ve `localStorage`'da tutuluyor (`hs_service_packages_v5`, `hs_service_lines_v1`, `hs_maintenance_catalog_v2`, `carcat.slotgen.v2`, `carcat.about.v1`). Bu yüzden rapordaki DTO'lar Lovable'ın mock veri şekillerinden çıkarıldı.
- Backend tarafında incelenenler: `BookingStaffApiController`, `StaffBookingService`, `BookingView`/`BookingInboxResponse`, `CalendarController`+`CalendarServiceImpl`, `RangeController`, `BookingDiscoveryController` (catalog/availability), `BookingCatalogService`, `BookingAvailabilityService`, entity'ler (`Booking`, `BookingItem`, `Branch`, `BranchPackage`, `BranchService`, `BranchServiceBrand`, `Range`, `Calendar`, `Partner`, `BookingStaff`). Auth akışı `carland_auth/StaffAuthController` üzerinden.

---

## 1. Özet matris

| Lovable sayfası / davranış | Backend karşılığı | Durum |
|---|---|---|
| Login | `POST /api/v1/staff/login` (carland_auth) | ✅ Tam (yerel UI zaten bağlı; Lovable'daki login sahte) |
| Şifremi unuttum (OTP → reset), ilk girişte şifre değişimi | `/staff/password/forgot`, `/verify-otp`, `/reset`, `PUT /staff/me/own-password` | ✅ Tam |
| Rezervasyonlar: pending onaylama | `POST /booking/staff/bookings/{id}/accept` | ✅ Tam |
| Rezervasyonlar: liste + sekmeler + sayaç + filtre | `GET /booking/staff/bookings` | ⚠️ Kısmi |
| Rezervasyonlar: reddetme (sebep seçerek) | `POST /booking/staff/bookings/{id}/reject` | ⚠️ Kısmi (sebep kaydedilmiyor) |
| Müşteri popover'ı (ad, telefon, avatar, WhatsApp) | – | ❌ Yok |
| Araç popover'ı (marka, model, kasa, yakıt, yıl, motor, km) | – | ❌ Yok |
| Hizmet tipi popover'ı (Dövri qulluq paketi/hizmetleri, Təmir/Yoxlanış/Alqı-satqı detayı) | `serviceKeys` sadece | ⚠️ Kısmi |
| Onaylanmış rezervasyon → "Xidməti tamamla" formu | – | ❌ Yok |
| Onaylanmış rezervasyon → "Müştəri gəlmədi" (no-show) | – | ❌ Yok |
| Slotlar: gün/hizmet bazında slot tablosu | `GET /api/v1/calendar/get`, owner `GET /booking/branches/{id}/availability` | ⚠️ Kısmi |
| Slotlar: toplu slot üretme (tarih aralığı, hafta içi/sonu, mola, çoklu hizmet) | `POST /api/v1/calendar/create` | ⚠️ Kısmi |
| Slotlar: saati kapat/aç, günü tamamen kapat/aç | – | ❌ Yok |
| Slotlar: kapasite +/- | – | ❌ Yok |
| Slotlar: manuel rezervasyon (telefon/walk-in) | – | ❌ Yok (`/range/book` müşteri akışı) |
| Slotlar: slot detayındaki rezervasyonlar, status değiştirme, silme | – | ❌ Yok |
| Slotlar: "N gün için növbə mövcuddur" göstergesi | – | ❌ Yok (liste gelirse client-side hesaplanabilir) |
| Xidmətlər: hizmet hattı aktif/deaktif (sadece Təmir / Yoxlanış / Alqı-Satqı; Dövri Qulluq'un toggle'ı yok, hep aktif) | `BranchService(kind=DIRECTION)` sadece owner GET | ⚠️ Kısmi (staff okuma/yazma yok) |
| Xidmətlər: paket matrisi (fiyat, aktif, dahil hizmetler, miktar, Hyper Extra özel hizmetleri az/en/ru) | `BranchPackage` sadece owner GET | ⚠️ Kısmi |
| Xidmətlər: fərdi hizmetler × 3 zorluk fiyatı (Sadə / Nisbətən mürəkkəb / Mürəkkəb) | `BranchService.priceMin/Max` | ⚠️ Kısmi (tier yok, yazma yok) |
| Sazlamalar: profil kartı (servis adı, e-poçt, saat dilimi) | `GET /booking/staff/branches` | ⚠️ Kısmi |
| Sazlamalar: "Hyper Servis haqqında" (ad, instagram, mallar, 8 foto) | – | ❌ Yok |
| Sazlamalar: hizmet başına marka/model + birim listesi | `BranchServiceBrand` (sadece marka adı) | ⚠️ Kısmi / pratikte ❌ |
| Header: bildirim zili, global arama | – | ❌ Yok (düşük öncelik) |

---

## 2. ✅ Tam uyumlu

1. **Staff auth** (carland_auth): login / forgot / verify-otp / reset / own-password. Yerel `src/auth.js` zaten doğru kullanıyor. `mustChangePassword` akışı carland_service tarafında da (`StaffBookingService.requireStaff`) uygulanıyor.
2. **Accept**: `POST /api/v1/booking/staff/bookings/{bookingId}/accept` → `BookingView`. Lovable'daki 2 checkbox'lı onay diyaloğu tamamen UI tarafında. Sadece `PENDING` iken çalışıyor (409); Lovable davranışıyla aynı.

## 3. ⚠️ API var ama uyum eksik

### 3.1 `GET /api/v1/booking/staff/bookings` (Rezervasyonlar sayfası)
Bugün dönen: `bookingId, ref, status, bookingMode, branchId, slotId, day, start, end, timezone, vin, carId, serviceKeys, priceMin/Max, currency, unit, unreadCount`. `startsAt`, `branchName`, `partnerName` dolmuyor.

Lovable'ın beklediği ama gelmeyenler:
- **Müşteri:** `customerName` (ad + soyad ayrı filtreleniyor), `phone`, `email`, `avatarUrl`. Yerel `DashboardPage.jsx` bunları şimdilik `mockFor()` ile uyduruyor.
- **Araç:** `brand`, `model`, `plate`, `year`, `body`, `fuel`, `engine`, `mileage`.
- **Hizmet:** yön (`routine|repair|inspection|trade`), paket adı (Klassik/Prestij/Titan/Hyper Extra) ya da hizmet başlıkları, repair/inspection/trade için serbest `detail` metni. Şu an sadece ham `serviceKeys` geliyor.
- **Süre / bitiş:** Lovable `duration` ("3h 15m") gösteriyor. Backend `end` = slot bitişi, iş süresi değil.
- **İptal sebebi:** `cancelReason` (kapalı sekmede info ikonu). `Booking.cancelReasonCode/cancelNote` var ama view'da yok.
- **Filtreler:** VIN, plaka, ad, soyad, telefon, tarih aralığı. Backend'de hiçbiri yok; yerel UI sadece VIN ve tarihi client-side filtreliyor.
- **Çoklu status:** Lovable'da "Bağlı" sekmesi = completed + canceled; "Təsdiqlənib" sekmesi = confirmed + (instant). Backend tek status alıyor, yerel UI 2–3 istek atıp birleştiriyor.
- **Sekme sayaçları:** her sekmenin toplamı gerekiyor. Response'ta `total`/`counts` yok.
- **Sıralama:** UI gün + saate göre grupluyor. Backend `createdAt DESC` sıralıyor, bu da sayfalamada gruplamayı bozuyor.
- **Status sözlüğü:** Lovable `canceled`, backend `cancelled` + ayrıca `rejected`, `auto_accepted`. UI sözlüğü backend'e göre sabitlenmeli (yerel UI zaten backend değerlerini kullanıyor).

### 3.2 `POST /booking/staff/bookings/{id}/reject`
- Controller `BookingRejectRequest{reason}` alıyor ama `StaffBookingService.reject` bunu **kullanmıyor**; sebep kaydedilmiyor.
- Lovable sabit sebep listesi + "Digər" serbest metin gönderiyor: `reject.r1..r5` (Slot artıq tutulub, Ehtiyat hissə mövcud deyil, Texniki imkan yoxdur, Müştəri cavab vermir, Səhv məlumat/dublikat) + `other`.
- Gereken: `reasonCode` + `note` alanları, `Booking.cancelReasonCode/cancelNote`'a yazılması ve listede geri dönmesi. Sebep listesi için bir endpoint (mevcut `BookingCancelReason` deseni kullanılabilir).

### 3.3 Slot listesi: `GET /api/v1/calendar/get` ve owner `.../availability`
- `calendar/get` **GET + `@RequestBody`**. Tarayıcıdaki `fetch` GET ile body gönderemez, bu yüzden UI'dan kullanılamaz. Ayrıca `role/phoneNumber/X-User-Id` header'larına dayanıyor ve takvim `serviceCategory` ile anahtarlanıyor; UI ise `serviceKey` (`pkg:…`, `svc:…`, `dir:…`) kullanıyor.
- `availability` owner token + booking flag'iyle çalışıyor. Slot başına `capacity/bookedCount/remaining/bookingMode/status` veriyor, fakat **slottaki rezervasyonların listesini** (müşteri adı, araç, kanal, not, status) vermiyor. Lovable tablosunda bu liste var.
- Lovable slot state'leri: `open|closed` + doluluk (`available|partial|full|closed`). Backend'de `RangeStatus.AVAILABLE` + hesaplanan `OPEN/FULL`.

### 3.4 Slot üretimi: `POST /api/v1/calendar/create`
Mevcut request tek gün, tek aralık, tek `serviceCategory`, tek `bookingMode`, tek `serviceKey`. Aynı gün için takvim varsa 409 dönüyor.

Lovable "Slot yaratma" drawer'ı ise şunları gönderiyor:
- birden çok hizmet (`routine` altında birden çok paket/hizmet + `repair`/`inspection`/`trade`)
- tarih aralığı (1 hafta / 1 ay / özel)
- hafta içi: başlangıç–bitiş, interval (30 / 60 / özel), kapasite (≤50), öğle molası
- hafta sonu: Cumartesi/Pazar açık-kapalı, ayrı saat, kapasite, mola
- hafta içi ve hafta sonu için **ayrı `bookingMode`** (`instant`/`approval`)
- çakışan mevcut slotu atlıyor ve sadece modunu güncelliyor; sonuç `{created, updated, skippedOverlap, skippedDays}`

### 3.5 Xidmətlər (katalog)
- Veri modeli kısmen var: `BranchService` (kind=`DIRECTION` ile `dir:repair|inspection|trade`, active) ve `BranchPackage` (`titleJson`, `priceMin/Max`, `includedServiceKeys`, active). Ama sadece **owner** `GET /booking/branches/{id}/catalog` var ve o da yalnızca aktif satırları döndürüyor. **Staff okuma ve yazma endpoint'i yok.**
- Eksik model alanları:
  - paket meta: `interval` ("6 ay / 10 000 km"), `checks`
  - paket içi kalemlerin grubu (`replace|extra|inspect|service`), kalem başına dahil/hariç ve `qty`
  - Hyper Extra'ya özel kalemler (az/en/ru)
  - fərdi hizmetlerde 3 tier fiyatı (`simple|medium|complex`)
  - Təmir / Yoxlanış / Alqı-Satqı hatlarının `active` bayrağı. **Tarayıcı doğrulaması:** Dövri Qulluq kartında toggle yok, hep aktif; bu yüzden routine için DIRECTION satırı/bayrağı gerekmiyor.
  - Hat deaktifken "Slot yaratma" drawer'ında o hat seçilemiyor ("Xidmətlər bölməsində deaktivdir"). Slot üretme endpoint'i deaktif hat için slot üretmeyi reddetmeli.

### 3.6 Sazlamalar
- `GET /booking/staff/branches` → `partner{id,name,logoUrl,rating,branches[…]}`. Profil kartındaki servis adı buradan gelebilir. `Partner.contactEmail` entity'de var ama `BookingStaffPartnerView`'da yok; timezone hiç yok.
- `BranchServiceBrand` sadece `brand` string tutuyor. Lovable'ın "Xidmətlərə uyğun marka və model" listesi ise ürün başına `name, unit (ədəd/litr/kq/metr/dəst/xidmət), price, country, note, imageUrl` istiyor ve "Standarta qaytar" (reset) davranışı var. Bu liste "Xidməti tamamla" formundaki marka seçicisini ve miktar birimini de besliyor.

## 4. ❌ API karşılığı yok
1. Rezervasyonu **tamamlama** ("Xidməti tamamla"). Her hizmet için: marka/model (katalogdan veya "Digər"), miktar, fiyat, endirim %, yekun məbləğ, tamamlanma tarihi, növbəti tarix (+6/12/18/24 ay, kapatılabilir), cari km, növbəti km (+5k/10k/20k/50k, kapatılabilir), not, dosya ekleri. Formdan hizmet eklenip çıkarılabiliyor. (`CarController /add/record` ve `AutoServiceController /insert/service/history` servis geçmişi yazıyor ama staff token + booking bağı yok; implementasyonda yeniden kullanım adayı olarak incelenmeli.)
2. **No-show**: `ns.r1..r5` + other sebep listesiyle.
3. **Slot operasyonları**: saat kapat/aç, gün kapat/aç (rezervasyonu olan saat kapanmaz), kapasite +/- (`booked ≤ capacity ≤ 50`).
4. **Manuel rezervasyon**: ad (≤40), telefon (`+994 XX XXX XX XX`), kanal (`phone|walkin|app|partner`), slot, plaka, marka-model, not (≤500). Status slot moduna göre: instant → accepted, approval → pending.
5. Slot detayında rezervasyon **status değiştirme** (pending/accepted/completed/cancelled) ve **silme/iptal**.
6. Slot "stok" özeti (hizmet başına kalan gün, slot sayısı, boş yer).
7. Hizmet hattı toggle, paket/hizmet yazma işlemleri (bkz. 3.5).
8. "Hyper Servis haqqında": servis adı, instagram, mallar (tag listesi), en fazla 8 fotoğraf.
9. Bildirim zili / global arama (header). **Tarayıcı doğrulaması:** Lovable'da zil tıklanınca hiçbir şey açılmıyor, yalnızca görsel; şimdilik API gerekmiyor.

---

## 5. Önerilen API'ler ve DTO örnekleri (tasarım; kod yok)
Ortak kurallar: base `/api/v1/booking/staff`, staff JWT, `Accept-Language`, `X-Client-Timezone`, `branchId` yetkisi `BookingStaffAccess.requireWritableBranch` ile. Para her yerde **qəpik** (`unit:"qepik"`), UI ₼'ye çeviriyor.

### 5.1 Rezervasyonlar
**`GET /bookings`** (mevcut endpoint genişletilir)
```
?tab=pending|confirmed|closed        // veya status=pending,confirmed (virgüllü)
&branchId=12&vin=&plate=&firstName=&lastName=&phone=&from=2026-09-01&to=2026-09-30
&page=1&pageSize=50&sort=day_asc
```
```json
{
  "page": 1, "pageSize": 50, "total": 37,
  "counts": { "pending": 5, "confirmed": 12, "closed": 20 },
  "items": [{
    "bookingId": 10431, "ref": "BK-10431",
    "status": "pending", "bookingMode": "approval", "source": "app",
    "branchId": 12, "branchName": "Central Bay",
    "day": "2026-09-28", "start": "10:45", "end": "11:15", "timezone": "Asia/Baku",
    "estimatedDurationMin": 240,
    "service": {
      "direction": "routine",
      "package": { "serviceKey": "pkg:classic", "title": "Klassik" },
      "items": [{ "serviceKey": "svc:oil", "title": "Mühərrik yağı dəyişimi" }],
      "detail": null
    },
    "customer": { "userId": 881, "name": "Konul", "surname": "Məmmədli",
                  "phone": "+994503001982", "email": null, "avatarUrl": "https://…" },
    "car": { "carId": 5521, "vin": "X6S07U115BF91J1FM", "plate": "88-HY-669",
             "brand": "Lexus", "model": "LX 600", "year": 2022,
             "body": "SUV", "fuel": "Benzin", "engine": "3500 sm³", "mileageKm": 45200 },
    "price": { "min": 5700, "max": 5700, "currency": "AZN", "unit": "qepik" },
    "cancel": null,
    "createdAt": "2026-09-26T08:12:00Z"
  }]
}
```
`cancel` örneği: `{ "type": "rejected|no_show|cancelled_by_customer", "reasonCode": "SLOT_TAKEN", "note": "…", "at": "…" }`

**`GET /bookings/{id}`** → aynı `StaffBookingView` + `items[]` fiyat satırları.

**`GET /reasons?type=reject|no_show`**
```json
{ "items": [
  { "code": "SLOT_TAKEN", "title": { "az": "Slot artıq tutulub", "en": "Slot already taken" } },
  { "code": "OTHER", "title": { "az": "Digər (əl ilə yaz)" }, "requiresNote": true } ] }
```
**`POST /bookings/{id}/reject`** (mevcut; body kullanılır hale gelir)
```json
{ "reasonCode": "NO_PARTS", "note": null }
```
**`POST /bookings/{id}/no-show`** (confirmed/auto_accepted → yeni status `no_show` ya da `cancelled` + type)
```json
{ "reasonCode": "LATE_ARRIVAL", "note": null }
```
**`POST /bookings/{id}/complete`** (confirmed → completed)
```json
{
  "items": [{
    "serviceKey": "svc:oil", "title": "Mühərrik yağı dəyişimi",
    "product": { "productId": 301, "name": "Mobil 1 5W-40", "qty": 4.5, "unit": "LITER" },
    "priceQepik": 18000, "discountPercent": 10, "finalQepik": 16200,
    "completedAt": "2026-09-28",
    "nextServiceDate": "2027-03-28",
    "currentMileageKm": 45200, "nextServiceMileageKm": 55200,
    "notes": "…", "attachmentIds": ["f_9a1"]
  }]
}
```
Dosya: `POST /uploads` (multipart) → `{ "id": "f_9a1", "url": "…" }`

### 5.2 Slotlar
**`GET /slots?branchId=12&serviceKey=pkg:classic&day=2026-09-28`**
```json
{
  "day": "2026-09-28", "serviceKey": "pkg:classic", "dayClosed": false,
  "slots": [{
    "slotId": 7781, "start": "09:00", "end": "09:30",
    "state": "open", "fill": "partial",
    "capacity": 2, "bookedCount": 1, "remaining": 1,
    "bookingMode": "approval", "serviceKey": "pkg:classic",
    "bookings": [{ "bookingId": 10431, "ref": "BK-10431", "status": "pending",
                   "customerName": "Nemət", "phone": "+994501234567",
                   "vehicle": "Chevrolet Tahoe", "plate": "10-AB-123",
                   "source": "walkin", "note": "…" }]
  }]
}
```
**`GET /slots/coverage?branchId=12`** (sekme rozetleri)
```json
{ "items": [{ "serviceKey": "routine", "days": 6, "lastDay": "2026-10-03",
              "daysLeft": 6, "slots": 96, "free": 150 }] }
```
**`POST /slots/generate`** (`calendar/create` yerine veya yanında)
```json
{
  "branchId": 12,
  "serviceKeys": ["pkg:classic", "svc:oil", "dir:repair"],
  "startDate": "2026-09-28", "endDate": "2026-10-04",
  "weekday": { "start": "09:00", "end": "18:00", "intervalMin": 30, "capacity": 2,
               "bookingMode": "approval",
               "break": { "enabled": true, "start": "13:00", "end": "14:00" } },
  "weekend": { "saturday": true, "sunday": false, "start": "10:00", "end": "16:00",
               "capacity": 1, "bookingMode": "instant",
               "break": { "enabled": false } }
}
```
→ `{ "created": 180, "updated": 12, "skippedOverlap": 12, "skippedDays": 1 }`

**`PATCH /slots/{slotId}`** → `{ "capacity": 3 }` veya `{ "state": "closed" }`. Rezervasyonlu slotu kapatmak ya da kapasiteyi `bookedCount` altına düşürmek 409.

**`POST /slots/day-state`** → `{ "branchId": 12, "serviceKey": "pkg:classic", "day": "2026-09-28", "state": "closed" }` → `{ "updated": 17, "skippedWithBookings": 2 }`

**`POST /slots/{slotId}/bookings`** (manuel rezervasyon)
```json
{ "customerName": "Nemət", "phone": "+994 50 123 45 67", "source": "PHONE",
  "plate": "10-AB-123", "vehicle": "Chevrolet Tahoe", "note": "Əlavə məlumat" }
```
→ `StaffBookingView`. Status: instant → `confirmed`, approval → `pending`. Müşteri kullanıcısı olmadığı için `Booking.customerUserId` nullable olmalı ya da ayrı `guest_*` alanları gerekir.

**`POST /bookings/{id}/cancel`** → `{ "reasonCode": "STAFF_CANCEL", "note": null }` (slot detayındaki "Sil"; kapasiteyi geri açar)

### 5.3 Xidmətlər (katalog)
**`GET /catalog?branchId=12`** (inaktifler dahil)
```json
{
  "lines": [ { "key": "routine", "active": true }, { "key": "repair", "active": false },
             { "key": "inspection", "active": false }, { "key": "trade", "active": false } ],
  "packages": [{
    "serviceKey": "pkg:classic", "tier": "small",
    "title": { "az": "Klassik" }, "priceQepik": 3900, "active": true,
    "interval": { "months": 6, "km": 10000 }, "checks": 26,
    "items": [ { "itemKey": "replace.engine_oil", "group": "replace",
                 "title": { "az": "Mühərrik yağı" }, "available": true,
                 "included": true, "qty": null } ],
    "customItems": []
  }],
  "services": [{
    "serviceKey": "svc:oil_filter", "code": "MYF",
    "title": { "az": "Mühərrik yağı və filtri dəyişimi" }, "active": true,
    "tierPrices": { "simple": 4000, "medium": 6000, "complex": 9000 }
  }]
}
```
- `PATCH /catalog/lines/{key}` → `{ "active": true }`
- `PATCH /catalog/packages/{serviceKey}` → `{ "title": { "az": "Hyper Extra" }, "priceQepik": 12900, "active": true }`
- `PUT /catalog/packages/{serviceKey}/items` → `[ { "itemKey": "replace.air_filter", "included": false, "qty": null } ]`
- `POST /catalog/packages/{serviceKey}/custom-items` → `{ "group": "extra", "title": { "az": "…", "en": "…", "ru": "…" } }`; ayrıca `PATCH …/{id}`, `DELETE …/{id}` (sadece Hyper Extra)
- `PATCH /catalog/services/{serviceKey}` → `{ "active": true, "tierPrices": { "simple": 4000, "medium": 6000, "complex": 9000 } }`

### 5.4 Sazlamalar
**`GET /me`**
```json
{ "userId": 55, "name": "Nemat", "surname": "Mirzayev", "role": "PARTNER_ADMIN",
  "partner": { "id": 3, "name": "HyperServis", "contactEmail": "ops@garageos.app", "logoUrl": "…" },
  "branch": { "id": 12, "name": "Central Bay", "timezone": "Asia/Baku" } }
```
**`GET` / `PUT /branches/{id}/about`**
```json
{ "serviceName": "HyperServis — Central Bay", "instagram": "@hyperservis.az",
  "goods": ["Mühərrik yağı", "Filterlər", "Əyləc mayesi", "Antifriz"],
  "photos": [ { "id": 91, "url": "…" } ] }
```
- `POST /branches/{id}/photos` (multipart, en fazla 8), `DELETE /branches/{id}/photos/{photoId}`

**`GET /branches/{id}/service-products?serviceKey=svc:oil`**
```json
{ "items": [ { "id": 301, "serviceKey": "svc:oil", "name": "Mobil 1 5W-40", "unit": "LITER",
               "priceQepik": 2450, "country": "ABŞ", "note": "…", "imageUrl": null } ] }
```
- `POST` / `PATCH /{productId}` / `DELETE /{productId}`, `POST /branches/{id}/service-products/reset`
- Birim enum'u: `PIECE | LITER | KG | METER | SET | SERVICE`

### 5.5 Opsiyonel
`GET /notifications?unread=true` (header zili): yeni pending rezervasyon ve slot bitiyor uyarıları.

---

## 6. Dikkat edilecek model/kavram uyumsuzlukları
- **Hizmet anahtarları:** UI `routine/repair/inspection/trade` + `pkg:small|medium|large|hyper` + `svc:*` kullanıyor. Backend `dir:repair|inspection|trade`, `pkg:*`, `svc:*` ve slotta `serviceKey` (`*` veya tek anahtar) kullanıyor; takvim ise ayrıca `serviceCategory` ile anahtarlı. Tek sözlükte birleşmeli.
- **Slot = Range, gün = Calendar:** Calendar `(day, serviceCategory, branch)` başına tek kayıt; UI'da gün × çoklu serviceKey var.
- **Hizmet adları üç farklı yerde:** Lovable'da maintenance-catalog İngilizce anahtarlar ("Engine Oil Change"), paket kalemleri ve fərdi hizmetler ise Azerbaycanca adlar kullanıyor. Backend'de `itemKey`/`serviceKey` sabitlenmeli.
- **Status:** `pending, confirmed, auto_accepted, rejected, cancelled, completed` + önerilen `no_show`. Lovable'ın `canceled`/`accepted` terimleri buna eşlenmeli.

## 7. Onay sonrası yapılacak (kod değişikliği yok)
- Bu raporu `C:\Users\Aziz\IdeaProjects\carland_service\docs\partner-ui-staff-api-gap.md` olarak kaydetmek (docs klasörü mevcut). Başka dosyaya dokunulmayacak.

## Doğrulama
- Rapordaki her "mevcut" iddia yukarıda adı geçen controller/service dosyalarından okundu. Lovable davranışları indirilen bundle'lardan çıkarıldı (scratchpad `lv/`).
- İsteğe bağlı: `/chrome` ile tarayıcı araçları açılırsa, Lovable ekranları görsel olarak gezilip matris teyit edilebilir.
