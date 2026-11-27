# hyper history kullanicinin mevcut cartini ezmesi durumu

Tarih: 2026-11-25. Bu sürüm deploy edildi. Genel Hyper geçmiş kuralını daraltır. Zarar görürse aşağıdaki geri alma ile eski haline döner.

## Eski kural

Müşteri geçmişi açılınca Hyper aynı kaydı tekrar gönderir. Ziyaretin kilometresı ve satırları Hyper’ın sayısıyla değişir. Aynı kayıt numarası yağ kartının son kilometresini de Hyper’ın sayısıyla yeniler. Hyper kendi kaydındaki kilometreyi sonradan düzeltirse kart da düzelirdi.

Müşteri düzenlemesi (`EDITED_BY_CUSTOMER`) ve daha yeni bir ziyaret bu sürümden önce de vardı. Onlara dokunulmadı.

## Bu sürüm ne yapıyor

Aynı Hyper kaydı, kayıtlı kilometreden **daha düşük** bir sayı gönderirse yazılmaz.

- Geçmiş yenilemesi ziyaret başlığını düşürmez. Başlık yüksek kaldıysa, o ziyaretin boş veya daha düşük satır kilometreleri başlığa çekilir.
- Partner webhook’u da aynı kaydın kilometresini düşüremez. Daha yüksek gelen sayı yazılır.
- Kartın `partner_record_id` değeri bu Hyper kaydıyla aynıysa ve Hyper’ın son km’si karttakinden düşükse karta yazılmaz. Ziyaret, kartın son km’sine yükseltilir. Bu dalda Hyper’ın sonraki km ve tarihi de uygulanmaz.
- Daha yeni bir ziyaret (başka kayıt numarası) eskisi gibi kartın üzerine yazılır. Kilometresi düşük olsa da yazılır.

Kod: `HyperMileageGuard`. Çağıranlar: `VisitHistoryServiceImpl.applyHyperSnapshot`, `PartnerServiceVisitUpdateServiceImpl.applyVisitSnapshot`, `HyperPercentageSyncService.applyPartnerData`.

## Nereye zarar verebilir

Hyper aynı kayıtta kilometreyi bilerek düşürürse (yanlış 80.000, doğrusu 66.000) biz yüksek sayıyı tutarız. Hyper’ın düzeltmesi karta ve ziyarete girmez.

Başlık yüksek tutulunca o ziyaretin bütün satırları, yağ satırı olmayanlar dahil, başlığın kilometresine çekilir.

Kart koruması yalnız `partner_record_id` dolu ve bu kayıtla aynıysa çalışır. `EDITED_BY_CUSTOMER` ve boş `partner_record_id` bu sürümün dışında kalır; onu eski müşteri kuralı tutar. 77 OU 529 yağ kartı bu ikinci gruptadır (son 73.000, sonraki 81.000). 15 Kasım ziyareti (kayıt 2713) elde 73.000 yapıldı. Kodu geri almak o satırı silmez. Eski kural açılınca Hyper o ziyareti yine 66.000 yazabilir.

Logda aranacak cümleler: `Keeping higher visit mileage over Hyper snapshot`, `Keeping local last km — Hyper snapshot is lower`.

## Geri alma

1. `HyperMileageGuard.java` ve `HyperMileageGuardTest.java` silinir.
2. `VisitHistoryServiceImpl.applyHyperSnapshot` içinde kilometre yine doğrudan Hyper’dan yazılır. `stampLinesKeptAbove` çağrısı kalkar.

```java
visit.setLastServiceMileage(item.getLastServiceMileage());
```

3. `PartnerServiceVisitUpdateServiceImpl.applyVisitSnapshot` eski karşılaştırmaya döner.

```java
if (!Objects.equals(visit.getLastServiceMileage(), item.getMileage())) {
    visit.setLastServiceMileage(item.getMileage());
    changed++;
}
```

4. `HyperPercentageSyncService.applyPartnerData` yine `void` olur. Düşük km’yi reddeden blok ve `return false` kalkar. `syncInternal` her uygulamada `percentageRepository.save` çağırır.

```java
applyPartnerData(car, percentage, match, matchedRecordId);
percentageRepository.save(percentage);
```

5. `HyperPercentageSyncServiceTest` içindeki şu üç test silinir: `forcedReloadDoesNotRollSameRecordMileageBackwards`, `forcedReloadAppliesHigherMileageOnTheSameHyperRecord`, `forcedReloadStillAppliesANewerVisitEvenWhenItsMileageIsLower`.

6. Deploy. 15 Kasım ziyaretinin 73.000 kalması bu kodla bağlıdır. Eski kural deploy edilince Hyper aynı kaydı 66.000 ile yeniden basabilir.
