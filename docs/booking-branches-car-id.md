# Şube listesine seçili araç

`GET /api/v1/booking/branches`

Bunu Hyperservice'in müşteri kaybını kesmek için ekledik. Hyperservice müşterileri Karkat'a alınırken onların hâlâ gelen müşterisine rakip servis göstermek, o müşteriyi başkasına yollamak olur. CEO bunu istemiyor. Son bir yıldır Hyperservice'e gelmeyen, ya da Karkat'a kendi gelen müşteri bütün şubeleri görmeye devam eder.

Kural seçili araca bakar. Aynı kişinin bir arabası yalnız Hyperservice şubelerini, diğer arabası bütün şubeleri görebilir. Liste hangi arabaya baktığını bilmeden bu ayrılamaz.

## Eski sürüm

`carId` zorunlu değil. Eski uygulama bu alanı göndermez. Göndermezse cevap bugünkü listenin aynısıdır: bütün aktif şubeler, hata yok, kısıtlama yok. Eski sürümü kırmamak için alanı zorunlu yapmadık.

## Flutter ne yazacak

Seçili arabanın kimliğini query'ye ekle. Bundan sonraki sürümlerde bu alan gitsin. Alan gittiği andan itibaren kural o araç için çalışır. Eski sürüm alanı yazmadığı için kural orada devreye girmez.

```
GET /api/v1/booking/branches?carId=1091
```

`findService` duruyor. İkisi birlikte gider.

```
GET /api/v1/booking/branches?findService=144&carId=1091
```

`carId` varken `X-User-Id` de gider. Bu başlık diğer araçlı çağrılarda zaten var. Boşsa cevap `400`. Araç yoksa `404`. Araç bu kullanıcıya ait değilse `403`.

Cevabın şekli değişmedi. Gövde yine `branches` dizisidir. Yeni alan eklenmedi. Kilitli araçta `partnerId` 1 olmayan satırlar gelmez. Açık araçta liste tamdır. `findService` doluysa önce hizmete göre süzülür, sonra kilit uygulanır.

## Ekranda ne görünür

İlk uyan hal geçerlidir.

| Durum | Liste |
|---|---|
| Araç Hyperservice kaydında yok | Bütün şubeler |
| Son Hyperservice ziyareti 365 günden daha eski | Bütün şubeler. Bir kez açılır, sonra Hyperservice'e gelse de geri kapanmaz |
| Müşterinin Karkat'taki ilk işi, öncesinde geçmişi olmayan bir randevu | Bütün şubeler. Randevu Hyperservice'e olsa da sonuç değişmez. Bu da geri kapanmaz |
| Son 365 günde Hyperservice'e gelmiş ve üsttekiler değil | Yalnız Hyperservice şubeleri |

Tam 365. gün hâlâ yalnız Hyperservice'tir. Ertesi gün liste kendiliğinden açılır.

Bu turda kural yalnız bu listededir. Arama ve harita (`GET /api/v1/booking/partners`) `carId` okumaz.
