package com.carland.carland_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * tr: Hukuki içerik REST controller'ı; kullanım koşulları ve gizlilik politikasını az/en/ru dillerinde döner.
 *     Metin kaynağı: Terms v4.0, Privacy v4.0 — "CarCat" MMC (az/en/ru). Tek belge iki mobil ekrana bölündü.
 * en: Legal content REST controller; serves terms and privacy policy in az/en/ru.
 *     Source: Terms v4.0, Privacy v4.0 — "CarCat" LLC (az/en/ru). One document, two mobile screens.
 */
@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
@Slf4j
public class LegalController {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @GetMapping("/terms-and-conditions")
    public ResponseEntity<JsonNode> getTerms(
            @RequestParam(value = "lang", defaultValue = "az") String lang) {
        return jsonResponse(termsJson(lang));
    }

    @GetMapping("/privacy/policy")
    public ResponseEntity<JsonNode> getPolicy(
            @RequestParam(value = "lang", defaultValue = "az") String lang) {
        return jsonResponse(policyJson(lang));
    }

    @GetMapping("/get")
    public String get() {
        log.info("bura girdi");
        return "AAHAHHAHAHAHAHHAHAAHAHAHA";
    }

    private static ResponseEntity<JsonNode> jsonResponse(String json) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.readTree(json));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private static String termsJson(String lang) {
        String key = langKey(lang);
        if (key.startsWith("en")) {
            return TERMS_JSON_EN;
        }
        if (key.startsWith("ru")) {
            return TERMS_JSON_RU;
        }
        return TERMS_JSON_AZ;
    }

    private static String policyJson(String lang) {
        String key = langKey(lang);
        if (key.startsWith("en")) {
            return POLICY_JSON_EN;
        }
        if (key.startsWith("ru")) {
            return POLICY_JSON_RU;
        }
        return POLICY_JSON_AZ;
    }

    private static String langKey(String lang) {
        return lang == null ? "az" : lang.toLowerCase();
    }

    // -------------------------------------------------------------------------
    // Terms of Use v4.0 — AZ / EN / RU
    // Sections 1–6, 11, 12 of the combined document.
    // -------------------------------------------------------------------------

    private static final String TERMS_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "15 noyabr 2026",
                "version": "4.0",
                "language": "az",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — İstifadə şərtləri",
                "subtitle": "Son yenilənmə: 15 noyabr 2026 | Versiya: 4.0 | \\"CarCat\\" MMC"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Ümumi müddəalar",
                  "content": "1.1. Bu İstifadə şərtləri və Məxfilik siyasəti (bundan sonra \\"Sənəd\\") \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti (bundan sonra \\"CarCat\\") ilə \\"CarCat\\" mobil tətbiqindən və rəqəmsal platformasından (bundan sonra \\"Tətbiq\\") istifadə edən şəxs (bundan sonra \\"İstifadəçi\\") arasında yaranan münasibətləri, habelə İstifadəçinin fərdi məlumatlarının toplanması, işlənməsi və qorunması qaydalarını müəyyən edir.\\n\\n1.2. İstifadəçi Tətbiqdə qeydiyyatdan keçməklə bu Sənədin şərtlərini tam həcmdə qəbul etdiyini və fərdi məlumatlarının Sənəddə göstərilən qaydada işlənməsinə razılıq verdiyini təsdiq edir.\\n\\n1.3. Fərdi məlumatların işlənməsi Azərbaycan Respublikasının qanunvericiliyinə uyğun olaraq həyata keçirilir."
                },
                {
                  "id": 2,
                  "title": "2. Şirkət haqqında məlumat",
                  "content": "2.1. Tətbiqin və fərdi məlumatların informasiya sisteminin mülkiyyətçisi və operatoru \\"CarCat\\" MMC-dir:\\n• Hüquqi şəxsin adı: \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• Dövlət reyestri: \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CarCat MİS)\\" informasiya sistemi Azərbaycan Respublikası Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• Ünvan: Bakı şəhəri, Azərbaycan Respublikası\\n• Elektron poçt: info@carcat.app\\n• Telefon: +994 70 575 75 70\\n• Veb-sayt: https://carcat.app"
                },
                {
                  "id": 3,
                  "title": "3. Xidmətlərin təsviri",
                  "content": "3.1. Tətbiq İstifadəçiyə avtomobili və ya digər nəqliyyat vasitəsi (bundan sonra \\"Nəqliyyat vasitəsi\\") ilə bağlı xidmətləri vahid platformada idarə etmək imkanı verir. CarCat servis, sığorta və digər xidmətləri bilavasitə göstərmir, İstifadəçini servis mərkəzləri, sığorta təşkilatları, digər xidmət təchizatçıları və tərəfdaşlar (bundan sonra \\"Xidmət təchizatçıları və Tərəfdaşlar\\") ilə əlaqələndirir.\\n\\n3.2. Tətbiq vasitəsilə aşağıdakı xidmətlər təqdim olunur:\\na) servis xidmətləri: tərəfdaş servis mərkəzlərində texniki baxış, yağdəyişmə, təmir, diaqnostika, alqı-satqı öncəsi yoxlama və digər servis xidmətlərinin bron edilməsi;\\nb) CarCat Passport: VIN kod əsasında Nəqliyyat vasitəsinin tarixçəsinin yoxlanılması, xidmət tarixçəsinin, yürüşünün və texniki xəbərdarlıqlarının izlənilməsi;\\nc) sığorta xidmətləri: sığorta təşkilatlarından və müvafiq dövlət qurumlarından sığorta məlumatlarının və təkliflərinin əldə edilməsi, sığorta müqavilələrinin rəsmiləşdirilməsi;\\nç) cərimələr: dövlət qeydiyyat nişanı və FİN kod əsasında inzibati xətalara görə tətbiq edilmiş cərimələr barədə məlumatların əldə edilməsi və onların ödənilməsi;\\nd) nəqliyyat vasitələri ekosistemi ilə bağlı digər xidmətlər.\\n\\n3.3. Tətbiq vasitəsilə sifariş edilən xidmət üzrə müqavilə bilavasitə İstifadəçi ilə müvafiq Xidmət təchizatçısı və ya Tərəfdaş arasında bağlanır. İstifadəçi sifarişin rəsmiləşdirilməsi və bunun üçün zəruri olan məlumatların Xidmət təchizatçısına və ya Tərəfdaşa ötürülməsi üçün CarCat-a səlahiyyət verir.\\n\\n3.4. Tətbiq daxilində ödənişlər, o cümlədən cərimə ödənişləri lisenziyalı bank və ödəniş təşkilatları vasitəsilə həyata keçirilir.\\n\\n3.5. CarCat Tətbiqin funksiyalarını və təqdim olunan xidmətlərin tərkibini dəyişdirmək, məhdudlaşdırmaq və ya dayandırmaq hüququna malikdir."
                },
                {
                  "id": 4,
                  "title": "4. İstifadəçinin hüquq və öhdəlikləri",
                  "content": "4.1. Tətbiqdən mobil nömrə ilə qeydiyyatdan keçmiş və qeydiyyata aldığı Nəqliyyat vasitəsinin mülkiyyətçisi olan və ya ondan istifadə hüququna malik şəxslər istifadə edə bilər.\\n\\n4.2. Tətbiq şəxsi istifadə üçün nəzərdə tutulub. Tətbiqdən kommersiya məqsədilə istifadə yalnız CarCat ilə ayrıca razılaşma əsasında mümkündür.\\n\\n4.3. İstifadəçi:\\na) hesab məlumatlarının və təsdiq kodlarının məxfiliyini təmin etməlidir;\\nb) mobil nömrəyə nəzarəti itirdikdə dərhal CarCat-a məlumat verməlidir;\\nc) Nəqliyyat vasitəsi özgəninkiləşdirildikdə və ya dövlət qeydiyyat nişanı dəyişdikdə Tətbiqdəki məlumatları yeniləməlidir;\\nç) Tətbiqə yanlış məlumat daxil etməməli, Tətbiqdən sui-istifadə etməməli və onun işinə müdaxilə etməməlidir.\\n\\n4.4. İstifadəçi Tətbiqə başqa şəxsə məxsus FİN kod, Nəqliyyat vasitəsi və ya digər fərdi məlumatı yalnız həmin şəxsin razılığı əsasında daxil etməlidir. İstifadəçi belə razılığı əldə etdiyini təsdiq edir və bununla bağlı bütün məsuliyyəti daşıyır.\\n\\n4.5. Bu Sənədin şərtləri pozulduqda və ya saxtakarlıq şübhəsi yarandıqda CarCat İstifadəçinin hesabını dayandırmaq hüququna malikdir.\\n\\n4.6. İstifadəçi bu Sənədin şərtlərini pozması nəticəsində CarCat-a dəyən zərərə və üçüncü şəxslərin iddialarına görə məsuliyyət daşıyır."
                },
                {
                  "id": 5,
                  "title": "5. Məsuliyyətin məhdudlaşdırılması",
                  "content": "5.1. Xidmət təchizatçılarının və Tərəfdaşların göstərdiyi xidmətlərin keyfiyyətinə, bu zaman Nəqliyyat vasitəsinə dəyən zərərə və qiymətlə bağlı mübahisələrə görə məsuliyyəti müvafiq Xidmət təchizatçısı və ya Tərəfdaş daşıyır.\\n\\n5.2. Tətbiqdə göstərilən tarixlər və xatırlatmalar məlumat xarakteri daşıyır. Nəqliyyat vasitəsinin qanunvericiliyin tələblərinə uyğunluğuna görə məsuliyyət İstifadəçinin üzərinə düşür.\\n\\n5.3. Tətbiq \\"olduğu kimi\\" prinsipi əsasında təqdim olunur. Texniki işlər və ya nasazlıqlarla əlaqədar Tətbiqin işində müvəqqəti fasilələr ola bilər.\\n\\n5.4. Qanunvericiliyin icazə verdiyi həddə CarCat dolayı zərərə və əldən çıxmış faydaya görə məsuliyyət daşımır.\\n\\n5.5. Tətbiqdə əks olunan cərimə, sığorta, Nəqliyyat vasitəsinin tarixçəsi və digər məlumatlar dövlət qurumlarından, sığorta təşkilatlarından, açıq mənbələrdən və digər mənbələrdən əldə edilir. CarCat bu məlumatların düzgünlüyünə, tamlığına və aktuallığına görə məsuliyyət daşımır.\\n\\n5.6. Cərimə ödənişləri bank və ödəniş təşkilatları vasitəsilə həyata keçirilir. Ödənişin müvafiq dövlət informasiya sistemlərində əks olunmasında yaranan gecikmələrə görə CarCat məsuliyyət daşımır.\\n\\n5.7. CarCat sığortaçı deyil. Sığorta müqaviləsinin şərtləri, sığorta haqqı və sığorta hadisəsi üzrə ödəniş öhdəlikləri müvafiq sığorta təşkilatı tərəfindən müəyyən edilir və icra olunur."
                },
                {
                  "id": 6,
                  "title": "6. Əqli mülkiyyət",
                  "content": "6.1. Tətbiq, onun proqram təminatı, dizaynı, \\"CarCat\\" adı və loqosu, habelə məlumat bazaları CarCat-a məxsusdur və qanunvericiliklə qorunur.\\n\\n6.2. CarCat-ın yazılı razılığı olmadan Tətbiqin və onun məzmununun surətinin çıxarılması, dəyişdirilməsi, yayılması və kommersiya məqsədilə istifadəsi qadağandır."
                },
                {
                  "id": 11,
                  "title": "11. Fors-major",
                  "content": "11.1. CarCat təbii fəlakətlər, hərbi əməliyyatlar, dövlət orqanlarının qərarları, kiberhücumlar, rabitə və enerji təchizatında kəsintilər və digər qarşısıalınmaz qüvvə halları nəticəsində öhdəliklərinin icra edilməməsinə və ya lazımi qaydada icra edilməməsinə görə məsuliyyət daşımır."
                },
                {
                  "id": 12,
                  "title": "12. Yekun müddəalar",
                  "content": "12.1. CarCat bu Sənədə dəyişiklik etmək hüququna malikdir. Sənədin yeni redaksiyası Tətbiqdə dərc edildiyi andan qüvvəyə minir.\\n\\n12.2. Bu Sənəd Azərbaycan Respublikasının qanunvericiliyi ilə tənzimlənir. Tərəflər arasında yaranan mübahisələr danışıqlar yolu ilə həll edilmədikdə müvafiq məhkəmələrdə baxılır.\\n\\n12.3. Sənədin hər hansı müddəası etibarsız hesab edilərsə, bu, digər müddəaların qüvvədə olmasına təsir göstərmir.\\n\\n12.4. Sənəd Azərbaycan dilində tərtib edilmişdir. Digər dillərə tərcümə ilə ziddiyyət yarandıqda Azərbaycan dilindəki mətn üstün tutulur.\\n\\n12.5. Bu Sənədlə bağlı suallar üzrə info@carcat.app elektron poçt ünvanı vasitəsilə CarCat ilə əlaqə saxlamaq olar.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                    "email": "info@carcat.app",
                    "website": "https://carcat.app",
                    "location": "Bakı, Azərbaycan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "İstifadəçi Tətbiqdə qeydiyyatdan keçməklə bu Sənədin şərtlərini tam həcmdə qəbul etdiyini və fərdi məlumatlarının Sənəddə göstərilən qaydada işlənməsinə razılıq verdiyini təsdiq edir."
              }
            }
            """;

    private static final String TERMS_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "15 November 2026",
                "version": "4.0",
                "language": "en",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" Limited Liability Company",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Terms of Use",
                "subtitle": "Last updated: 15 November 2026 | Version: 4.0 | \\"CarCat\\" LLC"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. General provisions",
                  "content": "1.1. These Terms of use and Privacy policy (the \\"Document\\") govern the relationship between \\"CarCat\\" Limited Liability Company (\\"CarCat\\") and any person using the \\"CarCat\\" mobile application and digital platform (the \\"Application\\") (the \\"User\\"), and set out the rules for the collection, processing and protection of the User's personal data.\\n\\n1.2. By registering in the Application, the User confirms that they accept the terms of this Document in full and consent to the processing of their personal data as described herein.\\n\\n1.3. Personal data is processed in accordance with the legislation of the Republic of Azerbaijan."
                },
                {
                  "id": 2,
                  "title": "2. Company information",
                  "content": "2.1. \\"CarCat\\" LLC is the owner and operator of the Application and of the personal data information system:\\n• Legal name: \\"CarCat\\" Limited Liability Company\\n• Taxpayer ID (VÖEN): 1309963601\\n• State register: The information system \\"Digital Management System for Vehicle Maintenance Services (CarCat MİS)\\" was entered in the State Register of Personal Data Information Systems under No. İS647005001 on 31 July 2026 by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan.\\n• Address: Baku, Republic of Azerbaijan\\n• Email: info@carcat.app\\n• Phone: +994 70 575 75 70\\n• Website: https://carcat.app"
                },
                {
                  "id": 3,
                  "title": "3. Description of services",
                  "content": "3.1. The Application enables the User to manage services related to their car or other vehicle (the \\"Vehicle\\") on a single platform. CarCat does not itself provide maintenance, insurance or other services; it connects the User with service centres, insurance companies, other service suppliers and partners (\\"Service suppliers and Partners\\").\\n\\n3.2. The following services are offered through the Application:\\na) maintenance services: booking roadworthiness inspections, oil changes, repairs, diagnostics, pre-purchase inspections and other services at partner service centres;\\nb) CarCat Passport: checking the Vehicle's history by VIN and tracking its service history, mileage and technical alerts;\\nc) insurance services: obtaining insurance information and quotes from insurance companies and relevant government authorities, and arranging insurance contracts;\\nd) fines: obtaining information on fines imposed for administrative offences, based on the vehicle registration number and personal identification number (FIN), and paying those fines;\\ne) other services related to the vehicle ecosystem.\\n\\n3.3. The contract for any service ordered through the Application is concluded directly between the User and the relevant Service supplier or Partner. The User authorises CarCat to place the order and to transfer the data required for it to the Service supplier or Partner.\\n\\n3.4. Payments in the Application, including payment of fines, are processed by licensed banks and payment institutions.\\n\\n3.5. CarCat reserves the right to change, restrict or discontinue the Application's features and the range of services offered."
                },
                {
                  "id": 4,
                  "title": "4. Rights and obligations of the User",
                  "content": "4.1. The Application may be used by persons who have registered with a mobile number and who own, or have the right to use, the Vehicle they register.\\n\\n4.2. The Application is intended for personal use. Commercial use of the Application is permitted only under a separate agreement with CarCat.\\n\\n4.3. The User shall:\\na) keep their account details and verification codes confidential;\\nb) notify CarCat immediately if they lose control of their mobile number;\\nc) update the information in the Application if the Vehicle is sold or otherwise transferred, or if its registration number changes;\\nd) not enter false information into the Application, misuse the Application or interfere with its operation.\\n\\n4.4. The User may enter another person's FIN, Vehicle details or other personal data into the Application only with that person's consent. The User confirms that such consent has been obtained and bears full responsibility in this regard.\\n\\n4.5. CarCat may suspend the User's account if the terms of this Document are breached or fraud is suspected.\\n\\n4.6. The User is liable for any loss incurred by CarCat, and for any third-party claims, arising from the User's breach of this Document."
                },
                {
                  "id": 5,
                  "title": "5. Limitation of liability",
                  "content": "5.1. The relevant Service supplier or Partner is responsible for the quality of the services it provides, for any damage caused to the Vehicle in the course of those services and for any pricing disputes.\\n\\n5.2. Dates and reminders shown in the Application are for information purposes only. The User is responsible for ensuring that the Vehicle complies with legal requirements.\\n\\n5.3. The Application is provided \\"as is\\". Its operation may be temporarily interrupted due to maintenance or technical faults.\\n\\n5.4. To the extent permitted by law, CarCat shall not be liable for indirect losses or loss of profit.\\n\\n5.5. Information on fines, insurance, Vehicle history and other data shown in the Application is obtained from government authorities, insurance companies, open sources and other sources. CarCat is not responsible for the accuracy, completeness or timeliness of such information.\\n\\n5.6. Fines are paid through banks and payment institutions. CarCat is not liable for delays in a payment being reflected in the relevant government information systems.\\n\\n5.7. CarCat is not an insurer. The terms of an insurance contract, the premium and any obligation to pay out on an insured event are determined and performed by the relevant insurance company."
                },
                {
                  "id": 6,
                  "title": "6. Intellectual property",
                  "content": "6.1. The Application, its software and design, the \\"CarCat\\" name and logo, and its databases belong to CarCat and are protected by law.\\n\\n6.2. Copying, modifying, distributing or commercially using the Application or its content without CarCat's written consent is prohibited."
                },
                {
                  "id": 11,
                  "title": "11. Force majeure",
                  "content": "11.1. CarCat shall not be liable for any failure to perform, or improper performance of, its obligations resulting from natural disasters, military operations, decisions of government authorities, cyberattacks, communications or power outages, or other force majeure events."
                },
                {
                  "id": 12,
                  "title": "12. Final provisions",
                  "content": "12.1. CarCat may amend this Document. A new version of the Document takes effect upon its publication in the Application.\\n\\n12.2. This Document is governed by the legislation of the Republic of Azerbaijan. Disputes between the parties that cannot be resolved through negotiation shall be referred to the competent courts.\\n\\n12.3. If any provision of this Document is held invalid, the remaining provisions shall remain in force.\\n\\n12.4. This Document is drawn up in Azerbaijani. In the event of any inconsistency between the Azerbaijani text and a translation, the Azerbaijani text shall prevail.\\n\\n12.5. Questions regarding this Document may be sent to CarCat at info@carcat.app.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "\\"CarCat\\" Limited Liability Company",
                    "email": "info@carcat.app",
                    "website": "https://carcat.app",
                    "location": "Baku, Azerbaijan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "By registering in the Application, the User confirms that they accept the terms of this Document in full and consent to the processing of their personal data as described herein."
              }
            }
            """;

    private static final String TERMS_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "15 ноября 2026 г.",
                "version": "4.0",
                "language": "ru",
                "company": "ООО «CarCat»",
                "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Условия использования",
                "subtitle": "Дата последнего обновления: 15 ноября 2026 г. | Версия: 4.0 | ООО «CarCat»"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Общие положения",
                  "content": "1.1. Настоящие Условия использования и Политика конфиденциальности (далее – «Документ») регулируют отношения между Обществом с ограниченной ответственностью «CarCat» (далее – «CarCat») и лицом, использующим мобильное приложение и цифровую платформу «CarCat» (далее – «Приложение»; такое лицо далее – «Пользователь»), а также определяют порядок сбора, обработки и защиты персональных данных Пользователя.\\n\\n1.2. Регистрируясь в Приложении, Пользователь подтверждает, что принимает условия настоящего Документа в полном объеме и дает согласие на обработку своих персональных данных в порядке, предусмотренном Документом.\\n\\n1.3. Обработка персональных данных осуществляется в соответствии с законодательством Азербайджанской Республики."
                },
                {
                  "id": 2,
                  "title": "2. Сведения о компании",
                  "content": "2.1. Собственником и оператором Приложения и информационной системы персональных данных является ООО «CarCat»:\\n• Полное наименование: Общество с ограниченной ответственностью «CarCat»\\n• ИНН (VÖEN): 1309963601\\n• Государственный реестр: информационная система «Система электронного управления сервисным обслуживанием транспортных средств (CarCat MİS)» зарегистрирована Национальным агентством кибербезопасности при Министерстве цифрового развития и транспорта Азербайджанской Республики 31 июля 2026 года в Государственном реестре информационных систем персональных данных под номером İS647005001.\\n• Адрес: г. Баку, Азербайджанская Республика\\n• Электронная почта: info@carcat.app\\n• Телефон: +994 70 575 75 70\\n• Веб-сайт: https://carcat.app"
                },
                {
                  "id": 3,
                  "title": "3. Описание услуг",
                  "content": "3.1. Приложение позволяет Пользователю управлять услугами, связанными с его автомобилем или иным транспортным средством (далее – «Транспортное средство»), на единой платформе. CarCat не оказывает сервисные, страховые и иные услуги непосредственно, а связывает Пользователя с сервисными центрами, страховыми организациями, иными поставщиками услуг и партнерами (далее – «Поставщики услуг и Партнеры»).\\n\\n3.2. Через Приложение предоставляются следующие услуги:\\nа) сервисные услуги: бронирование технического осмотра, замены масла, ремонта, диагностики, проверки перед покупкой и иных сервисных услуг в партнерских сервисных центрах;\\nб) CarCat Passport: проверка истории Транспортного средства по VIN-коду, отслеживание истории обслуживания, пробега и технических уведомлений;\\nв) страховые услуги: получение страховой информации и предложений от страховых организаций и соответствующих государственных органов, оформление договоров страхования;\\nг) штрафы: получение сведений о штрафах, наложенных за административные правонарушения, по государственному регистрационному номеру и FIN-коду, а также их оплата;\\nд) иные услуги, связанные с экосистемой транспортных средств.\\n\\n3.3. Договор на услугу, заказанную через Приложение, заключается непосредственно между Пользователем и соответствующим Поставщиком услуг или Партнером. Пользователь уполномочивает CarCat оформить заказ и передать необходимые для этого данные Поставщику услуг или Партнеру.\\n\\n3.4. Платежи в Приложении, в том числе оплата штрафов, осуществляются через лицензированные банки и платежные организации.\\n\\n3.5. CarCat вправе изменять, ограничивать или прекращать функции Приложения и состав предоставляемых услуг."
                },
                {
                  "id": 4,
                  "title": "4. Права и обязанности Пользователя",
                  "content": "4.1. Приложением могут пользоваться лица, зарегистрировавшиеся по номеру мобильного телефона и являющиеся собственниками регистрируемого Транспортного средства либо имеющие право на его использование.\\n\\n4.2. Приложение предназначено для личного использования. Использование Приложения в коммерческих целях допускается только на основании отдельного соглашения с CarCat.\\n\\n4.3. Пользователь обязан:\\nа) обеспечивать конфиденциальность данных учетной записи и кодов подтверждения;\\nб) незамедлительно сообщить CarCat об утрате контроля над номером мобильного телефона;\\nв) обновить сведения в Приложении при отчуждении Транспортного средства или изменении государственного регистрационного номера;\\nг) не вводить в Приложение недостоверные сведения, не злоупотреблять Приложением и не вмешиваться в его работу.\\n\\n4.4. Пользователь вправе вводить в Приложение FIN-код, данные о Транспортном средстве или иные персональные данные другого лица только с согласия этого лица. Пользователь подтверждает получение такого согласия и несет всю связанную с этим ответственность.\\n\\n4.5. CarCat вправе приостановить учетную запись Пользователя при нарушении условий настоящего Документа или подозрении в мошенничестве.\\n\\n4.6. Пользователь несет ответственность за ущерб, причиненный CarCat в результате нарушения им условий настоящего Документа, а также по связанным с этим претензиям третьих лиц."
                },
                {
                  "id": 5,
                  "title": "5. Ограничение ответственности",
                  "content": "5.1. Ответственность за качество услуг, оказываемых Поставщиками услуг и Партнерами, за ущерб, причиненный при этом Транспортному средству, и за споры о цене несет соответствующий Поставщик услуг или Партнер.\\n\\n5.2. Даты и напоминания, отображаемые в Приложении, носят информационный характер. Ответственность за соответствие Транспортного средства требованиям законодательства несет Пользователь.\\n\\n5.3. Приложение предоставляется по принципу «как есть». В работе Приложения возможны временные перерывы в связи с техническими работами или сбоями.\\n\\n5.4. В пределах, допускаемых законодательством, CarCat не несет ответственности за косвенный ущерб и упущенную выгоду.\\n\\n5.5. Сведения о штрафах, страховании, истории Транспортного средства и иные данные, отображаемые в Приложении, получаются от государственных органов, страховых организаций, из открытых и иных источников. CarCat не несет ответственности за достоверность, полноту и актуальность таких сведений.\\n\\n5.6. Оплата штрафов осуществляется через банки и платежные организации. CarCat не несет ответственности за задержки в отражении платежа в соответствующих государственных информационных системах.\\n\\n5.7. CarCat не является страховщиком. Условия договора страхования, размер страховой премии и обязательства по выплате при наступлении страхового случая определяются и исполняются соответствующей страховой организацией."
                },
                {
                  "id": 6,
                  "title": "6. Интеллектуальная собственность",
                  "content": "6.1. Приложение, его программное обеспечение и дизайн, наименование и логотип «CarCat», а также базы данных принадлежат CarCat и охраняются законодательством.\\n\\n6.2. Копирование, изменение, распространение и коммерческое использование Приложения и его содержимого без письменного согласия CarCat запрещены."
                },
                {
                  "id": 11,
                  "title": "11. Форс-мажор",
                  "content": "11.1. CarCat не несет ответственности за неисполнение или ненадлежащее исполнение своих обязательств вследствие стихийных бедствий, военных действий, решений государственных органов, кибератак, перебоев в связи и энергоснабжении и иных обстоятельств непреодолимой силы."
                },
                {
                  "id": 12,
                  "title": "12. Заключительные положения",
                  "content": "12.1. CarCat вправе вносить изменения в настоящий Документ. Новая редакция Документа вступает в силу с момента ее опубликования в Приложении.\\n\\n12.2. Настоящий Документ регулируется законодательством Азербайджанской Республики. Споры между сторонами, не урегулированные путем переговоров, рассматриваются в соответствующих судах.\\n\\n12.3. Признание какого-либо положения настоящего Документа недействительным не влияет на действительность остальных положений.\\n\\n12.4. Документ составлен на азербайджанском языке. В случае расхождений между текстом на азербайджанском языке и его переводом на другие языки преимущественную силу имеет текст на азербайджанском языке.\\n\\n12.5. По вопросам, связанным с настоящим Документом, можно обратиться в CarCat по адресу электронной почты info@carcat.app.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                    "email": "info@carcat.app",
                    "website": "https://carcat.app",
                    "location": "Баку, Азербайджан"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "Регистрируясь в Приложении, Пользователь подтверждает, что принимает условия настоящего Документа в полном объеме и дает согласие на обработку своих персональных данных в порядке, предусмотренном Документом."
              }
            }
            """;

    // -------------------------------------------------------------------------
    // Privacy Policy v4.0 — AZ / EN / RU
    // Sections 1, 2, 7–10, 12 of the combined document.
    // -------------------------------------------------------------------------

    private static final String POLICY_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "15 noyabr 2026",
                "version": "4.0",
                "language": "az",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "phone": "+994 70 575 75 70",
                "responseTime": ""
              },
              "header": {
                "title": "CarCat — Məxfilik siyasəti",
                "subtitle": "Son yenilənmə: 15 noyabr 2026 | Versiya: 4.0 | \\"CarCat\\" MMC"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Ümumi müddəalar",
                  "content": "1.1. Bu İstifadə şərtləri və Məxfilik siyasəti (bundan sonra \\"Sənəd\\") \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti (bundan sonra \\"CarCat\\") ilə \\"CarCat\\" mobil tətbiqindən və rəqəmsal platformasından (bundan sonra \\"Tətbiq\\") istifadə edən şəxs (bundan sonra \\"İstifadəçi\\") arasında yaranan münasibətləri, habelə İstifadəçinin fərdi məlumatlarının toplanması, işlənməsi və qorunması qaydalarını müəyyən edir.\\n\\n1.2. İstifadəçi Tətbiqdə qeydiyyatdan keçməklə bu Sənədin şərtlərini tam həcmdə qəbul etdiyini və fərdi məlumatlarının Sənəddə göstərilən qaydada işlənməsinə razılıq verdiyini təsdiq edir.\\n\\n1.3. Fərdi məlumatların işlənməsi Azərbaycan Respublikasının qanunvericiliyinə uyğun olaraq həyata keçirilir.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Şirkət haqqında məlumat",
                  "content": "2.1. Tətbiqin və fərdi məlumatların informasiya sisteminin mülkiyyətçisi və operatoru \\"CarCat\\" MMC-dir:\\n• Hüquqi şəxsin adı: \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• Dövlət reyestri: \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CarCat MİS)\\" informasiya sistemi Azərbaycan Respublikası Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• Ünvan: Bakı şəhəri, Azərbaycan Respublikası\\n• Elektron poçt: info@carcat.app\\n• Telefon: +994 70 575 75 70\\n• Veb-sayt: https://carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Toplanılan fərdi məlumatlar",
                  "content": "7.1. CarCat aşağıdakı məlumatları toplayır:\\na) şəxsiyyəti müəyyən edən məlumatlar: ad, soyad, şəxsiyyət vəsiqəsinin FİN kodu, mobil telefon nömrəsi, elektron poçt ünvanı və digər məlumatlar;\\nb) Nəqliyyat vasitəsinə dair məlumatlar: VIN kod, dövlət qeydiyyat nişanı, marka, model, buraxılış ili, yürüş, xidmət və təmir tarixçəsi, sığorta, cərimə və digər məlumatlar;\\nc) geolokasiya və digər məkan məlumatları: İstifadəçinin icazəsi ilə, yaxınlıqdakı servis mərkəzlərinin müəyyən edilməsi və digər xidmətlərin göstərilməsi məqsədilə;\\nç) texniki məlumatlar: IP ünvanı, cihazın modeli, əməliyyat sisteminin versiyası, cihaz identifikatorları, istifadə qeydləri, xəta hesabatları və digər məlumatlar.\\n\\n7.2. Bank kartı məlumatları bilavasitə bank və ödəniş təşkilatları tərəfindən emal olunur və CarCat tərəfindən saxlanılmır.\\n\\n7.3. Qanunvericiliklə müəyyən edilmiş məhdudiyyətlər nəzərə alınmaqla, qanunvericiliklə nəzərdə tutulmuş hallarda CarCat İstifadəçiyə və onun Nəqliyyat vasitəsinə dair məlumatları xidmət təminatçılarından, sığorta təşkilatlarından, müvafiq dövlət qurumlarından, bank və ödəniş təşkilatlarından və digər mənbələrdən əldə edə bilər. Bu məlumatlar İstifadəçiyə daha dolğun məlumat təqdim edilməsi məqsədilə istifadə olunur.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Məlumatların işlənməsi məqsədləri",
                  "content": "8.1. Fərdi məlumatlar aşağıdakı məqsədlərlə işlənir:\\na) Tətbiq vasitəsilə xidmətlərin göstərilməsi və Nəqliyyat vasitəsinin rəqəmsal servis pasportunun formalaşdırılması;\\nb) Xidmət təchizatçıları və Tərəfdaşlar vasitəsilə bronların, sığorta müqavilələrinin rəsmiləşdirilməsinin və cərimə ödənişlərinin həyata keçirilməsi;\\nc) texniki baxış, sığorta, yürüş və servis xatırlatmalarının göndərilməsi;\\nç) Tətbiqin təhlükəsizliyinin təmin edilməsi və saxtakarlığın qarşısının alınması;\\nd) İstifadəçi müraciətlərinin cavablandırılması;\\ne) qanunvericilikdən irəli gələn öhdəliklərin icrası.\\n\\n8.2. İstifadəçi Tətbiqdə qeydiyyatdan keçməklə xidmətlərlə bağlı SMS, push və elektron poçt bildirişlərinin alınmasına razılıq verir.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Məlumatların ötürülməsi",
                  "content": "9.1. CarCat fərdi məlumatları bu Sənəddə və qanunvericilikdə nəzərdə tutulmuş hallar istisna olmaqla üçüncü şəxslərə ötürmür.\\n\\n9.2. Fərdi məlumatlar yalnız zəruri həcmdə aşağıdakı şəxslərə ötürülə bilər:\\na) İstifadəçinin seçdiyi xidməti göstərən Xidmət təchizatçısına və ya Tərəfdaşa;\\nb) sığorta təşkilatlarına, müvafiq dövlət qurumlarına, bank və ödəniş təşkilatlarına, İstifadəçinin sifariş etdiyi xidmətin icrası üçün;\\nc) Tətbiqlə inteqrasiya olunmuş digər tərəfdaşlara;\\nç) server, bulud saxlama və bildiriş xidmətlərini təmin edən informasiya texnologiyaları təminatçılarına, məxfilik öhdəlikləri çərçivəsində;\\nd) qanunvericiliklə nəzərdə tutulmuş hallarda səlahiyyətli dövlət orqanlarına.\\n\\n9.3. Fərdi məlumatlar qanunvericiliyin tələblərinə uyğun olaraq xarici (transsərhəd) bulud serverlərində saxlanıla və işlənə bilər.",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Məlumatların qorunması",
                  "content": "10.1. CarCat fərdi məlumatların qanunsuz əldə edilməsinin, dəyişdirilməsinin və yayılmasının qarşısını almaq üçün zəruri texniki və təşkilati tədbirlər görür.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 12,
                  "title": "12. Yekun müddəalar",
                  "content": "12.1. CarCat bu Sənədə dəyişiklik etmək hüququna malikdir. Sənədin yeni redaksiyası Tətbiqdə dərc edildiyi andan qüvvəyə minir.\\n\\n12.2. Bu Sənəd Azərbaycan Respublikasının qanunvericiliyi ilə tənzimlənir. Tərəflər arasında yaranan mübahisələr danışıqlar yolu ilə həll edilmədikdə müvafiq məhkəmələrdə baxılır.\\n\\n12.3. Sənədin hər hansı müddəası etibarsız hesab edilərsə, bu, digər müddəaların qüvvədə olmasına təsir göstərmir.\\n\\n12.4. Sənəd Azərbaycan dilində tərtib edilmişdir. Digər dillərə tərcümə ilə ziddiyyət yarandıqda Azərbaycan dilindəki mətn üstün tutulur.\\n\\n12.5. Bu Sənədlə bağlı suallar üzrə info@carcat.app elektron poçt ünvanı vasitəsilə CarCat ilə əlaqə saxlamaq olar.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Üçüncü şəxslərə ötürülmə",
                "content": "CarCat fərdi məlumatları bu Sənəddə və qanunvericilikdə nəzərdə tutulmuş hallar istisna olmaqla üçüncü şəxslərə ötürmür.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 13,
                "title": "Əlaqə",
                "subtitle": "Bu Sənədlə bağlı suallar üzrə info@carcat.app elektron poçt ünvanı vasitəsilə CarCat ilə əlaqə saxlamaq olar.",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Bakı, Azərbaycan",
                "phone": "+994 70 575 75 70",
                "responseNote": ""
              },
              "footer": {
                "consentText": "İstifadəçi Tətbiqdə qeydiyyatdan keçməklə bu Sənədin şərtlərini tam həcmdə qəbul etdiyini və fərdi məlumatlarının Sənəddə göstərilən qaydada işlənməsinə razılıq verdiyini təsdiq edir."
              }
            }
            """;

    private static final String POLICY_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "15 November 2026",
                "version": "4.0",
                "language": "en",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" Limited Liability Company",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "phone": "+994 70 575 75 70",
                "responseTime": ""
              },
              "header": {
                "title": "CarCat — Privacy Policy",
                "subtitle": "Last updated: 15 November 2026 | Version: 4.0 | \\"CarCat\\" LLC"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. General provisions",
                  "content": "1.1. These Terms of use and Privacy policy (the \\"Document\\") govern the relationship between \\"CarCat\\" Limited Liability Company (\\"CarCat\\") and any person using the \\"CarCat\\" mobile application and digital platform (the \\"Application\\") (the \\"User\\"), and set out the rules for the collection, processing and protection of the User's personal data.\\n\\n1.2. By registering in the Application, the User confirms that they accept the terms of this Document in full and consent to the processing of their personal data as described herein.\\n\\n1.3. Personal data is processed in accordance with the legislation of the Republic of Azerbaijan.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Company information",
                  "content": "2.1. \\"CarCat\\" LLC is the owner and operator of the Application and of the personal data information system:\\n• Legal name: \\"CarCat\\" Limited Liability Company\\n• Taxpayer ID (VÖEN): 1309963601\\n• State register: The information system \\"Digital Management System for Vehicle Maintenance Services (CarCat MİS)\\" was entered in the State Register of Personal Data Information Systems under No. İS647005001 on 31 July 2026 by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan.\\n• Address: Baku, Republic of Azerbaijan\\n• Email: info@carcat.app\\n• Phone: +994 70 575 75 70\\n• Website: https://carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Personal data collected",
                  "content": "7.1. CarCat collects the following data:\\na) identification data: first name, surname, ID card FIN, mobile phone number, email address and other data;\\nb) Vehicle data: VIN, registration number, make, model, year of manufacture, mileage, service and repair history, insurance, fines and other data;\\nc) geolocation and other location data: with the User's permission, to identify nearby service centres and to provide other services;\\nd) technical data: IP address, device model, operating system version, device identifiers, usage logs, error reports and other data.\\n\\n7.2. Bank card details are processed directly by banks and payment institutions and are not stored by CarCat.\\n\\n7.3. Subject to the restrictions established by law, and in the cases provided for by law, CarCat may obtain data about the User and their Vehicle from service providers, insurance companies, relevant government authorities, banks and payment institutions, and other sources. Such data is used to provide the User with more complete information.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Purposes of processing",
                  "content": "8.1. Personal data is processed for the following purposes:\\na) providing services through the Application and creating the Vehicle's digital service passport;\\nb) making bookings, arranging insurance contracts and processing fine payments through Service suppliers and Partners;\\nc) sending reminders about roadworthiness inspections, insurance, mileage and servicing;\\nd) ensuring the security of the Application and preventing fraud;\\ne) responding to User enquiries;\\nf) complying with legal obligations.\\n\\n8.2. By registering in the Application, the User consents to receiving service-related SMS, push and email notifications.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Transfer of data",
                  "content": "9.1. CarCat does not transfer personal data to third parties except in the cases provided for in this Document and by law.\\n\\n9.2. Personal data may be transferred, only to the extent necessary, to:\\na) the Service supplier or Partner providing the service selected by the User;\\nb) insurance companies, relevant government authorities, banks and payment institutions, in order to perform the service ordered by the User;\\nc) other partners integrated with the Application;\\nd) information technology providers supplying server, cloud storage and notification services, subject to confidentiality obligations;\\ne) competent government authorities, in the cases provided for by law.\\n\\n9.3. Personal data may be stored and processed on foreign (cross-border) cloud servers in accordance with the requirements of law.",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Data protection",
                  "content": "10.1. CarCat takes the necessary technical and organisational measures to prevent unlawful access to, alteration or disclosure of personal data.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 12,
                  "title": "12. Final provisions",
                  "content": "12.1. CarCat may amend this Document. A new version of the Document takes effect upon its publication in the Application.\\n\\n12.2. This Document is governed by the legislation of the Republic of Azerbaijan. Disputes between the parties that cannot be resolved through negotiation shall be referred to the competent courts.\\n\\n12.3. If any provision of this Document is held invalid, the remaining provisions shall remain in force.\\n\\n12.4. This Document is drawn up in Azerbaijani. In the event of any inconsistency between the Azerbaijani text and a translation, the Azerbaijani text shall prevail.\\n\\n12.5. Questions regarding this Document may be sent to CarCat at info@carcat.app.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Transfer to third parties",
                "content": "CarCat does not transfer personal data to third parties except in the cases provided for in this Document and by law.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 13,
                "title": "Contact",
                "subtitle": "Questions regarding this Document may be sent to CarCat at info@carcat.app.",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" Limited Liability Company",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Baku, Azerbaijan",
                "phone": "+994 70 575 75 70",
                "responseNote": ""
              },
              "footer": {
                "consentText": "By registering in the Application, the User confirms that they accept the terms of this Document in full and consent to the processing of their personal data as described herein."
              }
            }
            """;

    private static final String POLICY_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "15 ноября 2026 г.",
                "version": "4.0",
                "language": "ru",
                "company": "ООО «CarCat»",
                "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "phone": "+994 70 575 75 70",
                "responseTime": ""
              },
              "header": {
                "title": "CarCat — Политика конфиденциальности",
                "subtitle": "Дата последнего обновления: 15 ноября 2026 г. | Версия: 4.0 | ООО «CarCat»"
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Общие положения",
                  "content": "1.1. Настоящие Условия использования и Политика конфиденциальности (далее – «Документ») регулируют отношения между Обществом с ограниченной ответственностью «CarCat» (далее – «CarCat») и лицом, использующим мобильное приложение и цифровую платформу «CarCat» (далее – «Приложение»; такое лицо далее – «Пользователь»), а также определяют порядок сбора, обработки и защиты персональных данных Пользователя.\\n\\n1.2. Регистрируясь в Приложении, Пользователь подтверждает, что принимает условия настоящего Документа в полном объеме и дает согласие на обработку своих персональных данных в порядке, предусмотренном Документом.\\n\\n1.3. Обработка персональных данных осуществляется в соответствии с законодательством Азербайджанской Республики.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Сведения о компании",
                  "content": "2.1. Собственником и оператором Приложения и информационной системы персональных данных является ООО «CarCat»:\\n• Полное наименование: Общество с ограниченной ответственностью «CarCat»\\n• ИНН (VÖEN): 1309963601\\n• Государственный реестр: информационная система «Система электронного управления сервисным обслуживанием транспортных средств (CarCat MİS)» зарегистрирована Национальным агентством кибербезопасности при Министерстве цифрового развития и транспорта Азербайджанской Республики 31 июля 2026 года в Государственном реестре информационных систем персональных данных под номером İS647005001.\\n• Адрес: г. Баку, Азербайджанская Республика\\n• Электронная почта: info@carcat.app\\n• Телефон: +994 70 575 75 70\\n• Веб-сайт: https://carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Собираемые персональные данные",
                  "content": "7.1. CarCat собирает следующие данные:\\nа) идентификационные данные: имя, фамилия, FIN-код удостоверения личности, номер мобильного телефона, адрес электронной почты и иные данные;\\nб) данные о Транспортном средстве: VIN-код, государственный регистрационный номер, марка, модель, год выпуска, пробег, история обслуживания и ремонта, сведения о страховании, штрафах и иные данные;\\nв) данные геолокации и иные данные о местоположении: с разрешения Пользователя, для определения ближайших сервисных центров и оказания иных услуг;\\nг) технические данные: IP-адрес, модель устройства, версия операционной системы, идентификаторы устройства, журналы использования, отчеты об ошибках и иные данные.\\n\\n7.2. Данные банковских карт обрабатываются непосредственно банками и платежными организациями и не хранятся CarCat.\\n\\n7.3. С учетом ограничений, установленных законодательством, и в случаях, предусмотренных законодательством, CarCat может получать данные о Пользователе и его Транспортном средстве от поставщиков услуг, страховых организаций, соответствующих государственных органов, банков и платежных организаций, а также из иных источников. Эти данные используются для предоставления Пользователю более полной информации.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Цели обработки данных",
                  "content": "8.1. Персональные данные обрабатываются в следующих целях:\\nа) оказание услуг через Приложение и формирование цифрового сервисного паспорта Транспортного средства;\\nб) осуществление бронирований, оформление договоров страхования и оплата штрафов через Поставщиков услуг и Партнеров;\\nв) направление напоминаний о техническом осмотре, страховании, пробеге и обслуживании;\\nг) обеспечение безопасности Приложения и предотвращение мошенничества;\\nд) рассмотрение обращений Пользователей;\\nе) исполнение обязательств, предусмотренных законодательством.\\n\\n8.2. Регистрируясь в Приложении, Пользователь дает согласие на получение связанных с услугами SMS, push-уведомлений и сообщений по электронной почте.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Передача данных",
                  "content": "9.1. CarCat не передает персональные данные третьим лицам, за исключением случаев, предусмотренных настоящим Документом и законодательством.\\n\\n9.2. Персональные данные могут передаваться только в необходимом объеме следующим лицам:\\nа) Поставщику услуг или Партнеру, оказывающему выбранную Пользователем услугу;\\nб) страховым организациям, соответствующим государственным органам, банкам и платежным организациям – для исполнения заказанной Пользователем услуги;\\nв) иным партнерам, интегрированным с Приложением;\\nг) поставщикам информационных технологий, обеспечивающим услуги серверов, облачного хранения и уведомлений, – в рамках обязательств о конфиденциальности;\\nд) уполномоченным государственным органам – в случаях, предусмотренных законодательством.\\n\\n9.3. Персональные данные могут храниться и обрабатываться на зарубежных (трансграничных) облачных серверах в соответствии с требованиями законодательства.",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Защита данных",
                  "content": "10.1. CarCat принимает необходимые технические и организационные меры для предотвращения неправомерного доступа к персональным данным, их изменения и распространения.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 12,
                  "title": "12. Заключительные положения",
                  "content": "12.1. CarCat вправе вносить изменения в настоящий Документ. Новая редакция Документа вступает в силу с момента ее опубликования в Приложении.\\n\\n12.2. Настоящий Документ регулируется законодательством Азербайджанской Республики. Споры между сторонами, не урегулированные путем переговоров, рассматриваются в соответствующих судах.\\n\\n12.3. Признание какого-либо положения настоящего Документа недействительным не влияет на действительность остальных положений.\\n\\n12.4. Документ составлен на азербайджанском языке. В случае расхождений между текстом на азербайджанском языке и его переводом на другие языки преимущественную силу имеет текст на азербайджанском языке.\\n\\n12.5. По вопросам, связанным с настоящим Документом, можно обратиться в CarCat по адресу электронной почты info@carcat.app.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Передача третьим лицам",
                "content": "CarCat не передает персональные данные третьим лицам, за исключением случаев, предусмотренных настоящим Документом и законодательством.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 13,
                "title": "Контакты",
                "subtitle": "По вопросам, связанным с настоящим Документом, можно обратиться в CarCat по адресу электронной почты info@carcat.app.",
                "company": "ООО «CarCat»",
                "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                "email": "info@carcat.app",
                "website": "https://carcat.app",
                "location": "Баку, Азербайджан",
                "phone": "+994 70 575 75 70",
                "responseNote": ""
              },
              "footer": {
                "consentText": "Регистрируясь в Приложении, Пользователь подтверждает, что принимает условия настоящего Документа в полном объеме и дает согласие на обработку своих персональных данных в порядке, предусмотренном Документом."
              }
            }
            """;
}
