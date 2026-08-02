package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.PrivacyPolicyResponse;
import com.carland.carland_service.dto.response.TermsConditionsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * tr: Hukuki içerik REST controller'ı; sabit JSON metinlerinden kullanım koşulları (terms) ve gizlilik politikasını (privacy policy) az/en dillerinde döner.
 * en: REST controller for legal content; serves the terms and conditions and privacy policy in az/en languages from hardcoded JSON strings.
 */
@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
@Slf4j
public class LegalController {


    /**
     * tr: lang parametresine göre (varsayılan "az") kullanım koşullarını JSON olarak döner; JSON parse hatasında 400 Bad Request döner.
     * en: Returns the terms and conditions as JSON based on the lang parameter (default "az"); returns 400 Bad Request if JSON parsing fails.
     */
    @GetMapping("/terms-and-conditions")
    public ResponseEntity<JsonNode> getTerms(
            @RequestParam(value = "lang", defaultValue = "az") String lang) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);


            if (lang.equalsIgnoreCase("az")) {

                JsonNode nodeAz = mapper.readTree(TERMS_JSON_AZ);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nodeAz);
            } else {

                JsonNode nodeEn = mapper.readTree(TERMS_JSON_EN);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nodeEn);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * tr: lang parametresine göre (varsayılan "az") gizlilik politikasını JSON olarak döner; JSON parse hatasında 400 Bad Request döner.
     * en: Returns the privacy policy as JSON based on the lang parameter (default "az"); returns 400 Bad Request if JSON parsing fails.
     */
    @GetMapping("/privacy/policy")
    public ResponseEntity<JsonNode> getPolicy(
            @RequestParam(value = "lang", defaultValue = "az") String lang) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);


            if (lang.equalsIgnoreCase("az")) {

                JsonNode nodeAz = mapper.readTree(POLICY_JSON_AZ);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nodeAz);
            } else {

                JsonNode nodeEn = mapper.readTree(POLICY_JSON_EN);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(nodeEn);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private static final String POLICY_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "December 25, 2025",
                "version": "1.0",
                "language": "en",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan",
                "responseTime": "30 days"
              },
              "header": {
                "title": "Your Privacy Matters",
                "subtitle": "Digital Innovation Agency LLC is committed to protecting your privacy. This Privacy Policy explains how we collect, use, store, and protect your personal information when you use CarCat."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "Information We Collect",
                  "content": "1.1 Personal Information\\n• Name\\n• Phone Number\\n• Profile Photo (Optional)\\n• Email Address\\n\\n1.2 Vehicle Information\\n• Vehicle Identification Number (VIN)\\n• License Plate Number\\n• Brand, Model, and Year\\n• Engine Type and Transmission\\n• Current and Historical Mileage\\n• Vehicle Photos (Optional)\\n\\n1.3 Service History Data\\n• Bookings and appointments\\n• Service dates and types\\n• Maintenance schedules\\n\\n1.4 Information We Do NOT Collect\\n• Location/GPS data\\n• Payment information\\n• Browsing history\\n• Biometric information",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "How We Use Your Information",
                  "content": "We use your information solely for:\\n\\n• Creating and managing your account\\n• Registering and managing vehicles\\n• Facilitating service bookings\\n• Tracking service history\\n• Sending service reminders\\n• Responding to support requests\\n• Improving app features\\n• Fixing technical issues",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "How We Store Your Information",
                  "content": "3.1 API-Based Storage\\nWe do not store data locally on your device. All information is:\\n\\n• Stored securely on our servers\\n• Encrypted during transmission (HTTPS/TLS)\\n• Protected with industry-standard security\\n• Backed up regularly\\n\\n3.2 Data Retention\\n• Active accounts: Data retained as long as account is active\\n• After deletion: Data deleted within 90 days",
                  "icon": "cloud_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "Data Sharing and Disclosure",
                  "content": "4.1 Service Providers\\nWhen you book an appointment, we share only necessary information:\\n• Your name and phone number\\n• Vehicle details\\n• Service history (if relevant)\\n\\n4.2 Legal Requirements\\nWe may disclose information if required to comply with legal obligations or protect our rights.",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "Firebase Cloud Messaging",
                  "content": "We use Firebase Cloud Messaging to send:\\n\\n• Service reminders\\n• Booking confirmations\\n• Maintenance notifications\\n• App updates\\n\\nYou may disable notifications in your device settings.\\n\\nNote: We currently do not actively send push notifications, but the infrastructure is in place.",
                  "icon": "notifications_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "Your Data Rights",
                  "content": "You have the following rights regarding your data:",
                  "icon": "verified_user_outlined",
                  "highlighted": false,
                  "dataRights": [
                    {
                      "icon": "visibility_outlined",
                      "title": "Access",
                      "description": "You can view all your information within the app at any time."
                    },
                    {
                      "icon": "edit_outlined",
                      "title": "Correction",
                      "description": "You can edit or update your information directly in the app."
                    },
                    {
                      "icon": "delete_outline",
                      "title": "Deletion",
                      "description": "Delete vehicles in-app or contact us for full account deletion within 90 days."
                    },
                    {
                      "icon": "download_outlined",
                      "title": "Data Portability",
                      "description": "Request a copy of your data in machine-readable format."
                    }
                  ]
                },
                {
                  "id": 7,
                  "title": "Data Security",
                  "content": "We implement:\\n\\nTechnical Safeguards:\\n• Encryption of data in transit\\n• Secure API authentication\\n• Regular security audits\\n• Protection against unauthorized access\\n\\nYour Responsibility:\\n• Keep login credentials confidential\\n• Use strong, unique passwords\\n• Log out on shared devices\\n• Report suspicious activity",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "Children's Privacy",
                  "content": "While CarCat does not have a strict minimum age requirement (to allow registration of family vehicles), we do not knowingly collect information from children under 13 without parental consent.\\n\\nIf you are under 18:\\n• Use CarCat with parental guidance\\n• You may register vehicles on behalf of family members\\n• Parents/guardians are responsible for monitoring usage",
                  "icon": "family_restroom_outlined",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "Third-Party Services",
                  "content": "9.1 Google ML Kit\\nWe use Google ML Kit for VIN scanning. Processing happens on-device and scanned VIN data is not transmitted to Google.\\n\\n9.2 Firebase Cloud Messaging\\nWe use Firebase for push notifications. Firebase may collect device identifiers.\\n\\n9.3 No Other Third Parties\\nWe do not integrate with analytics, advertising, social media, or payment processors.",
                  "icon": "integration_instructions_outlined",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "Changes to This Privacy Policy",
                  "content": "We may update this Privacy Policy from time to time. When we make changes:\\n\\n• We will update the \\"Last Updated\\" date\\n• We will notify you through the app or email\\n• Continued use constitutes acceptance",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_sharing",
                "title": "We Do NOT Share Your Data",
                "content": "Your personal information is never sold, rented, or shared with third parties for marketing purposes.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 11,
                "title": "Contact Us",
                "subtitle": "If you have questions or requests regarding this Privacy Policy:",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Baku, Azerbaijan",
                "responseNote": "We will respond to your inquiry within 30 days"
              },
              "footer": {
                "consentText": "By using CarCat, you acknowledge that you have read, understood, and agree to this Privacy Policy."
              }
            }
            """;


    private static final String POLICY_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "25 Dekabr 2025",
                "version": "1.0",
                "language": "az",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Bakı, Azərbaijan",
                "country": "Azərbaijan Respublikası",
                "responseTime": "30 gün"
              },
              "header": {
                "title": "Sizin Məxfilikləriniz Əhəmiyyətlidir",
                "subtitle": "Digital Innovation Agency LLC sizin məxfiliklərinizi qorumağa tərəfdardir. Bu Məxfilik Siyasəti CarCat istifadə edərkən şəxsi məlumatlarınızı necə topladığımızı, istifadə etdiyimizi, saxladığımızı və qorumağımızı izah edir."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "Topladığımız Məlumatlar",
                  "content": "1.1 Şəxsi Məlumat\\n• Ad\\n• Telefon Nömrəsi\\n• Profil Fotosu (İsteğe Bağlı)\\n• E-poçt Ünvanı\\n\\n1.2 Avtomobil Məlumatı\\n• Avtomobil Kimlik Nömrəsi (VIN)\\n• Avtomobil Nömrə Boşluğu\\n• Marka, Model və İl\\n• Mühərrrik Tipi və Transmission\\n• Cari və Tarixi Yürüş\\n• Avtomobil Şəkilləri (İsteğe Bağlı)\\n\\n1.3 Xidmət Tarixi Məlumatı\\n• Sifariş və görüşlər\\n• Xidmət tarixləri və növləri\\n• Texniki xidmət cədrəlləri\\n\\n1.4 Toplamadığımız Məlumatlar\\n• Məkan/GPS məlumatı\\n• Ödəniş məlumatı\\n• Baxış tarixi\\n• Biyometrik məlumat",
                  "icon": "info_outline",
                  "highlighted": false
                },
                {
                  "id": 2,
                  "title": "Məlumatlarınızı Necə İstifadə Edirik",
                  "content": "Məlumatlarınızı yalnız aşağıdakılar üçün istifadə edirik:\\n\\n• Hesab yaratmaq və idarə etmək\\n• Avtomobilləri qeydiyyatdan keçirmək və idarə etmək\\n• Xidmət sifariş etməni həyata keçirmək\\n• Xidmət tarixini izləmək\\n• Xidmət xəbərdarlıqları göndərmək\\n• Dəstək sorğularına cavab vermək\\n• Tətbiq funksiyalarını yaxşılaşdırmaq\\n• Texniki problemləri düzəltmək",
                  "icon": "settings_outlined",
                  "highlighted": false
                },
                {
                  "id": 3,
                  "title": "Məlumatlarınızı Necə Saxlayırıq",
                  "content": "3.1 API-ə Əsaslanan Saxlama\\nBiz cihazınızda məlumatları yerli olaraq saxlamırıq. Bütün məlumatlar:\\n\\n• Bizim serverlərimizdə təhlükəli şəkildə saxlanır\\n• Ötürülmə zamanı şifrələnir (HTTPS/TLS)\\n• Sənaye standart təhlükəsizliyi ilə qorunur\\n• Mütəmadi olaraq yedəkləndirilib\\n\\n3.2 Məlumat Saxlama Müddəti\\n• Fəal hesablar: Hesab fəal olduğu müddətçə saxlanır\\n• Silindikdən sonra: 90 gün içində silinir",
                  "icon": "cloud_outlined",
                  "highlighted": false
                },
                {
                  "id": 4,
                  "title": "Məlumatın Paylaşılması və Açıqlanması",
                  "content": "4.1 Xidmət Göstəriciləri\\nGörüş ayrıldığınız zaman, biz yalnız lazımi məlumatları paylaşırıq:\\n• Sizin ad və telefon nömrəniz\\n• Avtomobil təfsilatlari\\n• Xidmət tarixi (əgər müvafiqdirsə)\\n\\n4.2 Hüquqi Tələblər\\nHüquqi yükümlülüklərə uyğun olmaq və ya hüquqlarımızı qorumuş üçün məlumatları açıqlamağa məcbur ola bilərik.",
                  "icon": "share_outlined",
                  "highlighted": false
                },
                {
                  "id": 5,
                  "title": "Firebase Cloud Messaging",
                  "content": "Biz Firebase Cloud Messaging istifadə edərək göndəririk:\\n\\n• Xidmət xəbərdarlıqları\\n• Sifariş təsdiqlərini\\n• Texniki xidmət bildirişlərini\\n• Tətbiq yeniliklərini\\n\\nCihaz parametrlərində bildirişləri söndürə bilərsiniz.\\n\\nQeyd: Biz hazırda fəal olaraq push bildirişləri göndərmirik, lakin infrastruktur hazırdır.",
                  "icon": "notifications_outlined",
                  "highlighted": false
                },
                {
                  "id": 6,
                  "title": "Sizin Məlumatınızın Hüquqları",
                  "content": "Məlumatlarınıza nisbətən aşağıdakı hüquqlara sahibsiniz:",
                  "icon": "verified_user_outlined",
                  "highlighted": false,
                  "dataRights": [
                    {
                      "icon": "visibility_outlined",
                      "title": "Daxil Olmaq",
                      "description": "İstənilən zaman tətbiq daxilində bütün məlumatlarınızı görə bilərsiniz."
                    },
                    {
                      "icon": "edit_outlined",
                      "title": "Düzəltmə",
                      "description": "Tətbiq daxilində məlumatlarınızı birbaşa redaktə və yeniləyə bilərsiniz."
                    },
                    {
                      "icon": "delete_outline",
                      "title": "Silmə",
                      "description": "Avtomobilləri tətbiq daxilində silin ya da tam hesab silinməsi üçün 90 gün daxilində bizimlə əlaqə saxlayın."
                    },
                    {
                      "icon": "download_outlined",
                      "title": "Məlumat Tənqidiyyəti",
                      "description": "Məlumatlarınızın nüsxəsini maşın-oxunaqlı formatda istəyin."
                    }
                  ]
                },
                {
                  "id": 7,
                  "title": "Məlumat Təhlükəsizliyi",
                  "content": "Biz həyata keçiririk:\\n\\nTexniki Tədbirlər:\\n• Ötürüşdə məlumatın şifrələnməsi\\n• Təhlükəsiz API autentifikasiyası\\n• Müntəzəm təhlükəsizlik auditləri\\n• İcazəsiz girişə qarşı qorunma\\n\\nSizin Məsuliyyətiniz:\\n• Giriş etimadnamələrini surətli saxlamaq\\n• Güclü, unikal parollar istifadə etmək\\n• Ortaq cihazlarda çıxmaq\\n• Şübhəli fəaliyyətı bildirmək",
                  "icon": "security_outlined",
                  "highlighted": false
                },
                {
                  "id": 8,
                  "title": "Uşaqların Məxfilikliyi",
                  "content": "CarCat ciddi minimum yaş tələbi olmasa da (ailə avtomobilləri qeydiyyatı üçün), biz 13 yaşdan kiçik uşaqlardan məzuniyyət olmadan məlumatları sammak bilmirik.\\n\\nEğər 18 yaşdan kiçiksəniz:\\n• CarCat'ı valideyn bələdçiliyi ilə istifadə edin\\n• Ailə üzvləri adından avtomobilləri qeydiyyatdan keçirə bilərsiniz\\n• Valideynlər/himayədarlar istifadə izarətindən məsuldur",
                  "icon": "family_restroom_outlined",
                  "highlighted": false
                },
                {
                  "id": 9,
                  "title": "Üçüncü Tərəf Xidmətləri",
                  "content": "9.1 Google ML Kit\\nBiz VIN skanı üçün Google ML Kit istifadə edirik. Emal cihazda baş verir və skanlanmış VIN məlumatı Google'a ötürülmür.\\n\\n9.2 Firebase Cloud Messaging\\nBiz push bildirişləri üçün Firebase istifadə edirik. Firebase cihaz identifikatorlarını toplaya bilər.\\n\\n9.3 Başqa Üçüncü Tərəflər Yoxdur\\nBiz analitika, reklam, sosial media və ya ödəniş işlətçiləri ilə inteqrasyon etmirik.",
                  "icon": "integration_instructions_outlined",
                  "highlighted": false
                },
                {
                  "id": 10,
                  "title": "Bu Məxfilik Siyasətində Dəyişikliklər",
                  "content": "Biz bu Məxfilik Siyasətini zaman-zaman yeniləyə bilərik. Dəyişikliklər edəndə:\\n\\n• \\"Son Yenilənən\\" tarixi yeniləyəcəyik\\n• Sizi tətbiq vasitəsilə və ya e-poçt yolu ilə xəbərdar edəcəyik\\n• Dəyişikliklərdən sonra istifadəyə davam qəbulun göstəricidir",
                  "icon": "update_outlined",
                  "highlighted": false
                }
              ],
              "highlightedBox": {
                "id": "no_data_sharing",
                "title": "Biz Məlumatlarınızı Paylaşmırıq",
                "content": "Sizin şəxsi məlumatlarınız heç vaxt satılmır, kirayə verilmir və ya marketinq məqsədləri üçün üçüncü tərəflərə paylaşılmır.",
                "icon": "shield_outlined"
              },
              "contactSection": {
                "id": 11,
                "title": "Bizimlə Əlaqə Saxlayın",
                "subtitle": "Bu Məxfilik Siyasəti haqqında suallarınız və ya sorğularınız varsa:",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Bakı, Azərbaijan",
                "responseNote": "Sorğuya 30 gün içində cavab verəcəyik"
              },
              "footer": {
                "consentText": "CarCat istifadə edərək, bu Məxfilik Siyasətini oxuduğunuzu, başa düşdüyünüzü və qəbul etdiyinizi təsdiq edirsiniz."
              }
            }
            """;


    private static final String TERMS_JSON_EN = """
            {
              "metadata": {
                "lastUpdated": "December 25, 2025",
                "version": "1.0",
                "language": "en",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Baku, Azerbaijan",
                "country": "Republic of Azerbaijan"
              },
              "header": {
                "title": "Welcome to CarCat",
                "subtitle": "These Terms and Conditions govern your access to and use of the CarCat mobile application and related services operated by Digital Innovation Agency LLC, registered in Baku, Azerbaijan."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "Acceptance of Terms",
                  "content": "By creating an account, accessing, or using CarCat, you acknowledge that you have read, understood, and agree to be bound by these Terms and our Privacy Policy. If you do not agree to these Terms, you must not use the Service."
                },
                {
                  "id": 2,
                  "title": "About CarCat",
                  "content": "CarCat is a digital platform that connects vehicle owners with automotive service providers. We facilitate:\\n\\n• Vehicle registration and management\\n• Service history tracking\\n• Booking appointments with automotive service centers\\n• Maintenance reminders and notifications\\n\\nImportant: CarCat acts solely as an intermediary platform. We do not provide automotive repair or maintenance services directly. All services are performed by independent third-party service providers."
                },
                {
                  "id": 3,
                  "title": "Eligibility",
                  "content": "3.1 Age Requirements\\nWhile there is no strict minimum age requirement to use CarCat, users under the age of 18 may register vehicles on behalf of family members or other authorized individuals.\\n\\n3.2 Account Registration\\nTo use certain features of CarCat, you must:\\n\\n• Provide accurate, current, and complete information\\n• Maintain and update your account information\\n• Keep your login credentials secure\\n• Notify us of any unauthorized access"
                },
                {
                  "id": 4,
                  "title": "User Responsibilities",
                  "content": "4.1 Vehicle Information\\nYou are responsible for ensuring that all vehicle information you provide is accurate and up-to-date.\\n\\n4.2 Prohibited Conduct\\nYou agree not to:\\n\\n• Provide false or fraudulent information\\n• Use the Service for unlawful purposes\\n• Interfere with or disrupt the Service\\n• Attempt unauthorized access\\n• Impersonate any person or entity\\n• Upload malicious content\\n\\n4.3 Third-Party Services\\nWhen you book appointments, you enter into a direct relationship with the service provider. We are not responsible for service quality, disputes, or vehicle damage."
                },
                {
                  "id": 5,
                  "title": "Service Availability",
                  "content": "We strive to keep CarCat available 24/7, but we do not guarantee uninterrupted access. The Service may be temporarily unavailable due to maintenance, technical issues, or network failures.\\n\\nWe reserve the right to modify, suspend, or discontinue any part of the Service at any time."
                },
                {
                  "id": 6,
                  "title": "Bookings and Appointments",
                  "content": "6.1 Booking Process\\nCarCat allows you to book appointments with service providers. By making a booking, you agree to attend or cancel according to the provider's policy.\\n\\n6.2 No Payment Processing\\nCarCat does not currently process payments. All financial transactions are made directly with service providers.\\n\\n6.3 Cancellations\\nCancellation policies are set by individual service providers."
                },
                {
                  "id": 7,
                  "title": "Intellectual Property Rights",
                  "content": "All content, features, and functionality of CarCat are the exclusive property of Digital Innovation Agency LLC and are protected by international copyright and trademark laws.\\n\\nBy uploading vehicle photos or content, you grant us a non-exclusive license to use such content solely for providing the Service. You retain ownership and may delete it anytime."
                },
                {
                  "id": 8,
                  "title": "Privacy and Data Protection",
                  "content": "We collect and process personal information as described in our Privacy Policy, including:\\n\\n• Name and contact information\\n• Phone number\\n• Vehicle information (VIN, plate, model, mileage)\\n• Vehicle and profile photos (optional)\\n• Service history\\n\\nYour data is used solely to provide and improve the Service. You may request deletion at any time."
                },
                {
                  "id": 9,
                  "title": "Machine Learning and Automation",
                  "content": "CarCat uses Google ML Kit for VIN scanning functionality. This technology helps you quickly register vehicles and reduce errors. VIN scanning data is processed on-device and not shared with third parties."
                },
                {
                  "id": 10,
                  "title": "Disclaimers and Limitation of Liability",
                  "content": "IMPORTANT: CarCat is a platform only. We do not employ or control service providers, guarantee service quality, or assume responsibility for service provider actions.\\n\\nAll service-related issues including incorrect repairs, vehicle damage, or pricing disputes must be resolved directly with the service provider.\\n\\nTO THE MAXIMUM EXTENT PERMITTED BY LAW:\\n• CarCat is provided \\"AS IS\\" without warranties\\n• We are not liable for any damages\\n• Our total liability shall not exceed $0"
                },
                {
                  "id": 11,
                  "title": "Disputes and Resolution",
                  "content": "Disputes between users and service providers must be resolved directly. We encourage communication through:\\n\\n• The in-app support section\\n• Direct contact with the provider\\n• Email to digital.innovation.agency.aze@gmail.com\\n\\nThese Terms are governed by the laws of the Republic of Azerbaijan."
                },
                {
                  "id": 12,
                  "title": "Account Termination",
                  "content": "You may terminate your account anytime through app settings or by contacting us.\\n\\nWe may suspend or terminate your account if you violate these Terms, engage in fraudulent activity, or provide false information."
                },
                {
                  "id": 13,
                  "title": "Changes to These Terms",
                  "content": "We may update these Terms from time to time. We will notify you through the app or via email. Continued use after changes constitutes acceptance."
                },
                {
                  "id": 14,
                  "title": "Contact Information",
                  "content": "For any questions or concerns regarding these Terms, please contact us at:",
                  "contact": {
                    "companyEn": "Digital Innovation Agency LLC",
                    "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                    "email": "digital.innovation.agency.aze@gmail.com",
                    "website": "https://digital-innovation.agency/",
                    "location": "Baku, Azerbaijan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "By using CarCat, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions."
              }
            }
            """;


    private static final String TERMS_JSON_AZ = """
            {
              "metadata": {
                "lastUpdated": "25 Dekabr 2025",
                "version": "1.0",
                "language": "az",
                "company": "Digital Innovation Agency LLC",
                "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                "email": "digital.innovation.agency.aze@gmail.com",
                "website": "https://digital-innovation.agency/",
                "location": "Bakı, Azərbaijan",
                "country": "Azərbaijan Respublikası"
              },
              "header": {
                "title": "CarCat'ə Xoş Gəldiniz",
                "subtitle": "Bu Şərtlər və Müəyyənliklər Digital Innovation Agency LLC tərəfindən işlədilən CarCat mobil tətbiqinə və əlaqəli xidmətlərə daxil olmaq və istifadə etməklə idarə olunur. Bakı, Azərbaijan şəhərində qeydiyyatdan keçmişdir."
              },
              "sections": [
                {
                  "id": 1,
                  "title": "Şərtlərin Qəbulu",
                  "content": "Hesab yaradıb, CarCat'ə daxil olub və ya istifadə edərək, bu Şərtlər və Məxfilik Siyasətimizi oxuduğunuzu, başa düşdüyünüzü və qəbul etdiyinizi təsdiq edirsiniz. Bu Şərtləri qəbul etmirsinizsə, Xidmətdən istifadə etməməlisiniz."
                },
                {
                  "id": 2,
                  "title": "CarCat Haqqında",
                  "content": "CarCat avtomobil sahiblərini avtomobil xidmət göstəriciləri ilə birləşdirən rəqəmsal platformadır. Biz aşağıdakıları təmin edirik:\\n\\n• Avtomobil qeydiyyatı və idarə edilməsi\\n• Xidmət tarixinin izlənməsi\\n• Avtomobil xidmət mərkəzləri ilə görüşlər əylənməsi\\n• Texniki xidmət xəbərdarlıqları və bildirişləri\\n\\nÖnəmli: CarCat yalnız vasitəçi platformasıdır. Biz birbaşa avtomobil təmir və ya texniki xidmət göstərmərik. Bütün xidmətlər müstəqil üçüncü tərəf xidmət göstəriciləri tərəfindən təmin edilir."
                },
                {
                  "id": 3,
                  "title": "Uyğunluq",
                  "content": "3.1 Yaş Tələbələri\\nCarCat istifadə etmək üçün ciddi minimum yaş tələbi olmasa da, 18 yaşından kiçik istifadəçilər ailə üzvləri və ya digər icazəli şəxslər adından avtomobil qeydiyyatından keçə bilərlər.\\n\\n3.2 Hesab Qeydiyyatı\\nCarCat'ın müəyyən funksiyalarından istifadə etmək üçün siz:\\n\\n• Dəqiq, cari və tam məlumat təmin etməlisiniz\\n• Hesab məlumatlarınızı saxlamalı və yeniləməlisiniz\\n• Giriş kredensiallarınızı təhlükəsiz saxlamalısınız\\n• İcazəsiz girişin olması halında bizi xəbərdar etməlisiniz"
                },
                {
                  "id": 4,
                  "title": "İstifadəçi Məsuliyyətləri",
                  "content": "4.1 Avtomobil Məlumatı\\nSiz təmin etdiyiniz bütün avtomobil məlumatının dəqiq və cari olmasından məsul olursunuz.\\n\\n4.2 Qadağan Edilən Fəaliyyət\\nSiz aşağıdakıları etməməyə razı olursunuz:\\n\\n• Yalan və ya saxtalaşdırılmış məlumat təmin etmək\\n• Xidmətdən qanunisiz məqsədlər üçün istifadə etmək\\n• Xidmətə müdaxilə və ya kəsilmə\\n• İcazəsiz girişə cəhd etmək\\n• Hər hansı bir şəxs və ya qurum kimligində istifadə etmək\\n• Zərərli məzmun yükləmək\\n\\n4.3 Üçüncü Tərəf Xidmətləri\\nGörüş ayarlandığınız zaman, siz xidmət göstərici ilə birbaşa əlaqə qurursunuz. Biz xidmət keyfiyyətinə, mübahisələrə və ya avtomobil zədəsinə məsul deyilik."
                },
                {
                  "id": 5,
                  "title": "Xidmət Mövcudluğu",
                  "content": "Biz CarCat'ı 24/7 mövcud saxlamağa çalışırıq, ancaq fasilələr olmadan istifadə etməyi zəmanət vermərik. Xidmət texniki xidmət, texniki problemlər və ya şəbəkə nasazlıqları səbəbindən müvəqqətən mövcud olmaya bilər.\\n\\nBiz istənilən vaxt Xidmətin hər hansı bir hissəsini dəyişdirmək, dayandırmaq və ya ləğv etmək hüququnun mahfuz olmasını elan edirik."
                },
                {
                  "id": 6,
                  "title": "Sifariş və Görüşlər",
                  "content": "6.1 Sifariş Prosesi\\nCarCat xidmət göstəriciləri ilə görüşlər əylənməyə imkan verir. Sifariş edərək, siz göstəricinin siyasətinə uyğun şəkildə hazır qalmağa və ya ləğv etməyə razı olursunuz.\\n\\n6.2 Ödəniş Emalı Yoxdur\\nCarCat hazırda ödənişləri emal etmir. Bütün maliyyə əməliyyatları birbaşa xidmət göstəriciləri ilə həyata keçirilir.\\n\\n6.3 Ləğv Edilməsi\\nLəğv siyasətləri fərdi xidmət göstəriciləri tərəfindən müəyyən edilir."
                },
                {
                  "id": 7,
                  "title": "Əqli Mülkiyyət Hüquqları",
                  "content": "CarCat'ın bütün məzmunu, xüsusiyyətləri və funksionallığı Digital Innovation Agency LLC'nin mülkiyyətidir və beynəlxalq müəlliflik və ticarət nişanı qanunları ilə qorunur.\\n\\nAvtomobil şəkilləri və ya məzmun yükləməklə, siz bizə belə məzmunu yalnız Xidməti təmin etmək üçün istifadə etmək üçün lisenziya verirsiniz. Siz mülkiyyətə sahibsiniz və istənilən zaman məlumatların silinməsini tələb edə bilərsiniz."
                },
                {
                  "id": 8,
                  "title": "Məxfilik və Məlumatın Qorunması",
                  "content": "Biz Məxfilik Siyasətində təsvir edildiyi kimi şəxsi məlumatları toplayırıq və emal edirik:\\n\\n• Ad və əlaqə məlumatı\\n• Telefon nömrəsi\\n• Avtomobil məlumatı (VIN, rəqəm, model, yürüş)\\n• Avtomobil və profil fotoları (isteğe bağlı)\\n• Xidmət tarixi\\n\\nSizin məlumatlarınız yalnız Xidməti təmin etmək və təbliğ etmək üçün istifadə olunur. İstənilən zaman silməyi tələb edə bilərsiniz."
                },
                {
                  "id": 9,
                  "title": "Maşın Təlimi və Avtomatlaşdırma",
                  "content": "CarCat VIN skanı funksionallığı üçün Google ML Kit istifadə edir. Bu texnologiya avtomobilləri tez qeydiyyatdan keçirməyə və səhvləri azaltmağa kömək edir. VIN skanlama məlumatları cihazda emal olunur və üçüncü tərəflərə paylaşılmır."
                },
                {
                  "id": 10,
                  "title": "Məzənnə və Məsuliyyət Məhdudlaşması",
                  "content": "Vacib məlumat: CarCat yalnız bir platformadır. Biz xidmət göstəriciləri işə götürmürik və ya idarə etmirik, xidmət keyfiyyətini zəmanət vermirik və xidmət göstərici fəaliyyətinə məsul olmayırıq.\\n\\nXidmətlə bağlı bütün problemlər, o cümlədən səhv təmirl, avtomobil zədəsi və ya qiymət mübahisələri birbaşa xidmət göstərici ilə həll edilməlidir.\\n\\nQANUNA GÖRƏ AZAMISI SAHİB OLMADIQDA:\\n• CarCat \\\"OLDUĞU KİMİ\\\" zəmanət olmadan təmin edilir\\n• Biz hər hansı bir zərərə məsul deyilik\\n• Bizim ümumi məsuliyyət 0 dollara keçməyəcəkdir"
                },
                {
                  "id": 11,
                  "title": "Mübahisələr və Həll",
                  "content": "İstifadəçilər və xidmət göstəriciləri arasındakı mübahisələr birbaşa həll edilməlidir. Aşağıdakı yollarla ünsiyyət qurmağı tövsiyə edirik:\\n\\n• Tətbiq daxilində dəstək bölməsi\\n• Xidmət göstərici ilə birbaşa əlaqə\\n• digital.innovation.agency.aze@gmail.com adına e-poçt\\n\\nBu Şərtlər Azərbaijan Respublikasının qanunları ilə idarə olunur."
                },
                {
                  "id": 12,
                  "title": "Hesab Ləğvi",
                  "content": "İstənilən zaman tətbiq parametrləri vasitəsilə və ya bizimlə əlaqə saxlayaraq hesabınızı ləğv edə bilərsiniz.\\n\\nEğer bu Şərtləri pozursanız, saxtakarlıq fəaliyyətinə cəsarət etsəniz və ya yalan məlumat təmin etsəniz, biz hesabınızı dayandıra və ya ləğv edə bilərik."
                },
                {
                  "id": 13,
                  "title": "Bu Şərtlərdə Dəyişikliklər",
                  "content": "Biz bu Şərtləri zaman-zaman yeniləyə bilərik. Dəyişikliklər edəndə sizi tətbiq vasitəsilə və ya e-poçt yolu ilə xəbərdar edəcəyik. Dəyişikliklərdən sonra istifadəyə davam etmək qəbulun göstəricidir."
                },
                {
                  "id": 14,
                  "title": "Əlaqə Məlumatı",
                  "content": "Bu Şərtlər haqqında hər hansı sual və problem yaranarsa, bizimlə əlaqə saxlayın:",
                  "contact": {
                    "companyEn": "Digital Innovation Agency LLC",
                    "companyAz": "Rəqəmsal İnnovasiyalar Agentliyi",
                    "email": "digital.innovation.agency.aze@gmail.com",
                    "website": "https://digital-innovation.agency/",
                    "location": "Bakı, Azərbaijan"
                  }
                }
              ],
              "footer": {
                "acceptanceText": "CarCat istifadə edərək, bu Şərtləri oxuduğunuzu, başa düşdüyünüzü və qəbul etdiyinizi təsdiq edirsiniz."
              }
            }
            """;

    /**
     * tr: Servisin ayakta olduğunu doğrulamak için kullanılan basit test ucu; sabit bir string döner.
     * en: Simple test endpoint used to verify the service is up; returns a fixed string.
     */
    @GetMapping("/get")
    public String get() {
        log.info("bura girdi");
        return "AAHAHHAHAHAHAHHAHAAHAHAHA";
    }
}
