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
 *     Metin kaynağı: CRCT-214 (Terms v3.3, Privacy v3.5 — CARCAT MMC). Privacy RU backend çevirisidir.
 * en: Legal content REST controller; serves terms and privacy policy in az/en/ru.
 *     Source: CRCT-214 (Terms v3.3, Privacy v3.5 — CARCAT LLC). Privacy RU is a backend translation.
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
    // Terms of Use v3.3 — AZ / EN / RU (CRCT-214)
    // -------------------------------------------------------------------------

    private static final String TERMS_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "5 Sentyabr 2026",
                "version": "3.3",
                "language": "az",
                "company": "CARCAT MMC",
                "companyAz": "\\"CARCAT\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — İstifadə Şərtləri",
                "subtitle": "Tətbiqdən istifadə bu Şərtlərə tabedir. Şəxsi istifadə üçün nəzərdə tutulub. Üçüncü tərəf xidmətlərində CarCat nümayəndə (agent) kimi çıxış edir. Tətbiq \\"olduğu kimi\\" təqdim olunur."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Giriş",
                  "content": "Bu İstifadə Şərtləri (\\"Şərtlər\\") \\"CARCAT\\" MMC (\\"CarCat\\", \\"biz\\") ilə istifadəçi (\\"siz\\") arasındakı hüquqi münasibətləri tənzimləyir. Tətbiqdən istifadə etməklə bu Şərtləri və Məxfilik Siyasətimizi qəbul etmiş olursunuz. CarCat bu Şərtləri istənilən vaxt birtərəfli qaydada dəyişdirmək hüququnu saxlayır."
                },
                {
                  "id": 2,
                  "title": "2. Biz Kimik? (Hüquqi və Dövlət Qeydiyyatı Məlumatları)",
                  "content": "Biz Azərbaycan Respublikasının qanunvericiliyinə uyğun olaraq \\"bir pəncərə\\" prinsipi ilə dövlət qeydiyyatına alınmış \\"CARCAT\\" Məhdud Məsuliyyətli Cəmiyyəti-yik.\\n\\n• Hüquqi Şəxsin Tam Adı: \\"CARCAT\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• İnformasiya Sisteminin Dövlət Reyestr Qeydiyyatı: CarCat tətbiqinin informasiya sistemi — \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CARCAT MİS)\\" AR Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Veb-sayt: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. Tətbiq Necə İşləyir?",
                  "content": "CarCat avtomobilinizlə bağlı bütün işləri tək məkandan idarə etməyə imkan verən müstəqil rəqəmsal platformadır.\\n\\nTətbiq vasitəsilə üçüncü tərəf Xidmət Göstəricisindən xidmət aldıqda, CarCat-a həmin xidməti sizin adınızdan almaq və məlumatlarınızı (o cümlədən avtomobil və əlaqə məlumatlarını) təchizatçıya ötürmək və ya inteqrasiya çərçivəsində mübadilə etmək səlahiyyəti (agentlik) verirsiniz. Yaranan müqavilə birbaşa Siz və Xidmət Göstəricisi arasında bağlanır. Xidmətin icrası zamanı ortaya çıxan problemlərə görə birbaşa xidmət göstərən tərəflə əlaqə saxlanılmalıdır."
                },
                {
                  "id": 4,
                  "title": "4. Xidmətlərimiz",
                  "content": "CarCat tətbiqi daxilində aşağıdakı modulları və xidmətləri təqdim edir:\\n\\n• CarCat Maintain (Servis və Təmir): Tərəfdaş servis mərkəzlərində texniki baxış, yağdəyişmə və təmir xidmətlərini bron etmək.\\n• CarCat Passport (Avtomobil Pasportu): VIN kod vasitəsilə avtomobilin xidmət tarixçəsini, yürüşünü və texniki xəbərdarlıqlarını izləmək."
                },
                {
                  "id": 5,
                  "title": "5. Kimlər İstifadə Edə Bilər?",
                  "content": "Tətbiqdən istifadə etmək üçün aşağıdakı şərtlərə cavab verməlisiniz:\\n\\n• Tətbiqdə qeydiyyat mobil nömrə vasitəsilə aparıldığı üçün yalnız özünüzə məxsus qanuni mobil nömrədən istifadə etmək;\\n• Tətbiqdən yalnız şəxsi (qeyri-kommersiya) məqsədlər üçün istifadə etmək;\\n• Qeydiyyata aldığınız avtomobilin qanuni mülkiyyətçisi olmaq və ya mülkiyyətçidən açıq icazə almaq."
                },
                {
                  "id": 6,
                  "title": "6. Məsuliyyətin Məhdudlaşdırılması (Liability Disclaimer)",
                  "content": "• Xidmət Keyfiyyəti: Üçüncü tərəf servis mərkəzlərinin göstərdiyi təmirlərin keyfiyyətinə, avtomobilə dəyən zərərə və ya qiymət mübahisələrinə görə CarCat məsuliyyət daşımır.\\n• Dəqiqlik: Tətbiqdə avtomatik xatırladılan texniki baxış və ya yeniləmə tarixləri məlumat xarakterlidir. Avtomobilinizin qanuni tələblərə uyğunluğuna görə cavabdehlik tam olaraq sizin üzərinizdədir.\\n• Maliyyə Məhdudiyyəti: Qanunla icazə verilən maksimum həddə, CarCat-ın sizə vurduğu zərərə görə ümumi maliyyə məsuliyyəti 1 AZN məbləği ilə məhdudlaşır."
                },
                {
                  "id": 7,
                  "title": "7. Hesabın İdarə Edilməsi və Təhlükəsizlik",
                  "content": "• Siz hesab məlumatlarınızın məxfiliyini qorumağa cavabdehsiniz.\\n• Qeydiyyatda olan mobil nömrənizə sahibliyi və ya nəzarəti itirdiyiniz halda, hesabınızı dərhal silməlisiniz.\\n• Avtomobil satıldıqda və ya dövlət nömrə nişanı dəyişdikdə tətbiqdəki məlumatları güncəlləməlisiniz.\\n• CarCat şərtlər pozulduqda və ya saxtakarlıq şübhəsi olduqda hesabı xəbərdarlıq etmədən dayandırmaq hüququnu saxlayır."
                },
                {
                  "id": 8,
                  "title": "8. Məlumatların İşlənməsi, İnteqrasiya və Transsərhəd Ötürülməsi",
                  "content": "Tətbiqdən istifadə etməklə və ya qeydiyyatdan keçməklə xidmətlərin icrası və sistem inteqrasiyaları zamanı məlumatlarınızın üçüncü tərəf tərəfdaşlarla mübadilə edilməsinə, habelə xarici serverlərdə təhlükəsiz saxlanılmasına razılıq verirsiniz."
                },
                {
                  "id": 9,
                  "title": "9. Şikayətlər və Dəstək",
                  "content": "Tətbiqlə bağlı hər hansı sual, şikayət və ya təklifiniz olduqda aşağıdakı kanallarla bizimlə əlaqə saxlaya bilərsiniz:\\n\\n• Tətbiq daxilində: Çat dəstək bölməsi\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Əlaqə nömrəsi: +994 70 575 75 70\\n\\nSorğularınıza 5 iş günü ərzində ilkin cavab verilir və maksimum 30 gün müddətində tam həll edilir."
                },
                {
                  "id": 10,
                  "title": "10. Qanunvericilik və Məhkəmə Aiddiyyəti",
                  "content": "Bu Şərtlər Azərbaycan Respublikasının qanunvericiliyi ilə tənzimlənir. Yaranan mübahisələr tərəflər arasında həll edilmədikdə Bakı şəhərinin müvafiq məhkəmələri tərəfindən baxılır."
                },
                {
                  "id": 11,
                  "title": "11. Şərtlərin Birtərəfli Dəyişdirilməsi",
                  "content": "CarCat bu İstifadə Şərtlərini istənilən vaxt birtərəfli qaydada dəyişdirmək və ya yeniləmək hüququnu saxlayır. Yenilənmiş Şərtlər Tətbiqdə dərc edildiyi andan qüvvəyə minir. Dəyişikliklərdən sonra Tətbiqdən istifadəyə davam etməyiniz yenilənmiş Şərtləri qəbul etdiyiniz anlamına gəlir.",
                  "contact": {
                    "companyEn": "CARCAT LLC",
                    "companyAz": "\\"CARCAT\\" MMC",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Bakı, Azərbaycan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "CarCat istifadə etməklə bu İstifadə Şərtlərini oxuduğunuzu, başa düşdüyünüzü və qəbul etdiyinizi təsdiq edirsiniz."
              }
            }
            """;

    private static final String TERMS_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "September 5, 2026",
                "version": "3.3",
                "language": "en",
                "company": "CARCAT LLC",
                "companyAz": "\\"CARCAT\\" Limited Liability Company",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Terms of Use",
                "subtitle": "Use of the Application is subject to these Terms. For personal use only. For third-party services CarCat acts as an agent. The Application is provided \\"as is\\"."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Introduction",
                  "content": "These Terms of Use (\\"Terms\\") govern the legal relationship between \\"CARCAT\\" LLC (\\"CarCat\\", \\"we\\") and the user (\\"you\\"). By using the Application, you accept these Terms and our Privacy Policy. CarCat reserves the right to unilaterally modify these Terms at any time."
                },
                {
                  "id": 2,
                  "title": "2. Who We Are (Legal and State Registration Details)",
                  "content": "We are \\"CARCAT\\" Limited Liability Company, state-registered under the \\"one-stop shop\\" principle in accordance with the legislation of the Republic of Azerbaijan.\\n\\n• Full Legal Name: \\"CARCAT\\" Limited Liability Company\\n• TIN (VÖEN): 1309963601\\n• Information System State Register Registration: The information system of the CarCat application — \\"Electronic Management System for Vehicle Maintenance Services (CARCAT MIS)\\" was registered in the State Register of Personal Data Information Systems by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan on July 31, 2026 under No. İS647005001.\\n• Email: nemat.mirzayev@carcat.app\\n• Website: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. How the Application Works",
                  "content": "CarCat is an independent digital platform that allows you to manage all vehicle-related tasks in one place.\\n\\nWhen you receive a service from a third-party Service Provider through the Application, you grant CarCat the authority (agency) to obtain that service on your behalf and to transfer or exchange your data (including vehicle and contact details) with the provider within the framework of integration. The resulting agreement is concluded directly between You and the Service Provider. Any issues arising during service execution must be resolved directly with the service provider."
                },
                {
                  "id": 4,
                  "title": "4. Our Services",
                  "content": "The CarCat application provides the following modules and services:\\n\\n• CarCat Maintain (Service & Repair): Booking technical inspection, oil change, and repair services at partner service centers.\\n• CarCat Passport (Vehicle Passport): Tracking vehicle service history, mileage, and technical alerts via VIN code."
                },
                {
                  "id": 5,
                  "title": "5. Who Can Use the Application?",
                  "content": "To use the Application, you must meet the following conditions:\\n\\n• Since registration is carried out via a mobile number, use only a lawful mobile number belonging to you;\\n• Use the Application solely for personal (non-commercial) purposes;\\n• Be the legal owner of the registered vehicle or have explicit authorization from the owner."
                },
                {
                  "id": 6,
                  "title": "6. Limitation of Liability",
                  "content": "• Quality of Service: CarCat is not responsible for the quality of repairs performed by third-party service centers, damage to the vehicle, or price disputes.\\n• Accuracy: Automated reminders for technical inspections or updates in the Application are informational. Compliance with legal vehicle requirements remains entirely your responsibility.\\n• Financial Limitation: To the maximum extent permitted by law, CarCat's total financial liability for any damage caused to you is limited to 1 AZN."
                },
                {
                  "id": 7,
                  "title": "7. Account Management and Security",
                  "content": "• You are responsible for maintaining the confidentiality of your account information.\\n• If you lose ownership or control of your registered mobile number, you must delete your account immediately.\\n• You must update the data in the application if the vehicle is sold or the license plate number changes.\\n• CarCat reserves the right to suspend accounts without prior notice in case of violation of terms or suspicion of fraud."
                },
                {
                  "id": 8,
                  "title": "8. Data Processing, Integration, and Cross-Border Transfer",
                  "content": "By using or registering on the Application, you consent to the exchange of your data with third-party partners during service execution and system integrations, as well as its secure storage on foreign servers."
                },
                {
                  "id": 9,
                  "title": "9. Complaints and Support",
                  "content": "If you have any questions, complaints, or suggestions regarding the Application, you can contact us via:\\n\\n• In-app: Chat support section\\n• Email: nemat.mirzayev@carcat.app\\n• Phone: +994 70 575 75 70\\n\\nInitial responses to inquiries are provided within 5 business days, and fully resolved within a maximum of 30 days."
                },
                {
                  "id": 10,
                  "title": "10. Governing Law and Jurisdiction",
                  "content": "These Terms are governed by the legislation of the Republic of Azerbaijan. Any disputes that cannot be settled amicably between the parties shall be resolved by the relevant courts of Baku."
                },
                {
                  "id": 11,
                  "title": "11. Unilateral Amendments to Terms",
                  "content": "CarCat reserves the right to unilaterally modify or update these Terms of Use at any time. Updated Terms take effect immediately upon publication in the Application. Continued use of the Application after changes constitutes acceptance of the updated Terms.",
                  "contact": {
                    "companyEn": "CARCAT LLC",
                    "companyAz": "\\"CARCAT\\" MMC",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Baku, Azerbaijan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "By using CarCat, you acknowledge that you have read, understood, and agree to be bound by these Terms of Use."
              }
            }
            """;

    private static final String TERMS_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "5 Сентября 2026",
                "version": "3.3",
                "language": "ru",
                "company": "ООО \\"CARCAT\\"",
                "companyAz": "Общество с ограниченной ответственностью \\"CARCAT\\"",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Условия Использования",
                "subtitle": "Использование Приложения регулируется настоящими Условиями. Только для личного пользования. При услугах третьих лиц CarCat выступает как агент. Приложение предоставляется \\"как есть\\"."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Введение",
                  "content": "Настоящие Условия использования («Условия») регулируют правовые отношения между ООО «CARCAT» («CarCat», «мы») и пользователем («вы»). Используя Приложение, вы принимаете настоящие Условия и нашу Политику конфиденциальности. CarCat оставляет за собой право в одностороннем порядке изменять настоящие Условия в любое время."
                },
                {
                  "id": 2,
                  "title": "2. Кто мы? (Юридические данные и сведения о гос. регистрации)",
                  "content": "Мы — Общество с ограниченной ответственностью «CARCAT», прошедшее государственную регистрацию по принципу «одного окна» в соответствии с законодательством Азербайджанской Республики.\\n\\n• Полное юридическое наименование: Общество с ограниченной ответственностью «CARCAT»\\n• ИНН (VÖEN): 1309963601\\n• Регистрация информационной системы в Государственном реестре: Информационная система приложения CarCat — «Электронная система управления сервисными услугами транспортных средств (CARCAT MIS)» была зарегистрирована Национальным агентством по кибербезопасности при Министерстве цифрового развития и транспорта АР 31 июля 2026 года под № İS647005001 в Государственном реестре информационных систем персональных данных.\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Веб-сайт: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. Как работает приложение?",
                  "content": "CarCat — это независимая цифровая платформа, позволяющая управлять всеми вопросами, связанными с вашим автомобилем, в одном месте.\\n\\nПри получении услуг от третьего лица (Сервис-провайдера) через Приложение вы предоставляете CarCat полномочия (агентство) приобретать эту услугу от вашего имени и передавать или обменивать ваши данные (включая данные об автомобиле и контактные данные) с поставщиком в рамках интеграции. Возникающий договор заключается непосредственно между Вами и Сервис-провайдером. Вопросы, возникающие в процессе оказания услуг, должны решаться напрямую с исполнителем."
                },
                {
                  "id": 4,
                  "title": "4. Наши услуги",
                  "content": "Приложение CarCat предоставляет следующие модули и услуги:\\n\\n• CarCat Maintain (Сервис и ремонт): Бронирование техосмотра, замены масла и ремонтных услуг в партнерских сервисных центрах.\\n• CarCat Passport (Паспорт автомобиля): Отслеживание истории обслуживания, пробега и технических уведомлений автомобиля по VIN-коду."
                },
                {
                  "id": 5,
                  "title": "5. Кто может использовать приложение?",
                  "content": "Для использования Приложения вы должны соответствовать следующим условиям:\\n\\n• Поскольку регистрация осуществляется по номеру телефона, использовать только принадлежащий вам на законных основаниях мобильный номер;\\n• Использовать Приложение исключительно в личных (некоммерческих) целях;\\n• Быть законным владельцем регистрируемого автомобиля или иметь явное разрешение от владельца."
                },
                {
                  "id": 6,
                  "title": "6. Ограничение ответственности",
                  "content": "• Качество услуг: CarCat не несет ответственности за качество ремонта, выполненного сторонними сервисными центрами, повреждения автомобиля или споры по ценам.\\n• Точность: Автоматические напоминания о техосмотре или обновлениях носят справочный характер. Ответственность за соответствие автомобиля законным требованиям лежит исключительно на вас.\\n• Финансовое ограничение: В максимальной степени, разрешенной законом, общая финансовая ответственность CarCat за любой причиненный вам ущерб ограничена суммой 1 AZN."
                },
                {
                  "id": 7,
                  "title": "7. Управление аккаунтом и безопасность",
                  "content": "• Вы несете ответственность за сохранение конфиденциальности данных вашего аккаунта.\\n• В случае утраты права владения или контроля над зарегистрированным номером телефона вы должны немедленно удалить свой аккаунт.\\n• Вы обязаны обновить данные в приложении в случае продажи автомобиля или изменения государственного регистрационного номера.\\n• CarCat оставляет за собой право приостановить действие аккаунта без предварительного уведомления при нарушении условий или подозрении в мошенничестве."
                },
                {
                  "id": 8,
                  "title": "8. Обработка данных, интеграция и трансграничная передача",
                  "content": "Используя Приложение или регистрируясь в нем, вы даете согласие на обмен вашими данными с третьими сторонами-партнерами при исполнении услуг и системных интеграциях, а также на их безопасное хранение на зарубежных серверах."
                },
                {
                  "id": 9,
                  "title": "9. Жалобы и поддержка",
                  "content": "Если у вас есть вопросы, жалобы или предложения по поводу Приложения, вы можете связаться с нами по следующим каналам:\\n\\n• В приложении: Чат поддержки\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Телефон: +994 70 575 75 70\\n\\nПервичный ответ на запросы предоставляется в течение 5 рабочих дней, полное решение — максимум в течение 30 дней."
                },
                {
                  "id": 10,
                  "title": "10. Применимое право и юрисдикция",
                  "content": "Настоящие Условия регулируются законодательством Азербайджанской Республики. Споры, не урегулированные между сторонами путем переговоров, подлежат рассмотрению в соответствующих судах города Баку."
                },
                {
                  "id": 11,
                  "title": "11. Одностороннее изменение условий",
                  "content": "CarCat оставляет за собой право в одностороннем порядке изменять или обновлять настоящие Условия использования в любое время. Обновленные Условия вступают в силу с момента их публикации в Приложении. Продолжение использования Приложения после внесения изменений означает ваше согласие с обновленными Условиями.",
                  "contact": {
                    "companyEn": "CARCAT LLC",
                    "companyAz": "ООО \\"CARCAT\\"",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Баку, Азербайджан"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "Используя CarCat, вы подтверждаете, что прочитали, поняли и соглашаетесь с настоящими Условиями использования."
              }
            }
            """;

    // -------------------------------------------------------------------------
    // Privacy Policy v3.5 — AZ / EN / RU (RU: backend translation of CEO EN)
    // -------------------------------------------------------------------------

    private static final String POLICY_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "5 Sentyabr 2026",
                "version": "3.5",
                "language": "az",
                "company": "CARCAT MMC",
                "companyAz": "\\"CARCAT\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "responseTime": "30 gün",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Məxfilik Siyasəti",
                "subtitle": "Bu Siyasət \\"CARCAT\\" MMC tərəfindən idarə olunan CarCat mobil tətbiqi və rəqəmsal platforması vasitəsilə fərdi məlumatlarınızın toplanması, işlənməsi, saxlanılması, qorunması və üçüncü tərəflərə ötürülməsi qaydalarını müəyyən edir."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Ümumi Müddəalar və Hüquqi Əsaslar",
                  "content": "Bu Məxfilik Siyasəti (\\"Siyasət\\") \\"CARCAT\\" MMC (\\"CarCat\\", \\"biz\\") tərəfindən idarə olunan CarCat mobil tətbiqi və rəqəmsal platforması vasitəsilə istifadəçilərin (\\"siz\\") fərdi məlumatlarının toplanması, işlənməsi, saxlanılması, qorunması və üçüncü tərəflərə ötürülməsi qaydalarını müəyyən edir.\\n\\nFərdi məlumatlarınızın işlənməsi Azərbaycan Respublikasının \\"Fərdi məlumatlar haqqında\\" Qanununa, sahəvi qanunvericiliyə və beynəlxalq məxfilik standartlarına tam uyğun olaraq həyata keçirilir. Tətbiqdən qeydiyyatdan keçməklə və ya xidmətlərdən istifadə etməklə siz bu Məxfilik Siyasətində göstərilən şərtlərlə fərdi məlumatlarınızın işlənməsinə açıq razılıq vermiş olursunuz.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Məlumat Nəzarətçisi və Dövlət Reyestr Qeydiyyatı",
                  "content": "Fərdi məlumatlarınızın işlənməsinə cavabdeh olan hüquqi şəxs:\\n\\n• Hüquqi Şəxsin Tam Adı: \\"CARCAT\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• Dövlət Reyestr Qeydiyyatı: CarCat tətbiqinin İnformasiya sistemi — \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CARCAT MİS)\\" AR Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• Hüquqi / Əlaqə Ünvanı: Bakı şəhəri, Azərbaycan\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Əlaqə Nömrəsi: +994 70 575 75 70\\n• Veb-sayt: https://carcat.app/",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Toplanılan Məlumatlar",
                  "content": "CarCat tətbiqi xidmətlərin keyfiyyətli və fasiləsiz göstərilməsi üçün aşağıdakı fərdi və texniki məlumatları toplayır:\\n\\n• Şəxsi İdentifikasiya Məlumatları: Ad, soyad, mobil telefon nömrəsi, e-poçt ünvanı;\\n• Avtomobil Məlumatları: Avtomobilin VIN kodu, dövlət qeydiyyat nişanı, markası, modeli, buraxılış ili, cari yürüşü (odometr göstəricisi) və xidmət/təmir tarixçəsi məlumatları;\\n• Geolokasiya Məlumatları: Ən yaxın servis mərkəzlərinin tapılması və xəritə inteqrasiyaları üçün cihazınızın naviqasiya/GPS məlumatları (icazəniz daxilində);\\n• Ödəniş və Maliyyə Məlumatları: Tətbiq daxilində həyata keçirilən ödənişlər zamanı bank kartı məlumatları birbaşa lisenziyalı ödəniş təşkilatları/banklar tərəfindən emal olunur; CarCat kartın tam rekvizitlərini öz serverlərində saxlamır;\\n• Texniki və Cihaz Məlumatları: IP ünvanı, cihaz modeli, əməliyyat sistemi versiyası, unikal cihaz identifikatorları (UUID), tətbiqdən istifadə loqları və xəta hesabatları.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Məlumatların Toplanması və İşlənməsi Məqsədləri",
                  "content": "Toplanılan məlumatlar aşağıdakı məqsədlərlə işlənilir:\\n\\n• CarCat Maintain və CarCat Passport modulları üzrə xidmətlərin təşkili və avtomobilin rəqəmsal servis pasportunun formalaşdırılması;\\n• Tərəfdaş servis mərkəzlərində texniki baxış, təmir və yağdəyişmə vaxtlarının bron edilməsi və xidmət sorğularının icrası;\\n• Avtomobilin texniki baxış, sığorta, yürüş və xidmət xəbərdarlıqlarının avtomatik bildiriş (push notification/SMS) vasitəsilə istifadəçiyə çatdırılması;\\n• Tətbiqin təhlükəsizliyinin təmin edilməsi, dələduzluq hallarının qarşısının alınması və sistem tənzimləmələrinin həyata keçirilməsi;\\n• Müştəri dəstəyi xidmətinin göstərilməsi, şikayət və sorğuların cavablandırılması;\\n• Qanunvericilikdən doğan hüquqi öhdəliklərin yerinə yetirilməsi.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Məlumatların Üçüncü Tərəflərə Ötürülməsi və İnteqrasiyalar",
                  "content": "CarCat fərdi məlumatlarınızı üçüncü tərəflərə satmır və kommersiya məqsədilə icarəyə vermir. Məlumatlar yalnız aşağıdakı hallarda ötürülə bilər:\\n\\n• Tərəfdaş Servis Mərkəzlərinə: Seçdiyiniz xidmətin göstərilməsi üçün zəruri olan həcmdə (ad, telefon, avtomobilin markası/modeli və VIN kodu);\\n• Ekosistem və İnteqrasiya Tərəfdaşlarına: Xidmət zəncirinin təmin edilməsi, identifikasiya və məlumat dəqiqləşdirilməsi məqsədilə inteqrasiya olunmuş tərəfdaşlar;\\n• Hüquq-Mühafizə və Dövlət Qurumlarına: Qanunla nəzərdə tutulmuş hallarda, səlahiyyətli dövlət organlarının rəsmi və əsaslandırılmış tələbi əsasında;\\n• Xidmət Təminatçılarına: Server, bulud saxlanma, analitika və bildiriş göndərişi xidmətlərini təmin edən IT subpodratçılara (məxfiliyin qorunması öhdəlikləri çərçivəsində).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Transsərhəd Məlumat Ötürülməsi",
                  "content": "Tətbiqin fasiləsiz və təhlükəsiz fəaliyyətini təmin etmək üçün fərdi məlumatlarınız beynəlxalq təhlükəsizlik standartlarına cavab verən xarici bulud serverlərində (Cloud Infrastructure) saxlanıla və işlənilə bilər. Tətbiqdən istifadə etməklə məlumatlarınızın təhlükəsiz transsərhəd ötürülməsinə razılıq vermiş olursunuz.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Məlumatların Təhlükəsizliyi, Məsuliyyət Məhdudiyyəti və Saxlanılma Müddəti",
                  "content": "CarCat fərdi məlumatların qanunsuz girişdən, dəyişdirilmədən, açıqlanmadan və ya məhv edilmədən qorunması üçün müasir şifrələmə protokollarından (SSL/TLS), təhlükəsizlik divarlarından (Firewall) və giriş məhdudiyyəti mexanizmlərindən istifadə edir.\\n\\n• Məsuliyyətin Məhdudlaşdırılması: CarCat platformasının təhlükəsizliyini təmin etmək üçün bütün zəruri texniki və təşkilati tədbirlər görülsə də, gözlənilməz kiberhücumlar, zərərli proqram təminatı hücumları (malware, ransomware), genişmiqyaslı kiber-qəsd, qarşısıalınmaz fors-major halları və ya qlobal infrastruktur kəsintiləri (DDoS hücumları və s.) nəticəsində yaranmış məlumat sızması, itkisi və ya xidmət kəsintilərinə görə \\"CARCAT\\" MMC qanunvericiliyin icazə verdiyi maksimum həddə hüquqi və maliyyə məsuliyyəti daşımır.\\n\\nMəlumatlarınız hesabınız aktiv olduğu müddətdə saxlanılır. Hesab silindikdə və ya qanunvericiliklə müəyyən edilmiş saxlanılma müddəti bitdikdə fərdi məlumatlar təhlükəsiz şəkildə silinir və ya anonimləşdirilir.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. İstifadəçinin Hüquqları",
                  "content": "Fərdi məlumatlarınızla bağlı aşağıdakı hüquqlara maliksiniz:",
                  "icon": "verified_user_outlined",
                  "highlighted": false,
                  "dataRights": [
                    {
                      "icon": "visibility_outlined",
                      "title": "Məlumat",
                      "description": "CarCat tərəfindən hansı fərdi məlumatlarınızın işlənildiyi haqqında məlumat tələb etmək."
                    },
                    {
                      "icon": "edit_outlined",
                      "title": "Düzəliş",
                      "description": "Səhv və ya natamam məlumatların düzəldilməsini tələb etmək."
                    },
                    {
                      "icon": "delete_outline",
                      "title": "Silmə",
                      "description": "Qanunla nəzərdə tutulmuş hallar müstəsna olmaqla, fərdi məlumatlarınızın silinməsini və ya işlənməsinin dayandırılmasını tələb etmək."
                    },
                    {
                      "icon": "cancel_outlined",
                      "title": "Razılığın geri götürülməsi",
                      "description": "Məlumatların işlənməsinə verdiyiniz razılığı istənilən vaxt geri götürmək (bu halda tətbiqin bəzi funksiyaları əlçatmaz ola bilər)."
                    }
                  ]
                },
                {
                  "id": 9,
                  "title": "9. Hesabın və Məlumatların Silinməsi",
                  "content": "İstifadəçi istənilən vaxt nemat.mirzayev@carcat.app e-poçt ünvanına rəsmi sorğu göndərməklə hesabının və ona bağlı fərdi məlumatların silinməsini tələb edə bilər. Sorğunuz maksimum 30 gün ərzində emal olunur.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Məxfilik Siyasətində Dəyişikliklər",
                  "content": "CarCat bu Məxfilik Siyasətini istənilən vaxt yeniləmək hüququnu saxlayır. Siyasətdə mühüm dəyişikliklər edildikdə tətbiq daxili bildiriş və ya e-poçt vasitəsilə sizə məlumat veriləcəkdir. Yenilənmiş Siyasət tətbiqdə dərc edildiyi andan qüvvəyə minir.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Məlumat satılmır",
                "content": "CarCat fərdi məlumatlarınızı üçüncü tərəflərə satmır və kommersiya məqsədilə icarəyə vermir.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 11,
                "title": "11. Əlaqə və Dəstək",
                "subtitle": "Bu Məxfilik Siyasəti və ya fərdi məlumatlarınızın işlənməsi ilə bağlı sual, sorğu və ya şikayətiniz olduqda:",
                "company": "CARCAT MMC",
                "companyAz": "\\"CARCAT\\" MMC",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Bakı, Azərbaycan",
                "phone": "+994 70 575 75 70",
                "responseNote": "Məsul Şəxs / CEO: Nemət Mirzəyev. Sorğular maksimum 30 gün ərzində emal olunur."
              },
              "footer": {
                "consentText": "Tətbiqdən qeydiyyatdan keçməklə və ya xidmətlərdən istifadə etməklə bu Məxfilik Siyasətində göstərilən şərtlərlə fərdi məlumatlarınızın işlənməsinə açıq razılıq vermiş olursunuz."
              }
            }
            """;

    private static final String POLICY_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "September 5, 2026",
                "version": "3.5",
                "language": "en",
                "company": "CARCAT LLC",
                "companyAz": "\\"CARCAT\\" Limited Liability Company",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "responseTime": "30 days",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Privacy Policy",
                "subtitle": "This Policy sets forth the rules governing the collection, processing, storage, protection, and transfer of personal data of users through the CarCat mobile application and digital platform managed by \\"CARCAT\\" LLC."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. General Provisions and Legal Basis",
                  "content": "This Privacy Policy (\\"Policy\\") sets forth the rules governing the collection, processing, storage, protection, and transfer of personal data of users (\\"you\\") through the CarCat mobile application and digital platform managed by \\"CARCAT\\" LLC (\\"CarCat\\", \\"we\\", \\"us\\").\\n\\nThe processing of your personal data is carried out in full compliance with the Law of the Republic of Azerbaijan \\"On Personal Data\\", relevant sector legislation, and international privacy standards. By registering on the application or using the services, you explicitly consent to the processing of your personal data under the terms specified in this Privacy Policy.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Data Controller and State Register Registration",
                  "content": "The legal entity responsible for the processing of your personal data:\\n\\n• Full Legal Name: \\"CARCAT\\" Limited Liability Company\\n• TIN (VÖEN): 1309963601\\n• State Register Registration: Information system of the CarCat application — \\"Electronic Management System for Vehicle Service Operations (CARCAT MIS)\\" was registered in the State Register of Information Systems of Personal Data by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan on July 31, 2026, under registration number İS647005001.\\n• Legal / Contact Address: Baku, Azerbaijan\\n• Email: nemat.mirzayev@carcat.app\\n• Phone: +994 70 575 75 70\\n• Website: https://carcat.app/",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Collected Data",
                  "content": "CarCat collects the following personal and technical data to ensure high-quality and uninterrupted service delivery:\\n\\n• Personal Identification Data: Name, surname, mobile phone number, email address;\\n• Vehicle Data: Vehicle VIN code, license plate number, make, model, year of manufacture, current mileage (odometer reading), and service/repair history records;\\n• Geolocation Data: Device navigation/GPS data (within your permission) to locate the nearest service centers and integrate maps;\\n• Payment and Financial Data: Bank card details during in-app payments are processed directly by licensed payment institutions/banks; CarCat does not store full card credentials on its servers;\\n• Technical and Device Data: IP address, device model, operating system version, unique device identifiers (UUID), application usage logs, and crash reports.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Purposes of Data Collection and Processing",
                  "content": "Collected data is processed for the following purposes:\\n\\n• Organization of services under CarCat Maintain and CarCat Passport modules and formation of the digital vehicle service passport;\\n• Booking inspection, repair, and oil change appointments at partner service centers and executing service requests;\\n• Delivering automated alerts (push notifications/SMS) regarding vehicle technical inspection, insurance, mileage, and maintenance updates;\\n• Ensuring platform security, preventing fraudulent activities, and performing system maintenance;\\n• Providing customer support, responding to inquiries, and handling complaints;\\n• Fulfilling legal obligations under applicable law.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Data Transfer to Third Parties and Integrations",
                  "content": "CarCat does not sell or rent your personal data to third parties for commercial purposes. Data may only be shared under the following circumstances:\\n\\n• To Partner Service Centers: To the extent necessary to deliver your requested service (name, phone number, vehicle make/model, and VIN code);\\n• To Ecosystem and Integration Partners: Integrated partners for service continuity, identity verification, and data validation;\\n• To Law Enforcement and Public Authorities: In cases provided by law, based on official and justified requests from authorized state bodies;\\n• To Service Providers: IT subcontractors providing server hosting, cloud storage, analytics, and notification delivery services (subject to strict confidentiality obligations).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Cross-Border Data Transfer",
                  "content": "To ensure uninterrupted and secure operation of the application, your personal data may be stored and processed on external cloud servers (Cloud Infrastructure) that comply with international security standards. By using the application, you consent to the secure cross-border transfer of your data.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Data Security, Limitation of Liability, and Retention Period",
                  "content": "CarCat employs modern encryption protocols (SSL/TLS), firewalls, and access control mechanisms to protect personal data against unauthorized access, alteration, disclosure, or destruction.\\n\\n• Limitation of Liability: While all necessary technical and organizational measures are taken to secure the CarCat platform, \\"CARCAT\\" LLC assumes no legal or financial liability, to the maximum extent permitted by law, for data breaches, data loss, or service interruptions resulting from unforeseen cyberattacks, malicious software attacks (malware, ransomware), large-scale cyber intrusions, force majeure events, or global infrastructure outages (DDoS attacks, etc.).\\n\\nYour data is stored for as long as your account remains active. Upon account deletion or expiration of the legally mandated retention period, personal data will be securely deleted or anonymized.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. User Rights",
                  "content": "You have the following rights regarding your personal data:",
                  "icon": "verified_user_outlined",
                  "highlighted": false,
                  "dataRights": [
                    {
                      "icon": "visibility_outlined",
                      "title": "Access",
                      "description": "Request information on what personal data is processed by CarCat."
                    },
                    {
                      "icon": "edit_outlined",
                      "title": "Correction",
                      "description": "Request correction of incorrect or incomplete data."
                    },
                    {
                      "icon": "delete_outline",
                      "title": "Deletion",
                      "description": "Request deletion or cessation of processing of your personal data, except where required by law."
                    },
                    {
                      "icon": "cancel_outlined",
                      "title": "Withdraw consent",
                      "description": "Withdraw your consent to data processing at any time (note that certain app features may become unavailable)."
                    }
                  ]
                },
                {
                  "id": 9,
                  "title": "9. Account and Data Deletion",
                  "content": "Users can request the deletion of their account and associated personal data at any time by sending an official request to nemat.mirzayev@carcat.app. Requests are processed within a maximum of 30 days.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Changes to the Privacy Policy",
                  "content": "CarCat reserves the right to update this Privacy Policy at any time. In the event of material changes, you will be notified via in-app notifications or email. The updated Policy takes effect upon publication in the application.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "We Do Not Sell Your Data",
                "content": "CarCat does not sell or rent your personal data to third parties for commercial purposes.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 11,
                "title": "11. Contact and Support",
                "subtitle": "If you have any questions, inquiries, or complaints regarding this Privacy Policy or the processing of your personal data:",
                "company": "CARCAT LLC",
                "companyAz": "\\"CARCAT\\" MMC",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Baku, Azerbaijan",
                "phone": "+994 70 575 75 70",
                "responseNote": "Responsible Person / CEO: Nemat Mirzayev. Requests are processed within a maximum of 30 days."
              },
              "footer": {
                "consentText": "By registering on the application or using the services, you explicitly consent to the processing of your personal data under the terms specified in this Privacy Policy."
              }
            }
            """;

    private static final String POLICY_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "5 Сентября 2026",
                "version": "3.5",
                "language": "ru",
                "company": "ООО \\"CARCAT\\"",
                "companyAz": "Общество с ограниченной ответственностью \\"CARCAT\\"",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "responseTime": "30 дней",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Политика конфиденциальности",
                "subtitle": "Настоящая Политика определяет правила сбора, обработки, хранения, защиты и передачи персональных данных пользователей через мобильное приложение и цифровую платформу CarCat, управляемую ООО «CARCAT»."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Общие положения и правовая основа",
                  "content": "Настоящая Политика конфиденциальности («Политика») определяет правила сбора, обработки, хранения, защиты и передачи персональных данных пользователей («вы») через мобильное приложение и цифровую платформу CarCat, управляемую ООО «CARCAT» («CarCat», «мы»).\\n\\nОбработка ваших персональных данных осуществляется в полном соответствии с Законом Азербайджанской Республики «О персональных данных», отраслевым законодательством и международными стандартами конфиденциальности. Регистрируясь в приложении или пользуясь услугами, вы даёте явное согласие на обработку ваших персональных данных на условиях, указанных в настоящей Политике.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Контролёр данных и регистрация в Государственном реестре",
                  "content": "Юридическое лицо, ответственное за обработку ваших персональных данных:\\n\\n• Полное юридическое наименование: Общество с ограниченной ответственностью «CARCAT»\\n• ИНН (VÖEN): 1309963601\\n• Регистрация в Государственном реестре: Информационная система приложения CarCat — «Электронная система управления сервисными услугами транспортных средств (CARCAT MIS)» зарегистрирована Национальным агентством по кибербезопасности при Министерстве цифрового развития и транспорта АР 31 июля 2026 года под № İS647005001 в Государственном реестре информационных систем персональных данных.\\n• Юридический / контактный адрес: город Баку, Азербайджан\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Телефон: +994 70 575 75 70\\n• Веб-сайт: https://carcat.app/",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Собираемые данные",
                  "content": "Приложение CarCat собирает следующие персональные и технические данные для качественного и бесперебойного оказания услуг:\\n\\n• Данные личной идентификации: имя, фамилия, номер мобильного телефона, адрес электронной почты;\\n• Данные об автомобиле: VIN-код, государственный регистрационный номер, марка, модель, год выпуска, текущий пробег (показания одометра) и история обслуживания/ремонта;\\n• Данные геолокации: навигационные/GPS-данные устройства (с вашего разрешения) для поиска ближайших сервисных центров и интеграции карт;\\n• Платёжные и финансовые данные: данные банковских карт при оплатах в приложении обрабатываются напрямую лицензированными платёжными организациями/банками; CarCat не хранит полные реквизиты карты на своих серверах;\\n• Технические данные и данные устройства: IP-адрес, модель устройства, версия ОС, уникальные идентификаторы устройства (UUID), журналы использования приложения и отчёты об ошибках.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Цели сбора и обработки данных",
                  "content": "Собранные данные обрабатываются для следующих целей:\\n\\n• Организация услуг по модулям CarCat Maintain и CarCat Passport и формирование цифрового сервисного паспорта автомобиля;\\n• Бронирование техосмотра, ремонта и замены масла в партнёрских сервисных центрах и исполнение сервисных запросов;\\n• Доставка автоматических уведомлений (push/SMS) о техосмотре, страховке, пробеге и обслуживании;\\n• Обеспечение безопасности платформы, предотвращение мошенничества и техническое обслуживание системы;\\n• Клиентская поддержка, ответы на обращения и жалобы;\\n• Исполнение правовых обязательств, предусмотренных законодательством.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Передача данных третьим лицам и интеграции",
                  "content": "CarCat не продаёт и не сдаёт в аренду ваши персональные данные третьим лицам в коммерческих целях. Данные могут передаваться только в следующих случаях:\\n\\n• Партнёрским сервисным центрам: в объёме, необходимом для оказания выбранной услуги (имя, телефон, марка/модель автомобиля и VIN-код);\\n• Партнёрам экосистемы и интеграций: для непрерывности услуг, идентификации и уточнения данных;\\n• Правоохранительным и государственным органам: в случаях, предусмотренных законом, на основании официальных и обоснованных запросов уполномоченных органов;\\n• Поставщикам услуг: IT-субподрядчикам, обеспечивающим хостинг, облачное хранение, аналитику и доставку уведомлений (в рамках обязательств по конфиденциальности).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Трансграничная передача данных",
                  "content": "Для бесперебойной и безопасной работы приложения ваши персональные данные могут храниться и обрабатываться на внешних облачных серверах (Cloud Infrastructure), соответствующих международным стандартам безопасности. Используя приложение, вы соглашаетесь на безопасную трансграничную передачу ваших данных.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Безопасность данных, ограничение ответственности и срок хранения",
                  "content": "CarCat применяет современные протоколы шифрования (SSL/TLS), межсетевые экраны (Firewall) и механизмы ограничения доступа для защиты персональных данных от несанкционированного доступа, изменения, раскрытия или уничтожения.\\n\\n• Ограничение ответственности: несмотря на все необходимые технические и организационные меры по обеспечению безопасности платформы CarCat, ООО «CARCAT» в максимальной степени, разрешённой законом, не несёт правовой и финансовой ответственности за утечку, утрату данных или перебои в обслуживании вследствие непредвиденных кибератак, вредоносного ПО (malware, ransomware), масштабных кибервторжений, форс-мажора или глобальных сбоев инфраструктуры (DDoS и т. п.).\\n\\nВаши данные хранятся, пока аккаунт активен. После удаления аккаунта или истечения установленного законом срока хранения персональные данные безопасно удаляются или обезличиваются.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Права пользователя",
                  "content": "В отношении ваших персональных данных вы имеете следующие права:",
                  "icon": "verified_user_outlined",
                  "highlighted": false,
                  "dataRights": [
                    {
                      "icon": "visibility_outlined",
                      "title": "Информация",
                      "description": "Запросить сведения о том, какие ваши персональные данные обрабатывает CarCat."
                    },
                    {
                      "icon": "edit_outlined",
                      "title": "Исправление",
                      "description": "Потребовать исправления неверных или неполных данных."
                    },
                    {
                      "icon": "delete_outline",
                      "title": "Удаление",
                      "description": "Потребовать удаления или прекращения обработки персональных данных, за исключением случаев, предусмотренных законом."
                    },
                    {
                      "icon": "cancel_outlined",
                      "title": "Отзыв согласия",
                      "description": "В любой момент отозвать согласие на обработку данных (при этом некоторые функции приложения могут стать недоступны)."
                    }
                  ]
                },
                {
                  "id": 9,
                  "title": "9. Удаление аккаунта и данных",
                  "content": "Пользователь в любое время может потребовать удаления аккаунта и связанных персональных данных, направив официальный запрос на nemat.mirzayev@carcat.app. Запрос обрабатывается в срок не более 30 дней.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "10. Изменения Политики конфиденциальности",
                  "content": "CarCat оставляет за собой право обновлять настоящую Политику в любое время. При существенных изменениях вы будете уведомлены через уведомление в приложении или по электронной почте. Обновлённая Политика вступает в силу с момента публикации в приложении.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Данные не продаются",
                "content": "CarCat не продаёт и не сдаёт в аренду ваши персональные данные третьим лицам в коммерческих целях.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 11,
                "title": "11. Контакты и поддержка",
                "subtitle": "По вопросам настоящей Политики или обработки персональных данных:",
                "company": "ООО \\"CARCAT\\"",
                "companyAz": "ООО \\"CARCAT\\"",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Баку, Азербайджан",
                "phone": "+994 70 575 75 70",
                "responseNote": "Ответственное лицо / CEO: Немат Мирзаев. Запросы обрабатываются в срок не более 30 дней."
              },
              "footer": {
                "consentText": "Регистрируясь в приложении или пользуясь услугами, вы даёте явное согласие на обработку персональных данных на условиях настоящей Политики конфиденциальности."
              }
            }
            """;
}
