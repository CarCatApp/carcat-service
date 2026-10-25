# App announcements — Flutter entegrasyon rehberi

Bu dosya **tek kaynak**. Backend `carland_service` canlı sözleşmesi. Hedefleme (sürüm, platform, dil, saat, mağaza linki) sunucuda biter. Uygulama listeyi seçmez, sadece gösterir.

Tarih: 21 Oct 2026

---

## 1. Ne

Cold start’ta bir kez çağrılır. Gösterilecek duyuru varsa bottom sheet açılır. Liste boşsa uygulama normal açılır. Kullanıcıya hata gösterilmez.

Duyuruyu panel kurar (`/admin/app-announcements`). Flutter’da oluşturma / düzenleme API’si yok.

---

## 2. Yap / yapma

| Yap | Yapma |
|---|---|
| Yalnızca cold start’ta `GET .../active` | Her ekranda, pull-to-refresh’te, login’de tekrar çağırma |
| `200` + JSON **dizi** bekle. Boş = `[]` | `204`, `404`, `null`, `{}` beklemek |
| `id`’yi **int** oku | `id`’yi String sanmak |
| Sıralı listenin ilk uygun kaydını göster (aşağıda) | Listeyi tek elemana indirmek veya kendi sıranı kurmak |
| Görüldü’yü cihazda `id` ile tut | User id / telefon ile “görüldü” tutmak |
| `dismissible: false` ise sheet kapanmasın | Zorunlu duyuruyu “görüldü” deyip bir daha hiç göstermemek |
| `ctaUrl` varsa onu aç (https). `ctaUrlIos` / `ctaUrlAndroid` varsa cihazın mağazasını onlar arasından seç | Mağaza linkini uygulamada sabitlemek |
| `ctaLabel` null ise kendi varsayılan buton metnin | Boş buton basmak |
| Bu path’te `401` olursa **logout etme** | Global interceptor’ın oturumu kapatması |
| Hata olursa sessizce devam et | Snackbar / dialog |
| Bilinmeyen JSON alanını yut | Alan adı değişince kırılmak |

---

## 3. URL

Prod (Kong):

```text
GET https://digital-innovation.agency/carland/server-carland/api/v1/app-announcements/active
```

Servis içi path aynı: `/api/v1/app-announcements/active`

Gövde yok. Query yok. `POST` yok.

---

## 4. Header

| Header | Gönder | Sunucu |
|---|---|---|
| `Accept-Language` | Uygulama dili: `az`, `en`, `ru`. `az-AZ` de olur | İlk etiketin ana dili. Desteklenmeyen veya yoksa `az`. Metin bu dilde gelir; o dil yoksa `az` |
| `X-App-Version` | Mağaza sürümü, örn. `3.0.5` | Sunucu karşılaştırır. `3.0.10` > `3.0.5`. `+76` ve `-beta` atılır |
| `X-Platform` | `IOS` veya `ANDROID` | Büyük/küçük harf serbest. Başka değer = platform yok sayılır |
| `Authorization` | İstersen mevcut Bearer kalsın | **Okunmaz.** Geçersiz token bu API’de 401 sebebi değildir |
| `X-Client-Timezone`, `X-Skip-Token-Refresh`, `X-User-Id`, `role` | Gerekmez | Yok sayılır |

Geçersiz header **400 üretmez**. Liste daralır: sürüm okunamazsa yalnız sürüm sınırı olmayanlar, platform okunamazsa yalnız herkese açık olanlar gelir.

`X-App-Version` ve `X-Platform` 32 karakteri aşmasın.

---

## 5. Cevap

`200`

`Content-Type: application/json; charset=UTF-8`

Zarfsız dizi. En fazla **5** eleman. Sıra anlamlıdır: kapatılamazlar üstte, sonra bakım, güncelleme, bilgi.

Boş:

```json
[]
```

Dolu:

```json
[
  {
    "id": 12,
    "type": "UPDATE",
    "dismissible": false,
    "title": "Yeniləmə tələb olunur",
    "body": "Davam etmək üçün tətbiqi yeniləyin.",
    "ctaLabel": "Yenilə",
    "ctaUrl": "https://apps.apple.com/app/id000",
    "ctaUrlIos": "https://apps.apple.com/app/id000",
    "ctaUrlAndroid": "https://play.google.com/store/apps/details?id=com.nematmirzayev.carcat"
  },
  {
    "id": 13,
    "type": "INFO",
    "dismissible": true,
    "title": "Xidmət saatları",
    "body": "Bayram günü mərkəzlər bağlıdır.",
    "ctaLabel": null,
    "ctaUrl": null,
    "ctaUrlIos": null,
    "ctaUrlAndroid": null
  }
]
```

### Alanlar

| Alan | Tip | Anlam |
|---|---|---|
| `id` | number (int) | Kalıcı. Başka duyuruya tekrar verilmez. “Görüldü” anahtarı budur |
| `type` | `UPDATE` \| `INFO` \| `MAINTENANCE` | Yalnız ikon / stil. Mantık kurma |
| `dismissible` | bool | `false` = sheet kapatılamaz |
| `title` | string | Düz metin, boş değil |
| `body` | string | Düz metin. Satır sonu `\n`. Markdown / HTML yok |
| `ctaLabel` | string veya null | Buton yazısı. Null veya alan yok = senin varsayılan metnin (UPDATE için) |
| `ctaUrl` | string veya null | İsteyen platformun linki (`X-Platform`). Eski uygulama bunu açar |
| `ctaUrlIos` | string veya null | App Store. ALL kaydında `cta_url_ios`. Boş UPDATE’te sunucunun iOS mağaza linki |
| `ctaUrlAndroid` | string veya null | Play Store. ALL kaydında `cta_url_android`. Boş UPDATE’te sunucunun Android mağaza linki |

`ctaLabel` / `ctaUrl` / `ctaUrlIos` / `ctaUrlAndroid` JSON’da `null` gelebilir. Alan hiç gelmezse de null say.

Bu cevapta **olmayan** alanlar: `platform`, `minAppVersion`, `maxAppVersion`, `startsAt`, `endsAt`, `priority`, `active`. Sunucu bunları uygulamadan önce elemiştir.

`title` / `body` Azerbaycan harfini düz UTF-8 döner (`ə ı ş ğ ö ü ç`).

---

## 6. Sheet’i kim açarsın

Sunucu zaten sırayı kurmuş olur. Senin kuralın:

1. Listede `dismissible == false` varsa **ilkini** göster. Bunu cihazda “görüldü” diye saklama. Sunucu listeden düşünceye kadar her cold start’ta tekrar gelir (zorunlu güncelleme böyle kalır).
2. Yoksa, cihazda kayıtlı görülmüş `id`’ler arasında **olmayan ilk** kaydı göster.
3. O da yoksa sheet açma.

Kullanıcı kapatırsa yalnız o `id`’yi cihaza yaz (uygulama verisi, hesaba bağlı değil). Aynı id bir daha gösterilmez. Panoda “kopyala” yeni id üretir; o yeni kayıt gösterilir.

CTA:

- `ctaUrlIos` ve `ctaUrlAndroid` ikisi de gelebilir. Cihazın mağazasını aç.
- `ctaUrl` doluysa ve platform linki yoksa onu aç.
- `UPDATE` ve `ctaLabel` null ise varsayılan “Yenilə” / “Update” / “Обновить”.
- İkisi de null ise buton koyma (`INFO` / `MAINTENANCE` böyle gelebilir).

---

## 7. Hata

| Durum | Ne yap |
|---|---|
| `200` + `[]` | Sheet yok. Normal açılış |
| `200` + dizi | Bölüm 6 |
| `5xx` ve `{ "message": "..." }` | Yut. `message`’ı kullanıcıya basma |
| Ağ hatası, timeout, parse hatası | Yut. Açılışı kilitleme |
| `401` / `403` | Bu path için **logout yok**. Global JWT interceptor bu URL’i muaf tutsun |

Servis bu path’te 400 / 401 / 403 / 404 yazmaz. `401` görürsen sebep Kong’un JWT zorlamasıdır; backend’e yaz, kullanıcıyı atma.

---

## 8. Örnek istek

```bash
curl -i "https://digital-innovation.agency/carland/server-carland/api/v1/app-announcements/active" \
  -H "Accept-Language: az" \
  -H "X-App-Version: 3.0.5" \
  -H "X-Platform: IOS"
```

Token’siz de `200` ve dizi döner. Bozuk `Authorization: Bearer ...` de aynı sonucu vermeli.

---

## 9. Sana düşmeyen iş

- Semver karşılaştırması
- Hangi duyurunun bu sürüme / bu güne / bu dile ait olduğu
- Play Store / App Store linki üretmek
- Yayınlama, taslak, çeviri CRUD

Bunlar panel + sunucu. Sen header’ı doğru gönder, diziyi göster.
