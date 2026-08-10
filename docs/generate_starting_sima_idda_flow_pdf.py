"""PO-facing PDF: starting_sima_idda_flow.pdf (English, professional)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "starting_sima_idda_flow.pdf"
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
        self.cell(0, 8, "CarCat — starting_sima_idda_flow", align="R")
        self.ln(3)

    def footer(self):
        self.set_y(-14)
        self.set_font(self._f, "I", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def h1(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 16)
        self.set_text_color(15, 15, 15)
        self.multi_cell(self.epw, 8, t)
        self.ln(2)

    def h2(self, t):
        self.ln(2)
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 12)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 6, t)
        self.ln(0.5)

    def p(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, t)
        self.ln(0.4)

    def b(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, f"  •  {t}")

    def note(self, t):
        self.set_x(self.l_margin)
        self.set_fill_color(245, 245, 240)
        self.set_font(self._f, "", 9)
        self.set_text_color(60, 40, 0)
        self.multi_cell(self.epw, 5, t, fill=True)
        self.ln(1)

    def code(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 8.5)
        self.set_text_color(20, 20, 20)
        self.set_fill_color(242, 242, 242)
        self.multi_cell(self.epw, 4.2, t, fill=True)
        self.ln(1)


def build():
    pdf = Pdf()
    pdf.set_margins(14, 14, 14)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.h1("starting_sima_idda_flow")
    pdf.p(
        "Status summary of the SIMA identity verification and IDDA vehicle-list "
        "scaffold implemented in carland_service under the package test_sima_idda."
    )
    pdf.p(
        "This is an isolation / review scaffold. It is not production-ready. "
        "Persistence, credentials, and merge rules are intentionally deferred."
    )
    pdf.note(
        "Scope note: carland_auth is unchanged. Auth remains the app login and "
        "access layer only. SIMA and IDDA orchestration live in carland_service."
    )

    pdf.h2("1. Why a separate package?")
    pdf.b("Existing Customer, Car, application.yml, and environment config were not modified.")
    pdf.b("All scaffold code is contained in test_sima_idda for clear review and later move.")
    pdf.b("After sign-off, the code will be refactored into the permanent package structure.")

    pdf.h2("2. CarCat APIs exposed")
    pdf.p(
        "Clients call these endpoints. SIMA HMAC secrets never leave the backend."
    )
    pdf.code(
        "POST /api/v1/sima/verify/citizen\n"
        "POST /api/v1/sima/verify/passport\n"
        "POST /api/v1/sima/verify/foreign\n"
        "GET  /api/v1/idda/cars?fin=XXXXXXX"
    )
    pdf.b("citizen — Azerbaijan national ID card (shakhsiyyat vasiqasi).")
    pdf.b("passport — Azerbaijan travel passport.")
    pdf.b("foreign — foreign-resident documents: TRC, PRC, or ERP.")
    pdf.b("idda/cars — vehicle list by FIN; separate API (not auto-called after verify).")
    pdf.b("Caller identity: X-User-Id header (existing gateway pattern).")

    pdf.h2("3. SIMA integration behaviour")
    pdf.b("Feign clients call the official SIMA paths (aligned with AzInTelecom HTML docs).")
    pdf.code(
        ".../api/v1/kyc/identity/verify\n"
        ".../api/v1/kyc/identity/verify/passport\n"
        ".../api/v1/kyc/identity/verify/foreign"
    )
    pdf.b("Request JSON is minified (no whitespace) before signing and sending.")
    pdf.b("HMAC-SHA256 over the minified body; Base64 value goes into Signature.")
    pdf.b("Identifier and Auth-Scheme headers are set on every SIMA call.")
    pdf.b("Base URL, partner Identifier, and HMAC secret are EXAMPLE_* constants in code.")
    pdf.p(
        "Verification gate: isSuccess alone is not enough. "
        "Require isSuccess AND livenessStatus AND similarityStatus."
    )

    pdf.h2("4. Customer updates (scaffold only)")
    pdf.b("The existing Customer entity file was not edited.")
    pdf.b("Service code calls setters such as setFin, setIsVerified, setGender.")
    pdf.b("Those fields are not on Customer yet; compile may fail until the model is extended.")
    pdf.b("Existing name / surname fields are updated via setName / setSurname when present.")
    pdf.b("customerRepository.save() is not called. No verify result is persisted yet.")
    pdf.note(
        "Reason: validate API shape and flow first. "
        "Column design, migration, uniqueness rules, and save come in the next phase."
    )

    pdf.h2("5. IDDA vehicle list behaviour")
    pdf.b("No concrete IDDA path was defined in the source briefs; a placeholder Feign client is used.")
    pdf.b("Example call shape: GET /api/v1/vehicles?fin=...")
    pdf.b("Partner code and API key are EXAMPLE_* constants.")
    pdf.b("Local VINs are read from the customer's existing car list (in memory).")
    pdf.b("Incoming IDDA VINs are compared to local VINs.")
    pdf.b("Matches are written to application logs only.")
    pdf.b("No insert/update on the cars table. No merge into the garage.")

    pdf.h2("6. Why no DB merge yet?")
    pdf.b("Merge rules are not signed off (overwrite vs skip on VIN conflict).")
    pdf.b("Writing cars before the IDDA contract is confirmed risks bad production data.")
    pdf.b("Logging matches is a safe observation step with no data mutation.")
    pdf.b("Persistence will follow once IDDA response schema and product rules are fixed.")

    pdf.h2("7. Temporary items — planned refactor")
    pdf.b("Move EXAMPLE_SIMA_BASE_URL, HMAC secret, and Identifier to env / secret store.")
    pdf.b("Replace EXAMPLE_IDDA_* URL and keys with real credentials and contract.")
    pdf.b("Align IddaFeign path and response fields to the official IDDA API.")
    pdf.b("Add Customer FIN / verification columns and database migration.")
    pdf.b("Persist verify success; enforce unique verified FIN.")
    pdf.b("Implement VIN match → car-list merge per agreed product rule.")
    pdf.b("Rename / relocate test_sima_idda into the permanent package layout.")
    pdf.b("Add production error mapping, rate limits, and a feature flag.")

    pdf.h2("8. Package layout")
    pdf.code(
        "test_sima_idda/\n"
        "  controller/   SimaController, IddaController\n"
        "  service/      SimaKycService, IddaCarListService\n"
        "  feign/        SimaFeign, IddaFeign, raw-body encoder\n"
        "  dto/          request, response, sima, idda\n"
        "  hmac/         minify + sign\n"
        "  config/       EXAMPLE_* constants"
    )

    pdf.h2("9. End-to-end flow")
    pdf.code(
        "1) Client selects document channel: citizen, passport, or foreign\n"
        "2) POST /api/v1/sima/verify/...\n"
        "3) Backend minifies body, signs HMAC, calls SIMA via Feign\n"
        "4) Biometric gate passes → Customer setters invoked (no save)\n"
        "5) Client may call separately: GET /api/v1/idda/cars?fin=\n"
        "6) VIN compare → log matches (no car DB write)"
    )

    pdf.h2("10. Sharing the package with the team")
    pdf.p(
        "The package contains many source files. Prefer a single archive or a repository link "
        "rather than sending individual files."
    )
    pdf.b("Zip the test_sima_idda folder and share the archive with this PDF.")
    pdf.b("Or upload to Drive / OneDrive and share a link.")
    pdf.b("Or share a Git branch / pull request for review.")
    pdf.code(
        "Folder path:\n"
        "carland_service/src/main/java/com/carland/"
        "carland_service/test_sima_idda"
    )

    pdf.ln(2)
    pdf.set_font(pdf._f, "I", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        5,
        "Sources: AzInTelecom SIMA KYC documentation; CarCat M10 / SIMA integration brief. "
        "Regenerate: python docs/generate_starting_sima_idda_flow_pdf.py",
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
