"""SIMA_ROADMAP.pdf — backend developer rehberi (Turkce)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "SIMA_ROADMAP.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")
FONT_ITALIC = Path(r"C:\Windows\Fonts\ariali.ttf")


class Pdf(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font("T", "", str(FONT))
        self.add_font("T", "B", str(FONT_BOLD if FONT_BOLD.exists() else FONT))
        self.add_font("T", "I", str(FONT_ITALIC if FONT_ITALIC.exists() else FONT))
        self._f = "T"

    def header(self):
        self.set_font(self._f, "I", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 8, "CarCat — SIMA_ROADMAP (backend)", align="R")
        self.ln(3)

    def footer(self):
        self.set_y(-14)
        self.set_font(self._f, "I", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 10, f"Sayfa {self.page_no()}", align="C")

    def h1(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 18)
        self.set_text_color(15, 15, 15)
        self.multi_cell(self.epw, 9, t)
        self.ln(2)

    def h2(self, t):
        self.ln(3)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 13)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 7, t)
        self.ln(1)

    def h3(self, t):
        self.ln(1)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 11)
        self.set_text_color(35, 35, 35)
        self.multi_cell(self.epw, 6, t)

    def p(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, t)
        self.ln(0.5)

    def b(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, f"  •  {t}")

    def note(self, t):
        self.set_x(self.l_margin)
        self.set_fill_color(245, 245, 240)
        self.set_font(self._f, "B", 9)
        self.set_text_color(80, 50, 0)
        self.multi_cell(self.epw, 5, t, fill=True)
        self.ln(1)

    def code(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 8.5)
        self.set_text_color(20, 20, 20)
        self.set_fill_color(242, 242, 242)
        self.multi_cell(self.epw, 4.2, t.replace("\t", "    "), fill=True)
        self.ln(1)

    def step(self, n, title):
        self.ln(2)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 12)
        self.set_text_color(0, 90, 140)
        self.multi_cell(self.epw, 7, f"ADIM {n} — {title}")
        self.ln(0.5)


def build():
    pdf = Pdf()
    pdf.set_margins(14, 14, 14)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.h1("SIMA_ROADMAP")
    pdf.p(
        "Backend developer icin uygulama rehberi. "
        "Bu dokuman: ne yapacaksin, hangi sirayla, hangi API'leri cagiracaksin / yazacaksin."
    )
    pdf.p(
        "Kaynaklar: CarCat × M10 Integration Brief (Desktop HTML); "
        "AzInTelecom SIMA KYC Documentation (17.03.2026)."
    )
    pdf.note(
        "ONEMLI: v1'de tek SIMA endpoint kullanilacak: Verify Citizen Identity. "
        "Global Identification / passport / foreign v1 kapsaminda DEGIL."
    )

    # --- 1 ---
    pdf.h2("1. Ne yapıyoruz? (1 cümle)")
    pdf.p(
        "CarCat kullanicisi uygulamada FIN + vesika no (veya dogum tarihi) + canli selfie verir; "
        "backend bunu SIMA'ya gonderir; SIMA yuzu devlet kaydiyla eslestirirse hesabina "
        "dogrulanmis FIN baglanir. Selfie saklanmaz — sadece sonuc metadata saklanir."
    )
    pdf.p("FIN = SIMA body'deki pin alani (7 karakterlik sahsi kimlik numarasi).")

    # --- 2 ---
    pdf.h2("2. SIMA ile IDDA'yi karistirma")
    pdf.b("SIMA = yazma tarafi. Gercek kullanici + kamera. Kimlik dogrulama. BU ROADMAP.")
    pdf.b(
        "IDDA = okuma tarafi. M10 bilinmeyen FIN gonderince arac listesi cozumleme. "
        "SIMA bittikten SONRA ayri is. Burada API yazma."
    )
    pdf.p(
        "M10 Path A (tanimli FIN → history) SIMA dogrulamasina bagli. "
        "M10 Path B (IDDA) bu PDF'in sonunda kisaca not; v1 bloklamaz."
    )

    # --- 3 ---
    pdf.h2("3. Sirayla ne yapacaksin")
    pdf.p(
        "Asagidaki adimlari sirayla uygula. Bir onceki bitmeden sonrakine gecme "
        "(ozellikle credentials ve legal)."
    )

    # ADIM 0
    pdf.step(0, "Baslamadan once alman gerekenler")
    pdf.b("SIMA Identifier (partner id)")
    pdf.b("SIMA HMAC secret key")
    pdf.b("Staging base URL (sonra prod URL)")
    pdf.b("Threshold: liveness 0.9, similarity 0.9 (SIMA tarafinda partner config)")
    pdf.b("Legal/Security: Frankfurt'ta FIN metadata OK; selfie saklanmayacak")
    pdf.b("Product kurali: 1 FIN ↔ 1 hesap (default: baska hesapta varsa BLOK)")
    pdf.p(
        "Bunlar yoksa kod yazma. Env ornekleri: SIMA_BASE_URL, SIMA_IDENTIFIER, "
        "SIMA_HMAC_KEY, SIMA_AUTH_SCHEME=HMACSHA256"
    )

    # ADIM 1
    pdf.step(1, "Veritabanina alan ekle (User / Account)")
    pdf.p("Selfie / livePhoto ASLA saklanmaz. Sadece dogrulama sonucu:")
    pdf.b("fin (string) — dogrulanan FIN / pin")
    pdf.b("finVerified (boolean) — true sadece basarili gate sonrasi")
    pdf.b("finVerifiedAt (timestamp)")
    pdf.b("simaTransactionId (string)")
    pdf.b("livenessScore, similarityScore (decimal)")
    pdf.b("livenessThreshold, similarityThreshold (kullanilan esik, ornek 0.9)")
    pdf.b("documentNumber (opsiyonel metadata) — dogrulama aninda kullanilan")
    pdf.p(
        "Constraint: finVerified=true olan fin UNIQUE olmali. "
        "Ayni FIN baska dogrulanmis hesapta varsa verify API 409 / is kurali hatasi donsun."
    )
    pdf.note(
        "FIN M10 lookup icin aranabilir olmali ama PII. "
        "Security karar vermeli: column encrypt veya hash+lookup. "
        "Backend simdilik alanlari ekler; sifreleme stratejisini security imzalat."
    )

    # ADIM 2
    pdf.step(2, "SIMA HTTP client yaz (sadece server-side)")
    pdf.p(
        "Flutter HMAC key GORMEZ. Mobile selfie + FIN toplar, bizim backend'e gonderir; "
        "bizim backend SIMA'yi cagirir."
    )
    pdf.h3("Dis API (SIMA — sen cagiracaksin)")
    pdf.code("POST {SIMA_BASE_URL}/api/v1/kyc/identity/verify")
    pdf.h3("Header'lar")
    pdf.code(
        "Content-Type: application/json\n"
        "Auth-Scheme: HMACSHA256\n"
        "Signature:   <minified body'nin Base64 HMAC'i>\n"
        "Identifier:  <partner id>\n"
        "DeviceInfo:  (opsiyonel)"
    )
    pdf.h3("Body")
    pdf.code(
        '{\n'
        '  "pin": "10AAABC",\n'
        '  "documentNumber": "AA0012345",\n'
        '  "birthDate": null,\n'
        '  "livePhoto": "<base64 jpeg>",\n'
        '  "idempotencyKey": "<yeni UUID her istekte>"\n'
        "}"
    )
    pdf.b("pin = FIN (zorunlu)")
    pdf.b("livePhoto = base64 (zorunlu)")
    pdf.b("idempotencyKey = her request icin yeni UUID; body icinde, header degil")
    pdf.b(
        "documentNumber VEYA birthDate — tam biri dolu, digeri null. "
        "Ikisi birden veya ikisi bos = SIMA 713 / 714"
    )
    pdf.b(
        "Yeni nesil vesika: documentNumber AA veya AB ile baslar. "
        "Eski nesil: AZE prefix'ini SIL (ornegin AZE123 → 123 seklinde doc'a gore strip)"
    )

    pdf.h3("HMAC imzalama — HyperService ile ayni tuzak")
    pdf.p("Sirayla:")
    pdf.b("1) Body Map/DTO olustur (idempotencyKey dahil)")
    pdf.b("2) JSON'u BIR KEZ minify et (bosluk yok): ObjectMapper yazarken pretty print KAPALI")
    pdf.b("3) Ayni minified string'i hem HMAC'le hem HTTP body olarak gonder")
    pdf.b("4) HMAC-SHA256(key, minifiedBytes) → Base64 → Signature header")
    pdf.note(
        "Imzaladigin baytlar ≠ gonderdigin baytlar ise 70001 / imza hatasi alirsin. "
        "String'i bir kez uret, iki yerde kullan."
    )

    pdf.h3("Basarili sayma kurali (KRITIK)")
    pdf.p("isSuccess=true tek basina DOGRULAMA DEGIL. Sadece teknik islem bitti demek.")
    pdf.code(
        "verified = isSuccess == true\n"
        "        && result.livenessStatus == true\n"
        "        && result.similarityStatus == true"
    )
    pdf.p(
        "verified=false ama isSuccess=true olabilir (dusuk skor). "
        "Bunu kullaniciya 'dogrulama basarisiz' diye goster; 5xx yapma."
    )
    pdf.p(
        "Nadir: skor threshold ustunde olsa bile livenessStatus=false gelebilir "
        "(ek biometric check). Yine failed verification."
    )

    # ADIM 3
    pdf.step(3, "Bizim backend API'lerini yaz (mobile bunlari cagirir)")
    pdf.p(
        "Asagidaki endpoint'ler CarCat backend'inde senin yazacagin API'ler. "
        "Isimler ornek; path convention projedeki auth/API stiline uyarlanabilir."
    )

    pdf.h3("API-1: KYC dogrulama")
    pdf.code("POST /api/v1/kyc/sima/verify\nAuth: Bearer (login veya register-token JWT)")
    pdf.p("Request (mobile → backend):")
    pdf.code(
        '{\n'
        '  "fin": "10AAABC",\n'
        '  "documentNumber": "AA0012345",   // veya null\n'
        '  "birthDate": null,               // veya "1990-01-25"\n'
        '  "livePhotoBase64": "..."\n'
        "}"
    )
    pdf.p("Backend icinde sirayla yap:")
    pdf.b("1) JWT'den userId al; user yoksa 401")
    pdf.b("2) fin / livePhoto validate (bos degil, FIN format)")
    pdf.b("3) documentNumber XOR birthDate kontrolu")
    pdf.b("4) Ayni FIN baska verified hesapta var mi? Varsa 409 COLLISION — dur")
    pdf.b("5) User zaten finVerified ise: product karari (idempotent 200 veya 409)")
    pdf.b("6) UUID idempotencyKey uret")
    pdf.b("7) SIMA client ile Verify Citizen cagir")
    pdf.b("8) SIMA error → asagidaki mapping ile HTTP + code don; DB'ye verified yazma")
    pdf.b("9) verified gate true ise: fin + metadata kaydet, finVerified=true; selfie'yi AT")
    pdf.b("10) Response don (asagida)")

    pdf.p("Success response ornek:")
    pdf.code(
        '{\n'
        '  "verified": true,\n'
        '  "fin": "10AAABC",\n'
        '  "name": "ARAZ",\n'
        '  "surname": "ELIYEV",\n'
        '  "livenessScore": 0.99,\n'
        '  "similarityScore": 0.99,\n'
        '  "transactionId": "100550"\n'
        "}"
    )
    pdf.p("Fail response ornek (business):")
    pdf.code(
        '{\n'
        '  "verified": false,\n'
        '  "code": "SIMA_752",\n'
        '  "message": "Yuz eslesmedi. Tekrar deneyin."\n'
        "}"
    )

    pdf.h3("API-2: KYC status")
    pdf.code("GET /api/v1/kyc/status\nAuth: Bearer")
    pdf.code(
        '{\n'
        '  "finVerified": true,\n'
        '  "fin": "10AAABC",          // maskelenebilir: 10***BC\n'
        '  "finVerifiedAt": "2026-08-05T10:00:00Z"\n'
        "}"
    )
    pdf.p(
        "Mobile UI bu endpoint ile 'dogrulanmis / degil' gosterir. "
        "Dogrulama karari asla client-side 'SIMA success' ile verilmez — sadece bizim flag."
    )

    # ADIM 4
    pdf.step(4, "Uctan uca akis (sirayla)")
    pdf.code(
        "1. User login/register (mevcut telefon akisi bozulmaz)\n"
        "2. Flutter: FIN + vesika/dogum + selfie (foto kurallarina uy)\n"
        "3. Flutter → POST /api/v1/kyc/sima/verify  (Bearer)\n"
        "4. Backend → minify + HMAC → POST SIMA .../identity/verify\n"
        "5. Backend gate: isSuccess && livenessStatus && similarityStatus\n"
        "6. OK ise DB update; selfie discard\n"
        "7. Flutter → GET /api/v1/kyc/status  (ekran guncelle)\n"
        "8. (Sonra) M10 Path A bu finVerified FIN uzerinden history okuyacak"
    )

    # ADIM 5
    pdf.step(5, "SIMA hata kodlarini map et")
    pdf.p(
        "SIMA error.errorCode gelir. Kullaniciya Turkce/Azerice mesaj; "
        "710/750 gibi sistem hatalarini support log'a yaz, generic mesaj goster."
    )
    pdf.code(
        "722 / 7072  → kayit bulunamadi, tekrar dene\n"
        "721 / 7071  → vesikada foto yok, dogrulama yapilamaz\n"
        "751         → liveness fail → yeni selfie (isik/mesafe)\n"
        "752         → similarity fail → bir retry hakki\n"
        "7530        → birden fazla yuz → tek yuz iste\n"
        "7080-7082   → foto/base64 sorunlu → yeniden cek\n"
        "713 / 714   → documentNumber / birthDate kurali\n"
        "70002       → validation — bizim request'i kontrol et\n"
        "710         → credential/config — kullaniciya gosterme, alert\n"
        "750         → biometric servis — backoff retry + log\n"
        "70000/716   → unexpected — log + generic"
    )

    # ADIM 6
    pdf.step(6, "Ops / guvenlik (kodla birlikte)")
    pdf.b("Rate limit: verify endpoint (brute FIN + selfie abuse)")
    pdf.b("Log: pin/fin maskele; livePhoto ASLA loglama; transactionId + errorCode logla")
    pdf.b("Feature flag: sima.kyc.enabled — staging ac, prod kontrollu")
    pdf.b("Timeout / retry: 750 icin sinirli retry; 752'de otomatik spam etme")
    pdf.b("HMAC key sadece env/secret store; repo'ya koyma")

    # Flutter note short
    pdf.h2("4. Flutter tarafi (backend'in bilmesi gereken)")
    pdf.p("Sen yazmazsin ama dogrulama fail rate'i buradan gelir. Capture kurallari:")
    pdf.b("Yuz kadrajin %70–80'i; tek yuz; JPEG; 640x480 – 1080x1920; ≤1 MB")
    pdf.b("Face box genisligi > 200px; kafa ±20°; notr ifade; gozler acik")
    pdf.b("Filtre/crop yok — sadece downscale izinli")
    pdf.p("Mobile sadece bizim API'leri cagirir; SIMA URL ve key mobile'da olmaz.")

    # Later
    pdf.h2("5. Sonra (SIMA v1 bittikten sonra) — M10 / IDDA")
    pdf.p("Bu kisim ayri epic. SIMA v1'i bekletme.")
    pdf.b(
        "Path A: M10 FIN gonderir → CarCat'ta verified FIN varsa "
        "bagli arac + maintenance history don (SIMA/selfie yok)."
    )
    pdf.b(
        "Path B: FIN bilinmiyorsa → CarCat IDDA'dan sahip olunan araclar → "
        "kullanici secer → history veya 200 + maintenanceHistory: []"
    )
    pdf.p("Bos history = 404 DEGIL, 200 + bos array.")

    # Definition of done
    pdf.h2("6. Bitmis sayilir (Definition of Done)")
    pdf.b("Staging'de gercek SIMA credentials ile verify calisiyor")
    pdf.b("POST /kyc/sima/verify + GET /kyc/status canli")
    pdf.b("Gate dogru: uc bayrak birden true olmadan finVerified yazilmiyor")
    pdf.b("Selfie DB/disk/S3'te yok")
    pdf.b("FIN collision bloklaniyor")
    pdf.b("SIMA hata kodlari kullanici mesajina map")
    pdf.b("HMAC minify tek string ile imza + body ayni")

    pdf.ln(3)
    pdf.set_font(pdf._f, "I", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        5,
        "Yeniden uretmek icin: python docs/generate_sima_roadmap_pdf.py\n"
        "Cikti: docs/SIMA_ROADMAP.pdf",
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
