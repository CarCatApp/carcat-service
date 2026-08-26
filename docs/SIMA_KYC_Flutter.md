# SIMA KYC — Flutter entegrasyon rehberi

Bu dosya **tek kaynak**. Backend (carland_service) canlı sözleşmesi. Aziz’e sormadan buradan yaz.

Tarih: 24 Aug 2026 · Jira: CRCT-248, CRCT-249 (parent CRCT-232)

---

## 1. Ne değişti (tek cümle)

Müşteri SIMA ile doğrulanınca ad / soyad / FIN devlet kaydından gelir ve kilitlenir. Uygulama **doğrulama butonunu** `GET /user/information` içindeki `verified` alanına göre gizler. SIMA’ya gidince HTTP 200 almak **yetmez** — `verified: true` şart.

---

## 2. Yapma / yap

| Yap | Yapma |
|---|---|
| Citizen + foreign multipart | `/api/v1/sima/test/identity/verify` (Postman; kalkacak) |
| JPEG selfie, max 1 MB | PNG, HEIC, data-URI, 1 MB üstü |
| `verified` ile buton gizle | Sadece HTTP 200’e bakmak |
| Skorları 0.90 ile karşılaştırma (backend zaten bakıyor) | Skorları `90` ile karşılaştırmak |
| FIN alanı adı: `pin` | Login PIN (4 haneli) ile karıştırmak |
| Passport ekranı | AZE passport API (kapalı) |
| Idempotency key göndermek | — backend kendi üretir |

---

## 3. Base URL ve path

Prod gateway (Kong):

```
https://digital-innovation.agency/carland/server-carland
```

Örnek:

```
POST https://digital-innovation.agency/carland/server-carland/api/v1/sima/verify/citizen
GET  https://digital-innovation.agency/carland/server-carland/api/v1/user/information
```

Lokal / doğrudan servis: prefix yok, path `/api/v1/...` aynı.

---

## 4. Ortak header’lar

| Header | Zorunlu | Not |
|---|---|---|
| `Authorization` | Evet | `Bearer <access JWT>` |
| `X-User-Id` | Evet (Kong genelde JWT’den basar) | Müşteri id = auth user id |
| `role` | Profil API’de evet | `USER` |
| `phoneNumber` | Profil API’de evet | JWT’deki telefon |
| `Accept-Language` | Profilde evet; SIMA’da opsiyonel | `az` / `en` / `ru` — hata yazısı bu dile göre |
| `X-App-Version` | Feature flag `/me` için | Örn. `2.1.0` |

SIMA verify’de `role` / `phoneNumber` şart değil; `X-User-Id` şart.

---

## 5. Buton ne zaman görünsün

Sıra:

1. `GET /api/v1/feature-flags/me` → `SIMA_KYC_FLOW` (veya PO’nun verdiği flag adı) bu rol + app version için **ENABLED** değilse butonu **gösterme**.
2. `GET /api/v1/user/information` → `verified === true` ise butonu **gösterme** (kullanıcı zaten doğrulanmış).
3. İkisi de uygunsa “SIMA ile doğrula” göster.

Uygulama her açılışta `GET /information` çek. Local’e `verified` cache’le; yine de sunucu kaynağı `GET`’tir.

Flag HIDDEN/DISABLED: ekranı kapat. Backend yine çağrılsa da ürün kararı flag’dir.

---

## 6. İki kanal (ekran seçimi)

Pasaport **yok**.

### A) Yerel vatandaş — vesika

`POST /api/v1/sima/verify/citizen`  
`Content-Type: multipart/form-data`

| Form key | Zorunlu | Değer |
|---|---|---|
| `pin` | Evet | FIN, 7 karakter (harf+rakam). Ör. `62HJ5KQ` |
| `documentNumber` | XOR | Vesika seri/no. Ör. `AB0668397` |
| `birthDate` | XOR | `yyyy-MM-dd`. Ör. `1990-05-12` |
| `photo` | Evet | **File**, JPEG (`FF D8` ile başlar), **≤ 1 048 576 byte** |

**XOR:** `documentNumber` **veya** `birthDate` — tam biri dolu. İkisi birden veya ikisi boş → **400**, SIMA’ya gidilmez.

```
# doğru
pin + documentNumber + photo
pin + birthDate + photo

# yanlış
pin + documentNumber + birthDate + photo
pin + photo
```

### B) Yabancı — göçmenlik belgesi

`POST /api/v1/sima/verify/foreign`  
`Content-Type: multipart/form-data`

| Form key | Zorunlu | Değer |
|---|---|---|
| `pin` | Evet | FIN |
| `documentType` | Evet | Sadece `TRC` veya `PRC` veya `ERP` (büyük harf önerilir) |
| `photo` | Evet | Aynı JPEG kuralları |

`documentNumber` / `birthDate` bu kanalda **yok**.

---

## 7. Fotoğraf

- Multipart key adı tam olarak **`photo`**, type **File** (string base64 değil).
- JPEG. iOS HEIC → uygulamada JPEG’e çevir.
- `data:image/jpeg;base64,...` gönderme.
- 1 MB üstü → 400, SIMA’ya gitmez.
- Canlı yüz (selfie); evrak tarama değil.

Dio örneği:

```dart
FormData.fromMap({
  'pin': fin,
  'documentNumber': docNo, // XOR: birthDate kullanıyorsan bu satırı koyma
  'photo': await MultipartFile.fromFile(path, filename: 'selfie.jpg'),
});
```

`null` field gönderme; XOR’u bozar.

---

## 8. Verify JSON (her iki kanal, aynı şablon)

Body **her zaman** bu şekil (alanlar null olabilir). SIMA 4xx olsa bile bu şablon — global `error/message/status` **değil**.

```json
{
  "verified": false,
  "pin": "62HJ5KQ",
  "name": "ARAZ",
  "surname": "ƏLIYEV",
  "livenessScore": 0.996,
  "similarityScore": 0.999,
  "transactionId": "100550",
  "code": "OK",
  "message": "Verified",
  "simaResponseCode": 752,
  "simaMessage": "…",
  "simaErrorCode": 752
}
```

| Alan | Anlam |
|---|---|
| `verified` | **Tek başarı bayrağı.** `true` → profil güncellendi, butonu gizle, ad/soyad/FIN kilitle |
| `pin` / `name` / `surname` | SIMA’dan gelen (başarıda resmi). UI’yı bunlarla doldur |
| `livenessScore` / `similarityScore` | 0–1 ondalık. Eşik **0.90** (dahil). Backend karar verir; sen skor göstermek zorunda değilsin |
| `transactionId` | Destek / log |
| `code` | Bizim kod (`OK`, `SIMA_SCORE_GATE`, `PIN_ALREADY_EXISTS`, …) |
| `message` | Kullanıcıya gösterilebilecek yazı (dil header’ına göre bazıları) |
| `simaResponseCode` | SIMA’nın sayısal kodu (7xx). Yoksa `null` |
| `simaMessage` | SIMA’nın kendi metni. Varsa **bunu öncelikli göster** |
| `simaErrorCode` | `simaResponseCode` ile aynı (eski isim) |

Parse kuralı: `if (json['verified'] == true)` başarı. HTTP’ye bakarak “oldu” deme.

---

## 9. HTTP kodları (verify)

| HTTP | `verified` | Ne oldu | UI |
|---|---|---|---|
| **200** | `true` | KYC geçti, DB yazıldı | Butonu gizle; ad/soyad/FIN doldur ve kilitle |
| **200** | `false` | SIMA cevap verdi ama skor &lt; 0.90 veya iş kuralı | Buton kalsın; `simaMessage` yoksa `message` göster |
| **200** | `true` + `code: SIMA_ALREADY_VERIFIED` | Zaten doğrulanmış, SIMA’ya gidilmedi | Butonu gizle; hata toast’ı **yok** |
| **400** | `false` | SIMA validation / foto / XOR / `documentType` | Mesaj göster, tekrar dene |
| **409** | `false` + `code: PIN_ALREADY_EXISTS` | Bu FIN başka hesapta | “Bu FIN kodu artıq istifadə olunur” |
| 401 / 403 | (başka body) | JWT / Kong | Login |
| 5xx | SIMA veya biz | Tekrar dene | |

**Kritik:** HTTP 200 + `verified: false` = **başarısız KYC**. Loading’i kapat, yeşil tick basma.

Kullanıcı metni öncelik:

1. `simaMessage` (null değilse)
2. `message`
3. Sabit çeviri (`code`’a göre)

---

## 10. `code` listesi (verify body)

| `code` | Tipik HTTP | Anlam |
|---|---|---|
| `OK` | 200 | Doğrulandı |
| `SIMA_ALREADY_VERIFIED` | 200 | Zaten doğrulanmış |
| `SIMA_SCORE_GATE` | 200 | Skorlar 0.90 altında |
| `PIN_ALREADY_EXISTS` | 409 | FIN başka müşteride |
| `SIMA_EMPTY` | 502 civarı | Boş SIMA cevabı |
| `SIMA_<sayı>` | SIMA’nın HTTP’si | Ör. `SIMA_752` |
| `SIMA_FAIL` | SIMA HTTP | Kod yok, genel fail |

---

## 11. QA skor tablosu (backend ile aynı)

`verified: true` yalnız:

- SIMA işi başarılı **ve**
- `livenessScore >= 0.90` **ve**
- `similarityScore >= 0.90`

| # | liveness | similarity | Beklenen `verified` |
|---|---|---|---|
| 1 | 0.996 | 0.999 | true |
| 2 | 0.85 | 0.999 | false |
| 3 | 0.999 | 0.80 | false |
| 4 | 0.90 | 0.90 | true |
| 5 | 0.899 | 0.95 | false |

Sen skor hesabı yapmak **zorunda değilsin**; `verified` kullan. Tablo test / PO içindir.

---

## 12. Profil API — Məlumatlarım

### GET `/api/v1/user/information`

Header: `Authorization`, `role`, `phoneNumber`, `X-User-Id`, `Accept-Language`

```json
{
  "name": "ARAZ",
  "surname": "ƏLIYEV",
  "mail": "user@example.com",
  "pin": "62HJ5KQ",
  "phoneNumber": "+994501234567",
  "verified": true
}
```

| Alan | UI |
|---|---|
| `name` `surname` `pin` | `verified == true` → **read-only** (kilit ikon). Değiştirme |
| `mail` | Her zaman düzenlenebilir |
| `phoneNumber` | Salt okunur (JWT). PUT body’ye koyma |
| `verified` | `true` → SIMA butonu yok; ad/soyad/FIN kilit |

Baba adı, adres, cinsiyet, belge no **bu API’de yok**. Gösterme / isteme.

### PUT `/api/v1/user/information`

JSON:

```json
{
  "name": "Araz",
  "surname": "Eliyev",
  "mail": "yeni@mail.com",
  "pin": "62HJ5KQ"
}
```

Alias: `"email"` = `mail`.

Hepsi dolu olmalı (name, surname, mail, pin). Telefon body’de yok.

`verified == true` iken:

- Mail değişir.
- `name` / `surname` / `pin` **mevcut değerle aynı** gönder (GET’ten kopyala). Farklı gönderirsen **409**.

Başarılı PUT cevabı GET ile aynı şablon (`verified` dahil).

### PUT / GET hata gövdesi (verify’den farklı!)

Kilit / çakışma burada **SIMA şablonu değil**:

```json
{
  "error": "Conflict error",
  "message": "Təsdiqlənmiş ad, soyad və FIN dəyişdirilə bilməz",
  "timeStamp": "2026-08-24T20:00:00",
  "status": 409
}
```

| Durum | HTTP | `error` | `message` (az) |
|---|---|---|---|
| Verified iken ad/soyad/FIN değişti | 409 | Conflict error | Təsdiqlənmiş ad, soyad və FIN dəyişdirilə bilməz |
| FIN başka kullanıcıda (henüz verified değilken PUT) | 409 | Already exists error | Bu FIN kodu artıq istifadə olunur |
| Mail başka kullanıcıda | 409 | Already exists error | Bu e-poçt ünvanı artıq istifadə olunur |
| FIN format değil (7 alfanumerik) | 400 | | Invalid pin mesajı |
| Mail format değil | 400 | | Invalid mail |

`verified` kullanıcıda PUT’ta kilitli alanları disabled yap; 409’u kullanıcı hatası gibi gösterme.

FIN formatı (PUT): `^[A-Za-z0-9]{7}$`

---

## 13. Ekran akışı (önerilen)

```
Açılış
  → GET /feature-flags/me
  → GET /user/information
  → verified == true?
        evet → SIMA butonu yok; ad/soyad/FIN kilit; mail açık
        hayır → flag ENABLED ise buton var

Buton → yerel / yabancı seç
  → kamera JPEG ≤ 1MB
  → citizen: FIN + (vesika no XOR doğum tarihi)
  → foreign: FIN + TRC|PRC|ERP
  → POST verify
  → body.verified == true?
        evet → GET information (veya response’taki name/surname/pin) ile formu doldur
              → butonu gizle, kilitle
        hayır → simaMessage | message göster, buton kalsın
```

Çift tık: iki istek gidebilir; zararsız. İkinci istek already-verified 200 dönebilir. Toast basma.

Retry: yeni foto / aynı form. Key gönderme.

---

## 14. Kullanıcıya gösterilecek AZ metinler (hazır)

Backend çoğu durumda `message` / `simaMessage` basar. Yoksa:

| Durum | AZ |
|---|---|
| Skor düşük / yüz tutmadı | Üz uyğun gəlmədi. Yenidən cəhd edin. |
| FIN başkasında | Bu FIN kodu artıq istifadə olunur |
| Zaten verified | (toast yok; buton gizli) |
| XOR | Vəsiqə nömrəsi və ya doğum tarixindən yalnız birini doldurun |
| Foto | Yalnız JPEG, maksimum 1 MB |
| Foreign type | Sənəd tipi: TRC, PRC və ya ERP |
| Ağ / 5xx | Bir xəta baş verdi. Yenidən cəhd edin. |

EN/RU: `Accept-Language: en` veya `ru` — kilit ve FIN-çakışma backend’den gelir.

---

## 15. Feature flag

`GET /api/v1/feature-flags/me`  
Header: `role`, `X-App-Version` (ve JWT).

Cevaptaki named flag (prod’da `SIMA_KYC_FLOW` beklenir) state:

- `ENABLED` → akışı göster  
- `DISABLED` / `HIDDEN` → akışı gösterme  

Flag yok / 403: butonu gösterme (güvenli taraf).

---

## 16. Auth / login

- Login PIN (4 haneli) ≠ SIMA `pin` (FIN).
- Auth’taki ad/soyad **kullanma**; profil Carland `GET /information`.
- Doğrulama auth servisine gitmez.

---

## 17. Test checklist (Flutter)

- [ ] `verified: false` kullanıcıda buton var (flag açıkken)
- [ ] Başarılı citizen → buton yok, ad/soyad/FIN kilit, mail açık
- [ ] App kill/reopen → GET information → buton hâlâ yok
- [ ] Skor düşük simülasyonu (staging) → 200 + verified false → kırmızı/uyarı, buton duruyor
- [ ] Vesika + doğum tarihi birlikte → 400
- [ ] Foreign `documentType: PASSPORT` → 400
- [ ] PNG foto → 400
- [ ] Başkasının FIN’i → 409, AZ mesaj
- [ ] Verified iken PUT name değiştir → 409, alanlar kilitli olduğu için normalde olmamalı
- [ ] Verified iken sadece mail PUT (name/surname/pin GET ile aynı) → 200
- [ ] Test endpoint çağrılmıyor

---

## 18. Backend’e / Aziz’e sorma

Aşağıdakiler **kararlı**. Yeni Jira yoksa sorma:

- Passport kanalı yok  
- Profilde baba adı / adres / cinsiyet yok  
- Belge bitiş tarihi uygulamada yok (sadece DB)  
- Test API müşteriyi güncellemez  
- Idempotency Flutter’dan gitmez  
- `verified` kaynağı GET information  
- HTTP 200 ≠ KYC OK  

Eksik / bozuk staging cevap, 401 Kong, flag adı prod’da farklıysa — o zaman backend.

---

## 19. Kısa path özeti

```
POST /api/v1/sima/verify/citizen     multipart  JWT + X-User-Id
POST /api/v1/sima/verify/foreign     multipart  JWT + X-User-Id
GET  /api/v1/user/information        JWT + role + phoneNumber + X-User-Id
PUT  /api/v1/user/information        JSON       aynı header’lar
GET  /api/v1/feature-flags/me        JWT + role + X-App-Version

KULLANMA: POST /api/v1/sima/test/identity/verify
```
