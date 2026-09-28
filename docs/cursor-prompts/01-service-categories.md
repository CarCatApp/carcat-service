# Görev: Hizmet kategorileri (service categories) — backend + admin panel

Proje: `carland_service` (Spring Boot, JPA, PostgreSQL, `ddl-auto: update`, Thymeleaf admin panel).
Bu görevde **sadece backend ve admin paneli** yazılacak. partner-ui'a dokunma. Commit atma.
Kod stili: mevcut dosyalar gibi Lombok (`@Data @Builder @NoArgsConstructor @AllArgsConstructor @FieldDefaults(level = PRIVATE)`), sınıf/metot üstünde `tr:` / `en:` iki satırlı javadoc.

## Amaç
Partner panelindeki (partner-ui) "Xidmətlər" sayfasında şu an kodda sabit duran 2 kategori kartı backend'den gelecek:
- **Dövri Qulluq** — detay sayfası açılır, açma/kapama düğmesi yok (hep aktif).
- **Təmir Xidməti və Yoxlanış** — şube bazında açılıp kapatılır.

Kategorileri ve ikonlarını CarCat ekibi (ana şirket) admin panelinden yönetecek. Partner personeli sadece kendi şubesi için aç/kapat yapacak.

---

## 1. Entity'ler (`entity/`)

### `ServiceCategory` → tablo `service_categories` (tüm şubeler için ortak)
| alan | kolon | not |
|---|---|---|
| `Long id` | PK identity | |
| `String code` | `code`, unique, not null, length 64 | ör. `routine`, `repair_inspection` |
| `String titleJson` | `title_json`, not null, length 1024 | `{"az":"…","en":"…","ru":"…"}` — `BranchService.titleJson` ile aynı format |
| `String descriptionJson` | `description_json`, length 2048 | aynı format |
| `Integer sortOrder` | `sort_order`, not null, default 0 | |
| `Boolean openable` | not null, default false | true → partner-ui'da "Aç" ile detay sayfası |
| `Boolean toggleable` | not null, default true | false → şube kapatamaz, hep aktif |
| `Boolean active` | not null, default true | global aç/kapa (false → hiçbir şubeye listelenmez) |
| `String directionKeys` | `direction_keys`, length 256 | virgüllü `BranchService.serviceKey` listesi, ör. `dir:repair,dir:inspection`; boş olabilir |
| `Integer iconVersion` | `icon_version`, not null, default 0 | her ikon yüklemede +1 (tarayıcı önbelleği için) |

### `BranchServiceCategory` → tablo `branch_service_categories` (şube bazında aç/kapa)
- `Long id`, `Branch branch` (ManyToOne LAZY, `branch_id`, `BranchService`'teki gibi `@ForeignKey(ConstraintMode.NO_CONSTRAINT)`), `ServiceCategory category` (ManyToOne LAZY, `category_id`), `Boolean active` (not null).
- Unique constraint: `(branch_id, category_id)`.

### `ServiceCategoryPhoto` → tablo `service_category_photos`
`entity/PercentagePhoto.java` ile **birebir aynı yapı**: `imageId`, `Long categoryId` (unique), `fileName`, `fileType`, `@Lob @JdbcTypeCode(Types.BINARY) byte[] imageData`.

Her biri için `repository/` altında repository (`PercentagePhotoRepository`, `BranchServiceRepository` örnek al). Gerekli sorgular: kategoriler `findAllByOrderBySortOrderAscIdAsc`, `findByActiveTrueOrderBySortOrderAscIdAsc`, `findByCode`; şube satırı `findByBranch_IdAndCategory_Id`, `findByBranch_Id`; foto `findByCategoryId`.

---

## 2. Aktiflik kuralı (tek yerde, serviste)
Bir şube için kategori aktif mi:
- `category.active == false` → listelenmez.
- `category.toggleable == false` → her zaman `active = true`.
- aksi halde → `branch_service_categories` satırı varsa onun `active`'i, **yoksa `false`**.

Aç/kapa yazılırken (`toggleable == true` olmalı, değilse 409):
1. `branch_service_categories` satırını upsert et.
2. `directionKeys` içindeki her anahtar için o şubenin `BranchService` satırını (`branch_id + service_key`) bul, `active`'ini aynı değere çek. Satır yoksa oluştur: `kind = "DIRECTION"`, `titleJson = category.titleJson`, `currency = "AZN"` (`BookingCatalogSeeder.seedDirection` örneği). Böylece müşteri tarafındaki mevcut katalog (`BookingCatalogService`, `dir:*` satırları) aynı durumu görür.
Hepsi tek `@Transactional` içinde.

---

## 3. Partner (staff) API — yeni controller `BookingStaffCatalogController`
Auth, `BookingStaffApiController` ile aynı: `BookingStaffRequestAuth.userId(request)` + `mustChangePassword(request)`; servis tarafında `StaffBookingService.requireStaff` (private, satır ~112) mantığının aynısı → `BookingStaffAccess.requireActive(...)`, şube için `BookingStaffAccess.requireWritableBranch(staff, branchId, lang)`. `requireStaff`'ı kopyalamak yerine ortak bir yere taşıyabilirsin; `StaffBookingService` davranışı değişmesin.

**Şube çözümü (`branchId` opsiyonel):** verilmişse `requireWritableBranch`; verilmemişse `BRANCH_ADMIN` → `staff.getBranch()`; diğer roller → partnerin en küçük id'li şubesi (`BranchRepository.findByPartnerOrderByIdAsc`). Yanıtta kullanılan `branchId` dönsün.

### `GET /api/v1/booking/staff/catalog/categories?branchId=12`
Header: `Accept-Language` (az|en|ru, default az), `Authorization: Bearer …`.
```json
{
  "branchId": 12,
  "items": [
    {
      "id": 1,
      "code": "routine",
      "title": "Dövri Qulluq",
      "description": "Paketlər və fərdi dövri qulluq xidmətləri",
      "iconUrl": "/api/v1/photo/for/service-category/get?categoryId=1&v=3",
      "openable": true,
      "toggleable": false,
      "active": true
    }
  ]
}
```
- Sadece `category.active == true` olanlar, `sortOrder, id` sırasıyla.
- `title` / `description`: `titleJson` / `descriptionJson` içinden dile göre seçilir, yoksa `az`. `BookingCatalogService` title_json'u nasıl çözüyorsa onu yeniden kullan (ObjectMapper'ı orada var).
- `iconUrl`: kategori için foto satırı yoksa `null`; varsa yukarıdaki göreli yol, `v = iconVersion`.

### `PATCH /api/v1/booking/staff/catalog/categories/{id}`
Body `{ "branchId": 12, "active": true }` (`branchId` opsiyonel, kural yukarıdaki gibi). Dönüş: güncel tek item (listedeki şekil). Kategori yoksa 404, `toggleable == false` ise 409 (`ConflictException`).

DTO'lar `dto/booking/` altında: `StaffCatalogCategoryView`, `StaffCatalogCategoryListResponse`, `StaffCatalogCategoryToggleRequest`.

---

## 4. İkon API — `PhotoController` (+ `PhotoService` / `PhotoServiceImpl`)
`/for/percentage/get` ve `/for/percentage/upload` akışının **aynısı** (bkz. `PhotoServiceImpl.uploadPercentagePhoto` ~654, `getPercentagePhoto` ~687, `detectImage`, `mediaTypeOf`, `RedisCacheService` percentage metotları ~104-117):
- `GET /api/v1/photo/for/service-category/get?categoryId=` → `ResponseEntity<byte[]>`. Kategori yoksa 404, foto yoksa 404 (empty-state fallback yok). Başarılı yanıta `Cache-Control: public, max-age=31536000, immutable` ekle (URL'de `v` olduğu için güvenli).
- `POST /api/v1/photo/for/service-category/upload?categoryId=` multipart `file` → `PhotoResponse`. Eski byte'ların üstüne yazar, `category.iconVersion++`, Redis'i commit sonrası temizler.
- `DELETE /api/v1/photo/for/service-category/delete?categoryId=` → foto satırını siler, `iconVersion++`.
- Redis: `RedisCacheService`'e percentage'dakilerin eşi `getServiceCategoryPhoto / putServiceCategoryPhoto / evictServiceCategoryPhotoAfterCommit` ekle (ayrı key prefix).

**GET herkese açık olmalı** (ikonlar hassas değil, tarayıcı `<img src>` token gönderemez):
- `security/JWTConfiguration.java` → `.requestMatchers(HttpMethod.GET, "/api/v1/photo/for/service-category/get").permitAll()` ekle.
- `CustomFilter` bu yolda token yoksa isteği reddediyor mu kontrol et; reddediyorsa bu yolu atlat (diğer permitAll yollar nasıl atlanıyorsa aynı şekilde).
- `config/FeatureFlagWebConfig.java` interceptor'ı `/api/**`'a uygulanıyor; bu yolu etkileyip etkilemediğini kontrol et, etkiliyorsa `excludePathPatterns`'e ekle.
- **Upload ve delete public olmayacak** — sadece admin panel kullanacak (aşağıda).

---

## 5. Admin panel (Thymeleaf) — yeni sayfa `/admin/service-categories`
Örnek al: `controller/AdminPercentagePhotoController.java` + `templates/percentage-photos.html` (cookie auth, ikon önizleme + yükleme JS'i orada hazır).

Yeni controller `AdminServiceCategoryController` (`@Hidden @Controller`):
- Her endpoint başında `adminAccessService.isPanelAdmin(request)`; sayfa için değilse `redirect:` (percentage'daki gibi), JSON/byte endpoint'lerinde 401.
- `GET /admin/service-categories` → tüm kategoriler (aktif/pasif), `sortOrder` sırası → `templates/service-categories.html`.
- `POST /admin/service-categories/save` (JSON veya form): yeni oluştur ya da `id` varsa güncelle. Alanlar: `code` (sadece oluştururken; `^[a-z0-9_]+$`, unique → çakışırsa 409), `titleAz/En/Ru` (az zorunlu), `descriptionAz/En/Ru`, `sortOrder`, `openable`, `toggleable`, `active`, `directionKeys`. JSON alanlarını ObjectMapper ile üret (string birleştirme yapma).
- `GET /admin/service-categories/icon?categoryId=` → önizleme (PhotoService'i çağırır).
- `POST /admin/service-categories/icon/upload?categoryId=` multipart `file` → PhotoService upload.
- `POST /admin/service-categories/icon/delete?categoryId=` → PhotoService delete.
- Kategori **silme yok**; pasifleştirmek için `active = false`.

`templates/service-categories.html`:
- `percentage-photos.html`'in iskeletini, `admin.css`'ini ve header'ını kullan.
- Tablo: ikon önizleme (64px) + "Yüklə" / "Sil", code, AZ/EN/RU başlık, AZ açıklama, sıra, openable, toggleable, active, direction keys, "Düzəlt" butonu.
- Üstte "Yeni kateqoriya" butonu; oluşturma ve düzenleme aynı form/modal. Kaydedince sayfa yenilensin.
- Menüye link ekle: `<a class="nav-link" href="/admin/service-categories">Categories</a>` — `class="nav-links"` geçen **bütün** şablonlara (şu an 9 dosya: booking-partner-detail, booking-partners, car-history, cars, feature-flags, feedbacks, percentage-photos, push-notifications, users). Yeni sayfada `active` sınıfı.

---

## 6. Başlangıç verisi — `scripts/service_categories.sql`
Diğer `scripts/*.sql` dosyaları gibi, tekrar çalıştırılabilir (PostgreSQL): `CREATE TABLE IF NOT EXISTS` (3 tablo, entity'lerle aynı kolonlar) + `INSERT … ON CONFLICT (code) DO NOTHING`:

| code | title az / en / ru | description az / en / ru | sort | openable | toggleable | direction_keys |
|---|---|---|---|---|---|---|
| `routine` | Dövri Qulluq / Routine Care / Плановое обслуживание | Paketlər və fərdi dövri qulluq xidmətləri / Packages and individual routine care services / Пакеты и отдельные услуги планового обслуживания | 1 | true | false | (boş) |
| `repair_inspection` | Təmir Xidməti və Yoxlanış / Repair & Inspection / Ремонт и диагностика | Avtomobil təmir və yoxlanış xidmətləri / Vehicle repair and inspection services / Ремонт и диагностика автомобиля | 2 | false | true | `dir:repair,dir:inspection` |

---

## Dokunma
- `BookingCatalogService`, `BookingDiscoveryController`, `StaffBookingService` davranışları değişmesin (sadece ortak `requireStaff` taşınırsa aynı davranış).
- Mevcut percentage ikon endpoint'leri aynen kalsın.
- Yeni bağımlılık ekleme.

## Bitince doğrula
1. `./gradlew compileJava` hatasız.
2. Uygulamayı çalıştır, `scripts/service_categories.sql`'i uygula.
3. Admin panelde `/admin/service-categories` açılıyor, 2 kategori görünüyor, ikon yüklenip önizlemede çıkıyor, düzenleme kaydediliyor, menü linki tüm sayfalarda var.
4. Token'sız: `curl -i "http://localhost:<port>/api/v1/photo/for/service-category/get?categoryId=1"` → 200 + resim (ikon yüklendiyse), `Cache-Control` header'ı var.
5. Staff token'ıyla: `GET /api/v1/booking/staff/catalog/categories` → 2 item; `repair_inspection` başta `active:false`. `PATCH …/categories/2 {"active":true}` → `active:true` ve o şubenin `dir:repair` + `dir:inspection` `branch_services` satırları `active = true`. `PATCH …/categories/1` → 409.
6. Değiştirdiğin/eklediğin dosyaların listesini ve her endpoint için bir örnek isteği özetle.
