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
 *     Metin kaynağı: Terms v3.5, Privacy v3.5 — "CarCat" MMC (az/en/ru).
 * en: Legal content REST controller; serves terms and privacy policy in az/en/ru.
 *     Source: Terms v3.5, Privacy v3.5 — "CarCat" LLC (az/en/ru).
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
    // Terms of Use v3.5 — AZ / EN / RU
    // -------------------------------------------------------------------------

    private static final String TERMS_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "5 sentyabr 2026",
                "version": "3.5",
                "language": "az",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — İstifadə şərtləri",
                "subtitle": "Tətbiq yalnız şəxsi istifadə üçündür. Üçüncü tərəf ödənişlərində CarCat nümayəndə (agent) kimi çıxış edir. Tətbiq \\"olduğu kimi\\" təqdim olunur."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Giriş",
                  "content": "Bu İstifadə şərtləri (\\"Şərtlər\\") \\"CarCat\\" MMC (\\"CarCat\\", \\"biz\\") ilə istifadəçi (\\"siz\\") arasındakı hüquqi münasibətləri tənzimləyir. Tətbiqdən istifadə etməklə bu \\"Şərtləri və Məxfilik siyasətimizi\\" qəbul etmiş olursunuz. CarCat bu Şərtləri istənilən vaxt birtərəfli qaydada dəyişdirmək hüququnu saxlayır. Həmçinin:\\n\\n• Şəxsi istifadə: Tətbiq yalnız şəxsi istifadə üçün nəzərdə tutulub. Kommersiya məqsədilə istifadə edilə, satıla və ya yenidən paylanıla bilməz.\\n• Vasitəçilik və ödənişlər: Tətbiq vasitəsilə üçüncü tərəf Xidmət göstəricilərinə (servislər və s.) ödəniş etdikdə, CarCat sizin adınızdan nümayəndə (agent) kimi çıxış edir. Xidmət göstəricilərinin xətalarına və ya xidmət keyfiyyətinə görə CarCat məsuliyyət daşımır.\\n• \\"Olduğu kimi\\" prinsipi: Tətbiq \\"olduğu kimi\\" təqdim olunur. Məlumatların hər an 100% dəqiq və ya yenilənmiş olmasına zəmanət verilmir."
                },
                {
                  "id": 2,
                  "title": "2. Biz kimik?",
                  "content": "Biz Azərbaycan Respublikasının qanunvericiliyinə uyğun olaraq \\"bir pəncərə\\" prinsipi ilə dövlət qeydiyyatına alınmış \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyətiyik.\\n\\n• Hüquqi şəxsin tam adı: \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• İnformasiya sisteminin dövlət reyestr qeydiyyatı: CarCat tətbiqinin informasiya sistemi — \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CarCat MİS)\\" AR Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Veb-sayt: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. Tətbiq necə işləyir?",
                  "content": "CarCat avtomobilinizlə bağlı bütün işləri tək məkandan idarə etməyə imkan verən müstəqil rəqəmsal platformadır. Tətbiq vasitəsilə üçüncü tərəf Xidmət göstəricisindən xidmət aldıqda, CarCat-a həmin xidməti sizin adınızdan almaq və məlumatlarınızı (o cümlədən avtomobil və əlaqə məlumatlarını) təchizatçıya ötürmək və ya inteqrasiya çərçivəsində mübadilə etmək səlahiyyəti (agentlik) verirsiniz. Yaranan müqavilə birbaşa siz və Xidmət göstəricisi arasında bağlanır. Xidmətin icrası zamanı ortaya çıxan problemlərə görə birbaşa xidmət göstərən tərəflə əlaqə saxlanılmalıdır."
                },
                {
                  "id": 4,
                  "title": "4. Xidmətlərimiz",
                  "content": "CarCat tətbiqi daxilində aşağıdakı modulları və xidmətləri təqdim edir:\\n\\n• CarCat texniki qulluq və əlavə dəyər verən xidmətlər (Servis və təmir): Tərəfdaş servis mərkəzlərində texniki baxış, yağdəyişmə, təmir xidmətlərini bron etmək və digər məlumatlar\\n• CarCat Passport (Avtomobil pasportu): VIN kod vasitəsilə avtomobilin xidmət tarixçəsini, yürüşünü və texniki xəbərdarlıqlarını izləmək"
                },
                {
                  "id": 5,
                  "title": "5. Kimlər istifadə edə bilər?",
                  "content": "Tətbiqdən istifadə etmək üçün aşağıdakı şərtlərə cavab verməlisiniz:\\n\\n• Tətbiqdə qeydiyyat mobil nömrə vasitəsilə aparıldığı üçün yalnız özünüzə məxsus qanuni mobil nömrədən istifadə etmək;\\n• Tətbiqdən yalnız şəxsi (qeyri-kommersiya) məqsədlər üçün istifadə etmək;\\n• Qeydiyyata aldığınız avtomobilin qanuni mülkiyyətçisi olmaq və ya idarəetmə hüququna malik olmaq."
                },
                {
                  "id": 6,
                  "title": "6. Məsuliyyətin məhdudlaşdırılması (Liability Disclaimer)",
                  "content": "• Xidmət keyfiyyəti: Üçüncü tərəf servis mərkəzlərinin göstərdiyi təmirlərin keyfiyyətinə, avtomobilə dəyən zərərə və ya qiymət mübahisələrinə görə CarCat məsuliyyət daşımır.\\n• Dəqiqlik: Tətbiqdə avtomatik xatırladılan texniki baxış və ya yeniləmə tarixləri məlumat xarakterlidir. Avtomobilinizin qanuni tələblərə uyğunluğuna görə cavabdehlik tam olaraq sizin üzərinizdədir.\\n• Maliyyə məhdudiyyəti: Qanunla icazə verilən maksimum həddə, CarCat-ın sizə vurduğu zərərə görə ümumi maliyyə məsuliyyəti 1 AZN məbləği ilə məhdudlaşır."
                },
                {
                  "id": 7,
                  "title": "7. Hesabın idarə edilməsi və təhlükəsizlik",
                  "content": "• Siz hesab məlumatlarınızın məxfiliyini qorumağa cavabdehsiniz.\\n• Qeydiyyatda olan mobil nömrənizə sahibliyi və ya nəzarəti itirdiyiniz halda, hesabınızı dərhal silməlisiniz.\\n• Avtomobil satıldıqda və ya dövlət nömrə nişanı dəyişdikdə tətbiqdəki məlumatları güncəlləməlisiniz.\\n• CarCat şərtlər pozulduqda və ya saxtakarlıq şübhəsi olduqda hesabı xəbərdarlıq etmədən dayandırmaq hüququnu saxlayır."
                },
                {
                  "id": 8,
                  "title": "8. Məlumatların işlənməsi, inteqrasiya və transsərhəd ötürülməsi",
                  "content": "Tətbiqdən istifadə etməklə və ya qeydiyyatdan keçməklə xidmətlərin icrası və sistem inteqrasiyaları zamanı məlumatlarınızın üçüncü tərəf tərəfdaşlarla mübadilə edilməsinə, habelə xarici serverlərdə təhlükəsiz saxlanılmasına razılıq verirsiniz."
                },
                {
                  "id": 9,
                  "title": "9. Şikayətlər və dəstək",
                  "content": "Tətbiqlə bağlı hər hansı sual, şikayət və ya təklifiniz olduqda aşağıdakı kanallarla bizimlə əlaqə saxlaya bilərsiniz:\\n\\n• Tətbiq daxilində: Çat dəstək bölməsi\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Əlaqə nömrəsi: +994 70 575 75 70\\n\\nSorğularınıza 5 iş günü ərzində ilkin cavab verilir və maksimum 30 gün müddətində tam həll edilir."
                },
                {
                  "id": 10,
                  "title": "10. Qanunvericilik və məhkəmə aidiyyəti",
                  "content": "Bu Şərtlər Azərbaycan Respublikasının qanunvericiliyi ilə tənzimlənir. Yaranan mübahisələr tərəflər arasında həll edilmədikdə Bakı şəhərinin müvafiq məhkəmələri tərəfindən baxılır."
                },
                {
                  "id": 11,
                  "title": "11. Şərtlərin birtərəfli dəyişdirilməsi",
                  "content": "CarCat bu İstifadə şərtlərini istənilən vaxt birtərəfli qaydada dəyişdirmək və ya yeniləmək hüququnu saxlayır. Yenilənmiş Şərtlər Tətbiqdə dərc edildiyi andan qüvvəyə minir. Dəyişikliklərdən sonra Tətbiqdən istifadəyə davam etməyiniz yenilənmiş Şərtləri qəbul etdiyiniz anlamına gəlir.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "\\"CarCat\\" MMC",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Bakı, Azərbaycan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "Tətbiqdən istifadə etməklə bu İstifadə şərtlərini və Məxfilik siyasətimizi qəbul etmiş olursunuz."
              }
            }
            """;

    private static final String TERMS_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "5 September 2026",
                "version": "3.5",
                "language": "en",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" Limited Liability Company",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Terms of Use",
                "subtitle": "The Application is for personal use only. For payments to third-party Service Providers, CarCat acts as an agent. The Application is provided \\"as is\\"."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Introduction",
                  "content": "These Terms of Use (\\"Terms\\") govern the legal relationship between \\"CarCat\\" LLC (\\"CarCat\\", \\"we\\") and the user (\\"you\\"). By using the Application, you accept these \\"Terms and our Privacy Policy\\". CarCat reserves the right to amend these Terms unilaterally at any time. In addition:\\n\\n• Personal use: The Application is intended for personal use only. It may not be used, sold or redistributed for commercial purposes.\\n• Intermediation and payments: When you make payments to third-party Service Providers (service centres, etc.) through the Application, CarCat acts as a representative (agent) on your behalf. CarCat is not liable for errors of Service Providers or for the quality of their services.\\n• \\"As is\\" principle: The Application is provided \\"as is\\". We do not guarantee that the information will be 100% accurate or up to date at all times."
                },
                {
                  "id": 2,
                  "title": "2. Who are we?",
                  "content": "We are \\"CarCat\\" Limited Liability Company, state-registered under the \\"one-stop shop\\" principle in accordance with the legislation of the Republic of Azerbaijan.\\n\\n• Full legal name: \\"CarCat\\" Limited Liability Company\\n• TIN (VÖEN): 1309963601\\n• Registration of the information system in the state register: The information system of the CarCat application — \\"Electronic management system for vehicle maintenance services (CarCat MİS)\\" — was registered in the State Register of Personal Data Information Systems under number İS647005001 on 31 July 2026 by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan.\\n• Email: nemat.mirzayev@carcat.app\\n• Website: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. How does the Application work?",
                  "content": "CarCat is an independent digital platform that allows you to manage all matters related to your car from a single place. When you obtain a service from a third-party Service Provider through the Application, you authorise CarCat (as your agent) to purchase that service on your behalf and to transfer your data (including vehicle and contact details) to the provider or exchange it within the framework of integration. The resulting contract is concluded directly between you and the Service Provider. For any issues arising during the performance of the service, you should contact the party providing the service directly."
                },
                {
                  "id": 4,
                  "title": "4. Our services",
                  "content": "CarCat offers the following modules and services within the Application:\\n\\n• CarCat maintenance and value-added services (Service and repair): Booking technical inspection, oil change and repair services at partner service centres, and other information\\n• CarCat Passport (Vehicle passport): Tracking the vehicle's service history, mileage and technical alerts via the VIN code"
                },
                {
                  "id": 5,
                  "title": "5. Who can use the Application?",
                  "content": "To use the Application, you must meet the following conditions:\\n\\n• Since registration in the Application is carried out via a mobile number, use only a lawful mobile number that belongs to you;\\n• Use the Application only for personal (non-commercial) purposes;\\n• Be the legal owner of the vehicle you register or have the right to operate it."
                },
                {
                  "id": 6,
                  "title": "6. Limitation of liability",
                  "content": "• Service quality: CarCat is not liable for the quality of repairs performed by third-party service centres, damage caused to the vehicle, or pricing disputes.\\n• Accuracy: Technical inspection or renewal dates automatically reminded in the Application are for information purposes only. You bear full responsibility for your vehicle's compliance with legal requirements.\\n• Financial limitation: To the maximum extent permitted by law, CarCat's total financial liability for any damage caused to you is limited to 1 AZN."
                },
                {
                  "id": 7,
                  "title": "7. Account management and security",
                  "content": "• You are responsible for maintaining the confidentiality of your account information.\\n• If you lose ownership of or control over your registered mobile number, you must delete your account immediately.\\n• If the vehicle is sold or its state registration plate is changed, you must update the information in the Application.\\n• CarCat reserves the right to suspend an account without notice in the event of a breach of the Terms or suspected fraud."
                },
                {
                  "id": 8,
                  "title": "8. Data processing, integration and cross-border transfer",
                  "content": "By using the Application or registering, you consent to your data being exchanged with third-party partners during the performance of services and system integrations, as well as being securely stored on foreign servers."
                },
                {
                  "id": 9,
                  "title": "9. Complaints and support",
                  "content": "If you have any questions, complaints or suggestions regarding the Application, you can contact us through the following channels:\\n\\n• In the Application: Chat support section\\n• Email: nemat.mirzayev@carcat.app\\n• Contact number: +994 70 575 75 70\\n\\nAn initial response to your requests is provided within 5 business days, and they are fully resolved within a maximum of 30 days."
                },
                {
                  "id": 10,
                  "title": "10. Governing law and jurisdiction",
                  "content": "These Terms are governed by the legislation of the Republic of Azerbaijan. Disputes not resolved between the parties shall be considered by the competent courts of the city of Baku."
                },
                {
                  "id": 11,
                  "title": "11. Unilateral amendment of the Terms",
                  "content": "CarCat reserves the right to amend or update these Terms of Use unilaterally at any time. The updated Terms take effect from the moment they are published in the Application. Your continued use of the Application after the changes means that you accept the updated Terms.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "\\"CarCat\\" MMC",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Baku, Azerbaijan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "By using the Application, you accept these Terms of Use and our Privacy Policy."
              }
            }
            """;

    private static final String TERMS_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "5 сентября 2026",
                "version": "3.5",
                "language": "ru",
                "company": "ООО «CarCat»",
                "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                "email": "nemat.mirzayev@carcat.app",
                "website": "https://carcat.app/",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Условия использования",
                "subtitle": "Приложение предназначено только для личного использования. При оплате услуг сторонних поставщиков CarCat выступает как агент. Приложение предоставляется «как есть»."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Введение",
                  "content": "Настоящие Условия использования («Условия») регулируют правовые отношения между ООО «CarCat» («CarCat», «мы») и пользователем («вы»). Используя Приложение, вы принимаете настоящие «Условия и нашу Политику конфиденциальности». CarCat оставляет за собой право в любое время в одностороннем порядке изменять настоящие Условия. Кроме того:\\n\\n• Личное использование: Приложение предназначено исключительно для личного использования. Оно не может использоваться в коммерческих целях, продаваться или распространяться повторно.\\n• Посредничество и платежи: При оплате услуг сторонних Поставщиков услуг (сервисов и т. д.) через Приложение CarCat выступает от вашего имени в качестве представителя (агента). CarCat не несёт ответственности за ошибки Поставщиков услуг или качество их услуг.\\n• Принцип «как есть»: Приложение предоставляется «как есть». Мы не гарантируем, что информация в любой момент будет на 100% точной или актуальной."
                },
                {
                  "id": 2,
                  "title": "2. Кто мы?",
                  "content": "Мы — Общество с ограниченной ответственностью «CarCat», прошедшее государственную регистрацию по принципу «одного окна» в соответствии с законодательством Азербайджанской Республики.\\n\\n• Полное наименование юридического лица: Общество с ограниченной ответственностью «CarCat»\\n• ИНН (VÖEN): 1309963601\\n• Регистрация информационной системы в государственном реестре: Информационная система приложения CarCat — «Система электронного управления сервисными услугами для транспортных средств (CarCat MİS)» — зарегистрирована Национальным агентством кибербезопасности при Министерстве цифрового развития и транспорта Азербайджанской Республики 31 июля 2026 года под номером İS647005001 в Государственном реестре информационных систем персональных данных.\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Веб-сайт: https://carcat.app/"
                },
                {
                  "id": 3,
                  "title": "3. Как работает Приложение?",
                  "content": "CarCat — независимая цифровая платформа, позволяющая управлять всеми вопросами, связанными с вашим автомобилем, из одного места. Получая услугу у стороннего Поставщика услуг через Приложение, вы предоставляете CarCat полномочия (агентские) приобрести эту услугу от вашего имени и передать ваши данные (в том числе данные об автомобиле и контактные данные) поставщику либо обмениваться ими в рамках интеграции. Возникающий договор заключается непосредственно между вами и Поставщиком услуг. По вопросам, возникающим в ходе оказания услуги, следует обращаться непосредственно к стороне, оказывающей услугу."
                },
                {
                  "id": 4,
                  "title": "4. Наши услуги",
                  "content": "CarCat предоставляет в Приложении следующие модули и услуги:\\n\\n• Техническое обслуживание и дополнительные услуги CarCat (Сервис и ремонт): Бронирование техосмотра, замены масла и ремонтных услуг в партнёрских сервисных центрах, а также другая информация\\n• CarCat Passport (Паспорт автомобиля): Отслеживание истории обслуживания, пробега и технических уведомлений автомобиля по VIN-коду"
                },
                {
                  "id": 5,
                  "title": "5. Кто может пользоваться Приложением?",
                  "content": "Для использования Приложения вы должны соответствовать следующим условиям:\\n\\n• Поскольку регистрация в Приложении осуществляется по номеру мобильного телефона, использовать только законно принадлежащий вам номер мобильного телефона;\\n• Использовать Приложение только в личных (некоммерческих) целях;\\n• Быть законным владельцем регистрируемого автомобиля или иметь право на управление им."
                },
                {
                  "id": 6,
                  "title": "6. Ограничение ответственности",
                  "content": "• Качество услуг: CarCat не несёт ответственности за качество ремонта, выполненного сторонними сервисными центрами, ущерб, причинённый автомобилю, или споры о ценах.\\n• Точность: Даты техосмотра или обновления, о которых Приложение напоминает автоматически, носят информационный характер. Полная ответственность за соответствие вашего автомобиля требованиям законодательства лежит на вас.\\n• Финансовое ограничение: В максимальной степени, допускаемой законом, общая финансовая ответственность CarCat за причинённый вам ущерб ограничивается суммой 1 AZN."
                },
                {
                  "id": 7,
                  "title": "7. Управление учётной записью и безопасность",
                  "content": "• Вы несёте ответственность за сохранение конфиденциальности данных вашей учётной записи.\\n• В случае утраты права владения или контроля над зарегистрированным номером мобильного телефона вы должны немедленно удалить свою учётную запись.\\n• При продаже автомобиля или замене государственного номерного знака вы должны обновить данные в Приложении.\\n• CarCat оставляет за собой право приостановить учётную запись без предупреждения в случае нарушения Условий или подозрения в мошенничестве."
                },
                {
                  "id": 8,
                  "title": "8. Обработка данных, интеграция и трансграничная передача",
                  "content": "Используя Приложение или регистрируясь в нём, вы даёте согласие на обмен вашими данными со сторонними партнёрами при оказании услуг и системной интеграции, а также на их безопасное хранение на зарубежных серверах."
                },
                {
                  "id": 9,
                  "title": "9. Жалобы и поддержка",
                  "content": "Если у вас есть вопросы, жалобы или предложения, связанные с Приложением, вы можете связаться с нами по следующим каналам:\\n\\n• В Приложении: раздел чат-поддержки\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Контактный номер: +994 70 575 75 70\\n\\nПервичный ответ на ваши обращения предоставляется в течение 5 рабочих дней, а полное решение — в срок не более 30 дней."
                },
                {
                  "id": 10,
                  "title": "10. Применимое право и подсудность",
                  "content": "Настоящие Условия регулируются законодательством Азербайджанской Республики. Споры, не урегулированные между сторонами, рассматриваются соответствующими судами города Баку."
                },
                {
                  "id": 11,
                  "title": "11. Одностороннее изменение Условий",
                  "content": "CarCat оставляет за собой право в любое время в одностороннем порядке изменять или обновлять настоящие Условия использования. Обновлённые Условия вступают в силу с момента их публикации в Приложении. Продолжение использования Приложения после внесения изменений означает ваше согласие с обновлёнными Условиями.",
                  "contact": {
                    "companyEn": "\\"CarCat\\" LLC",
                    "companyAz": "ООО «CarCat»",
                    "email": "nemat.mirzayev@carcat.app",
                    "website": "https://carcat.app/",
                    "location": "Баку, Азербайджан"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "Используя Приложение, вы принимаете настоящие Условия использования и нашу Политику конфиденциальности."
              }
            }
            """;

    // -------------------------------------------------------------------------
    // Privacy Policy v3.5 — AZ / EN / RU
    // -------------------------------------------------------------------------

    private static final String POLICY_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "5 sentyabr 2026",
                "version": "3.5",
                "language": "az",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Bakı, Azərbaycan",
                "country": "Azərbaycan Respublikası",
                "responseTime": "30 gün",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Məxfilik siyasəti",
                "subtitle": "Bu Siyasət \\"CarCat\\" MMC tərəfindən idarə olunan CarCat mobil tətbiqi və rəqəmsal platforması vasitəsilə fərdi məlumatlarınızın toplanması, işlənməsi, saxlanılması, qorunması və üçüncü tərəflərə ötürülməsi qaydalarını müəyyən edir."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Ümumi müddəalar və hüquqi əsaslar",
                  "content": "Bu Məxfilik siyasəti (\\"Siyasət\\") \\"CarCat\\" MMC (\\"CarCat\\", \\"biz\\") tərəfindən idarə olunan “CarCat” mobil tətbiqi və rəqəmsal platforması vasitəsilə istifadəçilərin (\\"siz\\") fərdi məlumatlarının toplanması, işlənməsi, saxlanılması, qorunması və üçüncü tərəflərə ötürülməsi qaydalarını müəyyən edir. Fərdi məlumatlarınızın işlənməsi Azərbaycan Respublikasının \\"Fərdi məlumatlar haqqında\\" Qanununa uyğun olaraq həyata keçirilir. Tətbiqdə qeydiyyatdan keçməklə və ya xidmətlərdən istifadə etməklə siz bu Məxfilik siyasətində göstərilən şərtlərlə fərdi məlumatlarınızın işlənməsinə razılıq vermiş olursunuz.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Məlumat nəzarətçisi və dövlət reyestr qeydiyyatı",
                  "content": "Fərdi məlumatlarınızın işlənməsinə cavabdeh olan hüquqi şəxs:\\n\\n• Hüquqi şəxsin tam adı: \\"CarCat\\" Məhdud Məsuliyyətli Cəmiyyəti\\n• VÖEN: 1309963601\\n• Dövlət reyestr qeydiyyatı: CarCat tətbiqinin informasiya sistemi — \\"Nəqliyyat vasitələrinin servis xidmətlərinin elektron idarə edilməsi sistemi (CarCat MİS)\\" AR Rəqəmsal İnkişaf və Nəqliyyat Nazirliyi yanında Milli Kibertəhlükəsizlik Agentliyi tərəfindən 31 iyul 2026-cı il tarixində İS647005001 nömrəsi ilə Fərdi Məlumatların İnformasiya Sistemlərinin Dövlət Reyestrində qeydiyyata alınmışdır.\\n• Hüquqi / əlaqə ünvanı: Bakı şəhəri, Azərbaycan\\n• E-poçt: nemat.mirzayev@carcat.app\\n• Əlaqə nömrəsi: +994 70 575 75 70 (bu nömrəyə WhatsApp vasitəsilə yazmağınız tövsiyə olunur)\\n• Veb-sayt: www.carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Toplanılan məlumatlar",
                  "content": "CarCat tətbiqi xidmətlərin keyfiyyətli və fasiləsiz göstərilməsi üçün aşağıdakı fərdi və texniki məlumatları toplayır:\\n\\n• Şəxsi identifikasiya məlumatları: Ad, soyad, mobil telefon nömrəsi, e-poçt ünvanı və sair;\\n• Avtomobil məlumatları: Avtomobilin VIN kodu, dövlət qeydiyyat nişanı, markası, modeli, buraxılış ili, cari yürüşü (odometr göstəricisi), xidmət/təmir tarixçəsi məlumatları və sair;\\n• Geolokasiya məlumatları: Ən yaxın servis mərkəzlərinin tapılması və xəritə inteqrasiyaları üçün cihazınızın naviqasiya/GPS məlumatları (icazəniz daxilində);\\n• Ödəniş və maliyyə məlumatları: Tətbiq daxilində həyata keçirilən ödənişlər zamanı bank kartı məlumatları birbaşa lisenziyalı ödəniş təşkilatları/banklar tərəfindən emal olunur; CarCat kartın tam rekvizitlərini öz serverlərində saxlamır;\\n• Texniki və cihaz məlumatları: IP ünvanı, cihaz modeli, əməliyyat sistemi versiyası, unikal cihaz identifikatorları (UUID), tətbiqdən istifadə loqları və xəta hesabatları.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Məlumatların toplanması və işlənməsi məqsədləri",
                  "content": "Toplanılan məlumatlar aşağıdakı məqsədlərlə işlənir:\\n\\n• CarCat texniki qulluq və CarCat Passport modulları üzrə xidmətlərin təşkili və avtomobilin rəqəmsal servis pasportunun formalaşdırılması;\\n• Tərəfdaş servis mərkəzlərində texniki baxış, təmir və yağdəyişmə vaxtlarının bron edilməsi və xidmət sorğularının icrası;\\n• Avtomobilin texniki baxış, sığorta, yürüş və xidmət xəbərdarlıqlarının avtomatik bildiriş (push notification/SMS) vasitəsilə istifadəçiyə çatdırılması;\\n• Tətbiqin təhlükəsizliyinin təmin edilməsi, dələduzluq hallarının qarşısının alınması və sistem tənzimləmələrinin həyata keçirilməsi;\\n• Müştəri dəstəyi xidmətinin göstərilməsi, şikayət və sorğuların cavablandırılması;\\n• Qanunvericilikdən doğan hüquqi öhdəliklərin yerinə yetirilməsi.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Məlumatların üçüncü tərəflərə ötürülməsi",
                  "content": "CarCat fərdi məlumatlarınızı üçüncü tərəflərə satmır və kommersiya məqsədilə icarəyə vermir. Məlumatlar yalnız aşağıdakı hallarda ötürülə bilər:\\n\\n• Tərəfdaş servis mərkəzlərinə: Seçdiyiniz xidmətin göstərilməsi üçün zəruri olan həcmdə (ad, telefon, avtomobilin markası/modeli və VIN kodu);\\n• Ekosistem və inteqrasiya tərəfdaşlarına: Xidmət zəncirinin təmin edilməsi, identifikasiya və məlumat dəqiqləşdirilməsi məqsədilə inteqrasiya olunmuş tərəfdaşlar;\\n• Hüquq-mühafizə və dövlət qurumlarına: Qanunla nəzərdə tutulmuş hallarda, səlahiyyətli dövlət orqanlarının rəsmi və əsaslandırılmış tələbi əsasında;\\n• Xidmət təminatçılarına: Server, bulud saxlanma, analitika və bildiriş göndərişi xidmətlərini təmin edən IT subpodratçılara (məxfiliyin qorunması öhdəlikləri çərçivəsində).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Transsərhəd məlumat ötürülməsi",
                  "content": "Tətbiqin fasiləsiz və təhlükəsiz fəaliyyətini təmin etmək üçün fərdi məlumatlarınız beynəlxalq təhlükəsizlik standartlarına cavab verən xarici bulud serverlərində (Cloud Infrastructure) saxlanıla və işlənə bilər. Tətbiqdən istifadə etməklə məlumatlarınızın təhlükəsiz transsərhəd ötürülməsinə razılıq vermiş olursunuz.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Məlumatların təhlükəsizliyi, məsuliyyət məhdudiyyəti və saxlanılma müddəti",
                  "content": "CarCat fərdi məlumatların qanunsuz girişdən, dəyişdirilmədən, açıqlanmadan və ya məhv edilmədən qorunması üçün müasir şifrələmə protokollarından, təhlükəsizlik divarlarından (Firewall) və giriş məhdudiyyəti mexanizmlərindən istifadə edir.\\n\\nMəsuliyyətin məhdudlaşdırılması: CarCat platformasının təhlükəsizliyini təmin etmək üçün bütün zəruri texniki və təşkilati tədbirlər görülsə də, gözlənilməz kiberhücumlar, zərərli proqram təminatı hücumları (malware, ransomware), genişmiqyaslı kiberqəsd, qarşısıalınmaz fors-major halları və ya qlobal infrastruktur kəsintiləri (DDoS hücumları və s.) nəticəsində yaranmış məlumat sızması, itkisi və ya xidmət kəsintilərinə görə \\"CarCat\\" MMC qanunvericiliyin icazə verdiyi maksimum həddə hüquqi və maliyyə məsuliyyəti daşımır.\\n\\nMəlumatlarınız hesabınız aktiv olduğu müddətdə saxlanılır. Hesab silindikdə və ya qanunvericiliklə müəyyən edilmiş saxlanılma müddəti bitdikdə fərdi məlumatlar təhlükəsiz şəkildə silinir və ya anonimləşdirilir.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Hesabın və məlumatların silinməsi",
                  "content": "İstifadəçi istənilən vaxt nemat.mirzayev@carcat.app e-poçt ünvanına rəsmi sorğu göndərməklə hesabının və ona bağlı fərdi məlumatların silinməsini tələb edə bilər. Sorğunuz maksimum 30 gün ərzində emal olunur.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Məxfilik siyasətində dəyişikliklər",
                  "content": "CarCat bu Məxfilik siyasətini istənilən vaxt yeniləmək hüququnu saxlayır. Yenilənmiş Siyasət tətbiqdə dərc edildiyi andan qüvvəyə minir.",
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
                "id": 10,
                "title": "10. Əlaqə və dəstək",
                "subtitle": "Bu Məxfilik siyasəti və ya fərdi məlumatlarınızın işlənməsi ilə bağlı hər hansı sualınız, sorğunuz və ya şikayətiniz olduqda aşağıdakı vasitələrlə bizimlə əlaqə saxlaya bilərsiniz:",
                "company": "\\"CarCat\\" MMC",
                "companyAz": "\\"CarCat\\" MMC",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Bakı, Azərbaycan",
                "phone": "+994 70 575 75 70",
                "responseNote": "Bu nömrəyə WhatsApp vasitəsilə yazmağınız tövsiyə olunur."
              },
              "footer": {
                "consentText": "Tətbiqdə qeydiyyatdan keçməklə və ya xidmətlərdən istifadə etməklə siz bu Məxfilik siyasətində göstərilən şərtlərlə fərdi məlumatlarınızın işlənməsinə razılıq vermiş olursunuz."
              }
            }
            """;

    private static final String POLICY_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "5 September 2026",
                "version": "3.5",
                "language": "en",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" Limited Liability Company",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "responseTime": "30 days",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Privacy Policy",
                "subtitle": "This Policy sets out the rules for the collection, processing, storage, protection and transfer to third parties of personal data through the CarCat mobile application and digital platform operated by \\"CarCat\\" LLC."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. General provisions and legal basis",
                  "content": "This Privacy Policy (\\"Policy\\") sets out the rules for the collection, processing, storage, protection and transfer to third parties of the personal data of users (\\"you\\") through the “CarCat” mobile application and digital platform operated by \\"CarCat\\" LLC (\\"CarCat\\", \\"we\\"). Your personal data is processed in accordance with the Law of the Republic of Azerbaijan \\"On Personal Data\\". By registering in the Application or using the services, you consent to the processing of your personal data under the terms set out in this Privacy Policy.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Data controller and state register registration",
                  "content": "The legal entity responsible for processing your personal data:\\n\\n• Full legal name: \\"CarCat\\" Limited Liability Company\\n• TIN (VÖEN): 1309963601\\n• State register registration: The information system of the CarCat application — \\"Electronic management system for vehicle maintenance services (CarCat MİS)\\" — was registered in the State Register of Personal Data Information Systems under number İS647005001 on 31 July 2026 by the National Cybersecurity Agency under the Ministry of Digital Development and Transport of the Republic of Azerbaijan.\\n• Legal / contact address: Baku, Azerbaijan\\n• Email: nemat.mirzayev@carcat.app\\n• Contact number: +994 70 575 75 70 (we recommend contacting this number via WhatsApp)\\n• Website: www.carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Data we collect",
                  "content": "The CarCat application collects the following personal and technical data to ensure the high-quality and uninterrupted provision of services:\\n\\n• Personal identification data: First name, last name, mobile phone number, email address, etc.;\\n• Vehicle data: Vehicle VIN code, state registration plate, make, model, year of manufacture, current mileage (odometer reading), service/repair history, etc.;\\n• Geolocation data: Your device's navigation/GPS data for finding the nearest service centres and for map integrations (subject to your permission);\\n• Payment and financial data: For payments made within the Application, bank card data is processed directly by licensed payment institutions/banks; CarCat does not store full card details on its servers;\\n• Technical and device data: IP address, device model, operating system version, unique device identifiers (UUID), application usage logs and error reports.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Purposes of data collection and processing",
                  "content": "The collected data is processed for the following purposes:\\n\\n• Organising services under the CarCat maintenance and CarCat Passport modules and creating the vehicle's digital service passport;\\n• Booking technical inspection, repair and oil change appointments at partner service centres and fulfilling service requests;\\n• Delivering vehicle technical inspection, insurance, mileage and service alerts to the user via automatic notifications (push notification/SMS);\\n• Ensuring the security of the Application, preventing fraud and carrying out system adjustments;\\n• Providing customer support and responding to complaints and requests;\\n• Fulfilling legal obligations arising from legislation.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Transfer of data to third parties",
                  "content": "CarCat does not sell your personal data to third parties or rent it out for commercial purposes. Data may be transferred only in the following cases:\\n\\n• To partner service centres: To the extent necessary to provide the service you have selected (name, phone number, vehicle make/model and VIN code);\\n• To ecosystem and integration partners: Integrated partners, for the purposes of ensuring the service chain, identification and data verification;\\n• To law enforcement and government authorities: In cases provided for by law, on the basis of an official and substantiated request from competent state authorities;\\n• To service providers: IT subcontractors providing server, cloud storage, analytics and notification delivery services (subject to confidentiality obligations).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Cross-border data transfer",
                  "content": "To ensure the uninterrupted and secure operation of the Application, your personal data may be stored and processed on foreign cloud servers (Cloud Infrastructure) that meet international security standards. By using the Application, you consent to the secure cross-border transfer of your data.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Data security, limitation of liability and retention period",
                  "content": "CarCat uses modern encryption protocols, firewalls and access restriction mechanisms to protect personal data from unlawful access, alteration, disclosure or destruction.\\n\\nLimitation of liability: Although all necessary technical and organisational measures are taken to ensure the security of the CarCat platform, \\"CarCat\\" LLC, to the maximum extent permitted by law, bears no legal or financial liability for data leaks, data loss or service interruptions resulting from unforeseen cyberattacks, malicious software attacks (malware, ransomware), large-scale cyber sabotage, force majeure circumstances or global infrastructure outages (DDoS attacks, etc.).\\n\\nYour data is retained for as long as your account is active. When the account is deleted or the retention period established by law expires, personal data is securely deleted or anonymised.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Deletion of account and data",
                  "content": "The user may at any time request the deletion of their account and the related personal data by sending an official request to nemat.mirzayev@carcat.app. Your request will be processed within a maximum of 30 days.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Changes to the Privacy Policy",
                  "content": "CarCat reserves the right to update this Privacy Policy at any time. The updated Policy takes effect from the moment it is published in the Application.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "We do not sell your data",
                "content": "CarCat does not sell your personal data to third parties or rent it out for commercial purposes.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 10,
                "title": "10. Contact and support",
                "subtitle": "If you have any questions, requests or complaints regarding this Privacy Policy or the processing of your personal data, you can contact us through the following channels:",
                "company": "\\"CarCat\\" LLC",
                "companyAz": "\\"CarCat\\" MMC",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Baku, Azerbaijan",
                "phone": "+994 70 575 75 70",
                "responseNote": "We recommend contacting this number via WhatsApp."
              },
              "footer": {
                "consentText": "By registering in the Application or using the services, you consent to the processing of your personal data under the terms set out in this Privacy Policy."
              }
            }
            """;

    private static final String POLICY_JSON_RU = """
            {
              "metadata": {
                "lastUpdated": "5 сентября 2026",
                "version": "3.5",
                "language": "ru",
                "company": "ООО «CarCat»",
                "companyAz": "Общество с ограниченной ответственностью «CarCat»",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Баку, Азербайджан",
                "country": "Азербайджанская Республика",
                "responseTime": "30 дней",
                "phone": "+994 70 575 75 70"
              },
              "header": {
                "title": "CarCat — Политика конфиденциальности",
                "subtitle": "Настоящая Политика определяет правила сбора, обработки, хранения, защиты и передачи третьим лицам персональных данных через мобильное приложение и цифровую платформу CarCat, управляемые ООО «CarCat»."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "1. Общие положения и правовые основания",
                  "content": "Настоящая Политика конфиденциальности («Политика») определяет правила сбора, обработки, хранения, защиты и передачи третьим лицам персональных данных пользователей («вы») посредством мобильного приложения и цифровой платформы «CarCat», управляемых ООО «CarCat» («CarCat», «мы»). Обработка ваших персональных данных осуществляется в соответствии с Законом Азербайджанской Республики «О персональных данных». Регистрируясь в Приложении или пользуясь услугами, вы даёте согласие на обработку ваших персональных данных на условиях, изложенных в настоящей Политике конфиденциальности.",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "2. Оператор персональных данных и регистрация в государственном реестре",
                  "content": "Юридическое лицо, ответственное за обработку ваших персональных данных:\\n\\n• Полное наименование юридического лица: Общество с ограниченной ответственностью «CarCat»\\n• ИНН (VÖEN): 1309963601\\n• Регистрация в государственном реестре: Информационная система приложения CarCat — «Система электронного управления сервисными услугами для транспортных средств (CarCat MİS)» — зарегистрирована Национальным агентством кибербезопасности при Министерстве цифрового развития и транспорта Азербайджанской Республики 31 июля 2026 года под номером İS647005001 в Государственном реестре информационных систем персональных данных.\\n• Юридический / контактный адрес: город Баку, Азербайджан\\n• Эл. почта: nemat.mirzayev@carcat.app\\n• Контактный номер: +994 70 575 75 70 (рекомендуем писать на этот номер через WhatsApp)\\n• Веб-сайт: www.carcat.app",
                  "icon": "business_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "3. Собираемые данные",
                  "content": "Приложение CarCat собирает следующие персональные и технические данные для качественного и бесперебойного оказания услуг:\\n\\n• Данные для идентификации личности: Имя, фамилия, номер мобильного телефона, адрес электронной почты и т. д.;\\n• Данные об автомобиле: VIN-код автомобиля, государственный регистрационный знак, марка, модель, год выпуска, текущий пробег (показания одометра), история обслуживания/ремонта и т. д.;\\n• Данные геолокации: Навигационные/GPS-данные вашего устройства для поиска ближайших сервисных центров и интеграции с картами (с вашего разрешения);\\n• Платёжные и финансовые данные: При оплате в Приложении данные банковской карты обрабатываются непосредственно лицензированными платёжными организациями/банками; CarCat не хранит полные реквизиты карты на своих серверах;\\n• Технические данные и данные устройства: IP-адрес, модель устройства, версия операционной системы, уникальные идентификаторы устройства (UUID), журналы использования приложения и отчёты об ошибках.",
                  "icon": "folder_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "4. Цели сбора и обработки данных",
                  "content": "Собранные данные обрабатываются в следующих целях:\\n\\n• Организация услуг в рамках модулей технического обслуживания CarCat и CarCat Passport и формирование цифрового сервисного паспорта автомобиля;\\n• Бронирование времени техосмотра, ремонта и замены масла в партнёрских сервисных центрах и выполнение запросов на обслуживание;\\n• Доведение до пользователя уведомлений о техосмотре, страховании, пробеге и обслуживании автомобиля посредством автоматических оповещений (push-уведомления/SMS);\\n• Обеспечение безопасности Приложения, предотвращение мошенничества и выполнение системных настроек;\\n• Оказание клиентской поддержки, ответы на жалобы и запросы;\\n• Выполнение правовых обязательств, вытекающих из законодательства.",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "5. Передача данных третьим лицам",
                  "content": "CarCat не продаёт ваши персональные данные третьим лицам и не предоставляет их в аренду в коммерческих целях. Данные могут передаваться только в следующих случаях:\\n\\n• Партнёрским сервисным центрам: В объёме, необходимом для оказания выбранной вами услуги (имя, телефон, марка/модель автомобиля и VIN-код);\\n• Партнёрам экосистемы и интеграции: Интегрированным партнёрам в целях обеспечения цепочки услуг, идентификации и уточнения данных;\\n• Правоохранительным и государственным органам: В случаях, предусмотренных законом, на основании официального и обоснованного запроса уполномоченных государственных органов;\\n• Поставщикам услуг: ИТ-субподрядчикам, предоставляющим услуги серверов, облачного хранения, аналитики и отправки уведомлений (в рамках обязательств по соблюдению конфиденциальности).",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "6. Трансграничная передача данных",
                  "content": "Для обеспечения бесперебойной и безопасной работы Приложения ваши персональные данные могут храниться и обрабатываться на зарубежных облачных серверах (Cloud Infrastructure), соответствующих международным стандартам безопасности. Используя Приложение, вы даёте согласие на безопасную трансграничную передачу ваших данных.",
                  "icon": "public_outlined",
                  "highlighted": false
                },
                {
                  "id": 7,
                  "title": "7. Безопасность данных, ограничение ответственности и срок хранения",
                  "content": "CarCat использует современные протоколы шифрования, межсетевые экраны (Firewall) и механизмы ограничения доступа для защиты персональных данных от незаконного доступа, изменения, раскрытия или уничтожения.\\n\\nОграничение ответственности: Несмотря на принятие всех необходимых технических и организационных мер для обеспечения безопасности платформы CarCat, ООО «CarCat» в максимальной степени, допускаемой законодательством, не несёт правовой и финансовой ответственности за утечку или утрату данных либо перебои в работе сервиса, возникшие в результате непредвиденных кибератак, атак вредоносного программного обеспечения (malware, ransomware), масштабных кибердиверсий, непреодолимых форс-мажорных обстоятельств или глобальных сбоев инфраструктуры (DDoS-атаки и т. д.).\\n\\nВаши данные хранятся в течение всего срока действия вашей учётной записи. При удалении учётной записи или по истечении установленного законодательством срока хранения персональные данные безопасно удаляются или обезличиваются.",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "8. Удаление учётной записи и данных",
                  "content": "Пользователь в любое время может потребовать удаления своей учётной записи и связанных с ней персональных данных, направив официальный запрос на адрес электронной почты nemat.mirzayev@carcat.app. Ваш запрос будет обработан в срок не более 30 дней.",
                  "icon": "delete_outline",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "9. Изменения в Политике конфиденциальности",
                  "content": "CarCat оставляет за собой право в любое время обновлять настоящую Политику конфиденциальности. Обновлённая Политика вступает в силу с момента её публикации в Приложении.",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_selling",
                "title": "Данные не продаются",
                "content": "CarCat не продаёт ваши персональные данные третьим лицам и не предоставляет их в аренду в коммерческих целях.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 10,
                "title": "10. Контакты и поддержка",
                "subtitle": "Если у вас есть вопросы, запросы или жалобы, связанные с настоящей Политикой конфиденциальности или обработкой ваших персональных данных, вы можете связаться с нами следующими способами:",
                "company": "ООО «CarCat»",
                "companyAz": "ООО «CarCat»",
                "email": "nemat.mirzayev@carcat.app",
                "website": "www.carcat.app",
                "location": "Баку, Азербайджан",
                "phone": "+994 70 575 75 70",
                "responseNote": "Рекомендуем писать на этот номер через WhatsApp."
              },
              "footer": {
                "consentText": "Регистрируясь в Приложении или пользуясь услугами, вы даёте согласие на обработку ваших персональных данных на условиях, изложенных в настоящей Политике конфиденциальности."
              }
            }
            """;
}
