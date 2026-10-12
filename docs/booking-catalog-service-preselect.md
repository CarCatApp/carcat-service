# Katalog hizmeti ön seçimi

`GET /api/v1/booking/branches/{branchId}/individual-services`

Yeni endpoint yok. Percentages API değişmez. Mobil, `GET /service/percentages` gövdesindeki `serviceId` değerini bu çağrıya `catalogServiceId` olarak gönderir.

## Query

Bugünkü parametreler aynıdır.

| Parametre | Ne zaman okunur | Yok sayılma |
|---|---|---|
| `filter` | Her çağrı | Yok, boş veya `all` tam listedir. Sayı veya filter adı bugünkü gibi daraltır. Percentages `actionType` (örnek `Filters`) bu alan değildir. |
| `carId` | Yüzde alanları için | Ön seçim için gerekmez. Yüzde hâlâ `carId` ve `X-User-Id` ile dolar. |
| `bookingEntry` | Ön seçim kapısı | Yok, `STANDARD` veya başka bir değer: standart liste. Trim ve büyük harf uygulanır (`car_service` = `CAR_SERVICE`). Bilinmeyen değer `400` üretmez. |
| `catalogServiceId` | Yalnız `bookingEntry=CAR_SERVICE` | Diğer girişlerde gelse de okunmaz. `CAR_SERVICE` iken yoksa ön seçim yapılmaz, `400` yoktur. Sayı değilse Spring `Long` çevirisi `400` verir. |

`percentageId` gönderilmez. Eşleşme hizmet adıyla yapılmaz.

## Eşleşme

`catalogServiceId`, `services.id` değeridir. Açılış senkronu aynı üç dil adına düşen bütün şablon satırlarını (dizel ve benzin dahil) tek `individual_services` satırına bağlar. İstek bu bağı okur.

İşaret, bu yanıtın `services[]` listesinde olan satırlara bakılarak konur. Liste daralmaz. Kapalı şube satırı listede yoktur. `filter` bağlı satırı listeden çıkardıysa o çağrıda işaret yoktur.

Birden fazla bağlı satır bu listede varsa yalnız en küçük `individual_services.id` işaretlenir. Yanıttaki `id` ve `preselectedServiceId` bu id’dir. `branch_individual_services.id` değildir.

Eşleşme yoksa `200`. `404` yoktur.

## JSON

Eşleşme. `name` bugünkü gibi `Accept-Language` ile lokalize edilir. `priceMin`, `priceMid` ve `priceMax` bugünkü gibi qəpiktir (manat × 100).

`GET /api/v1/booking/branches/1/individual-services?carId=1091&filter=all&bookingEntry=CAR_SERVICE&catalogServiceId=143`

```json
{
  "branchId": 1,
  "preselectedServiceId": 501,
  "services": [
    {
      "id": 501,
      "code": "AIR_FILTER",
      "name": "Air filter",
      "priceMin": 2000,
      "priceMid": 3000,
      "priceMax": 4000,
      "currency": "AZN",
      "unit": "pcs",
      "individualServiceMappedPercentage": 8,
      "percentageNote": null,
      "remainingKm": 1200,
      "remainingMonth": 40,
      "catalogServiceId": 143,
      "preselected": true
    }
  ]
}
```

Eşleşme yok. Liste, fiyat ve yüzde bugünkü gibidir. `preselectedServiceId` durur ve `null`dır. Hiçbir satırda `preselected` veya `catalogServiceId` yoktur.

`GET /api/v1/booking/branches/1/individual-services?carId=1091&filter=all&bookingEntry=CAR_SERVICE&catalogServiceId=999999`

```json
{
  "branchId": 1,
  "preselectedServiceId": null,
  "services": [
    {
      "id": 501,
      "code": "AIR_FILTER",
      "name": "Air filter",
      "priceMin": 2000,
      "priceMid": 3000,
      "priceMax": 4000,
      "currency": "AZN",
      "unit": "pcs",
      "individualServiceMappedPercentage": 8,
      "percentageNote": null,
      "remainingKm": 1200,
      "remainingMonth": 40
    }
  ]
}
```

## Alanların yok ve null kuralları

| Durum | `preselectedServiceId` | Satırda `preselected` | Satırda `catalogServiceId` |
|---|---|---|---|
| `bookingEntry` yok, `STANDARD` veya başka değer | Alan yok | Alan yok | Alan yok |
| `CAR_SERVICE`, `catalogServiceId` yok | `null` | Alan yok | Alan yok |
| `CAR_SERVICE`, bağlı satır listede yok | `null` | Alan yok | Alan yok |
| `CAR_SERVICE`, eşleşme var | Eşleşen `id` | Yalnız o satırda `true` | Yalnız o satırda, gönderilen id |

Alan yoksa istemci `preselected` için `false` okur. Diğer satırlara `false` yazılmaz.

`carId` varsa her satırda `remainingKm` ve `remainingMonth` durur. Sayılar `GET /service/percentages` ile aynı yoldan gelir: elle düzenlenmiş satır kayıtlı son/sonrakini, CREATED satır Hyper ziyaretini, o da yoksa son servis artı şablon aralığını kullanır. Eşleşme, bu aracın şablonundaki `services.id` ile fərdi xidmətin `individual_service_sources` bağıdır. `remainingKm` o satırın `remainingKm` değeridir. `remainingMonth` o satırın `remainingMonths` gün sayısıdır, tipi sayıdır. Araç yoksa, bağlı şablon satırı yoksa veya km/tarih eksikse alan `null`dır. `individualServiceMappedPercentage` ve `percentageNote` hesabı değişmez. Ön seçim ad okumaz.

## Şube listesi

`GET /api/v1/booking/branches` gövdesi aynıdır. `page`, `pageSize`, `lat` ve `lng` bugünkü gibi okunmaz.

`findService` yoksa liste bugünkü gibidir. Varsa değer `services.id` dir (`catalogServiceId` ile aynı sayı). Şube listede kalırsa bağlı fərdi xidmət şubede açıktır, katalog satırı aktiftir, üç fiyattan en az biri yazılmıştır ve o hizmet için bugünden sonra yazılabilir en az bir saat vardır. Üst süre yoktur; sonraki aylar sayılır. Yazılabilir saat, ay takvimindeki kuraldır: başlangıç şimdiden sonra, gizli değil, durum `AVAILABLE`, kalan yer en az bir. Eşleşme yoksa `200` ve boş `branches`. Sayı değilse Spring `400` verir.

## Değişmeyen endpointler

Randevu gövdesi aynı kalır: `rangeId`, `packageId`, `individualServiceIds`, `issue`, `carId`. Seçilen hizmet, mobilin mevcut `individualServiceIds` alanına koyduğu `preselectedServiceId` değeridir.

- `GET /api/v1/booking/branches/{branchId}`
- `GET /api/v1/booking/individual-service-filters`
- `GET /api/v1/booking/branches/{branchId}/care-packages`
- `GET /api/v1/booking/care-packages/{packageId}/price-info`
- `GET /api/v1/booking/branches/{branchId}/calendar`
- `GET /api/v1/booking/branches/{branchId}/ranges`
- `POST /api/v1/booking/branches/{branchId}/appointments`
