# Rollback Noktası / Rollback Point

> **tr:** Büyük yeniden düzenleme (reorganization-plan.pdf) başlamadan ÖNCE alınan git kaydı.
> Kod çalışmazsa bu dosyadaki commit'e dönülür.
> **en:** Git snapshot taken BEFORE the big reorganization (reorganization-plan.pdf) started.
> If the code breaks, revert to the commit recorded here.

- **Tarih / Date:** 2026-08-01 12:40 (UTC+4)
- **Branch:** `main`
- **Çalışan son commit / Last known-good commit (HEAD):**
  - `d882082408e3efd8b4416003f7ef615530ec16af` — "added swager.." (2026-07-31 15:51, deployed & çalışıyor)
- **Önceki commitler / Previous commits:**
  - `0525d1c9bd0e095b8d66d0829ab2e0d7609f416c` — "added swager"
  - `0bb18d73c4cefcc9f9e0a59722cb2e020ff123f3` — "added admin panel users table from to time"

## Snapshot anındaki commit'lenmemiş değişiklikler / Uncommitted changes at snapshot time

Refactor başlamadan önce working tree'de şunlar zaten vardı (kullanıcı tarafından yapılmış):

- `carcat-service/` kopya klasörünün silinmesi stage edilmiş (git rm, ~205 dosya)
- `HYPER_PARTNER_WEBHOOK_API.md` silinmiş
- `service/validation/HyperServiceVisitValidator` ve `service/mapper/HyperWebhookIngestMapper` → `service/webhook/` altına taşınmış
- Ufak değişiklikler: `RestTemplateConfig`, `PartnerServiceVisitIngestServiceImpl`, `PartnerServiceVisitUpdateServiceImpl`, `PhotoServiceImpl`, `MailTrapConfig`
- `util/CustomImageCrop.java` silinmiş
- Yeni dosyalar: `docs/reorganization-plan.html`, `docs/reorganization-plan.pdf`

## Geri dönüş talimatı / How to revert

**tr:** Refactor sonrası kod çalışmazsa, deploy edilen son çalışan sürüme dönmek için:

```bash
# 1) Önce mevcut durumu kaybetmemek için yedek branch al (isteğe bağlı ama önerilir):
git branch backup-refactor

# 2) Çalışan sürüme dön (DİKKAT: commit'lenmemiş her şeyi siler):
git reset --hard d882082408e3efd8b4416003f7ef615530ec16af
```

**en:** If the code does not work after the refactor, to return to the last deployed working version:

```bash
# 1) Optionally back up current state first:
git branch backup-refactor

# 2) Hard reset to the working commit (WARNING: discards all uncommitted changes):
git reset --hard d882082408e3efd8b4416003f7ef615530ec16af
```

> **Not / Note:** `d882082` sunucuda şu an çalışan (GitHub Actions ile deploy edilmiş) sürümdür.
> `d882082` is the version currently running on the server (deployed via GitHub Actions).
