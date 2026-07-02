"""Generate multi-partner strategy PDF for Carland Service."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "multi-partner-webhook-strategy.pdf"


class StrategyPDF(FPDF):
    def header(self):
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "Carland Service - Multi-Partner Webhook Strategy", align="R")
        self.ln(4)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def section_title(self, title: str):
        self.ln(4)
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 13)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 7, title)
        self.ln(2)

    def sub_title(self, title: str):
        self.ln(2)
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 11)
        self.set_text_color(40, 40, 40)
        self.multi_cell(self.epw, 6, title)
        self.ln(1)

    def body(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, text)
        self.ln(1)

    def bullet(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, f"  -  {text}")

    def code_block(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Courier", "", 9)
        self.set_fill_color(245, 245, 245)
        self.multi_cell(self.epw, 5, text, fill=True)
        self.ln(2)


def build():
    pdf = StrategyPDF()
    pdf.set_margins(18, 18, 18)
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()

    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "B", 18)
    pdf.set_text_color(10, 10, 10)
    pdf.multi_cell(pdf.epw, 10, "Multi-Partner Webhook Strategy")
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "", 11)
    pdf.set_text_color(60, 60, 60)
    pdf.multi_cell(pdf.epw, 6, "Carland Service - Architecture and product advisory")
    pdf.ln(3)

    pdf.body(
        "Short answer: the foundation for multi-partner support partially exists, but the "
        "webhook and visit layers are effectively locked to Hyper today. Adding Toyota Absheron "
        "(or similar partners) tomorrow would work in a pinch, but clean and secure separation "
        "requires a few strategic changes."
    )

    pdf.section_title("1. What is already in place?")
    rows = [
        ("partners table", "Ready - id, name, source, logo, dealer"),
        ("EnumPartnerId", "Ready - HYPER(1), AVTOVAZ(2)"),
        ("Partner on visit", "Ready - serviceCenterId + serviceCenterName"),
        ("Car profile", "Ready - servicedPartnerIds[]"),
        ("Mobile API v2", "Ready - partner, serviceCenterId on each visit"),
        ("Webhook ingest", "Hyper-specific - DTOs, mapper, default partner = Hyper"),
        ("Auth", "Single WEBHOOK_SECRET - signature does not identify partner"),
        ("Visit unique key", "(car_id, hyper_record_id) - no partner scope"),
    ]
    for label, status in rows:
        pdf.set_x(pdf.l_margin)
        pdf.set_font("Helvetica", "B", 10)
        pdf.cell(48, 5, label + ":", ln=0)
        pdf.set_font("Helvetica", "", 10)
        pdf.multi_cell(pdf.epw - 48, 5, status)
    pdf.body(
        "In short: the data model's partner identity layer was designed with multi-partner in mind; "
        "the integration layer still behaves like a single partner."
    )

    pdf.section_title("2. Critical risk: recordId collision")
    pdf.body(
        "Today the idempotency key is: car + recordId. If Toyota sends recordId=19387 and Hyper "
        "also has recordId=19387 for the same car, the second record would either overwrite the first "
        "or hit a unique constraint failure. For multi-partner, this is the most urgent issue."
    )
    pdf.body("What it should be: composite unique key (car_id, partner_id, record_id).")
    pdf.body(
        "The column is named hyperRecordId - semantically it should be generalized "
        "(e.g. partnerRecordId / externalRecordId)."
    )

    pdf.section_title("3. How do we separate partners?")
    pdf.sub_title("3.1 URL path (recommended - clearest)")
    pdf.code_block(
        "POST /webhook/partner/hyper/new-service-visit\n"
        "POST /webhook/partner/toyota-absheron/new-service-visit"
    )
    pdf.bullet("Partner identity comes from the URL; do not rely on the body alone")
    pdf.bullet("Easy per-partner rate limits and IP allowlists in nginx")
    pdf.bullet("Extends the existing /webhook/partner/... path pattern")

    pdf.sub_title("3.2 Per-partner secret (recommended - security)")
    pdf.code_block("Hyper  -> WEBHOOK_SECRET_HYPER\nToyota -> WEBHOOK_SECRET_TOYOTA")
    pdf.bullet("Signature provides both authentication and partner identity")
    pdf.bullet("If one partner's secret leaks, others are unaffected")
    pdf.bullet("Webhook gateway partner registry: { slug, secret, carlandPartnerId }")

    pdf.sub_title("3.3 source / partnerId in body (not sufficient alone)")
    pdf.bullet("Can be used together with auth, but not trusted by itself")
    pdf.bullet("Real identity should come from URL or secret")
    pdf.body("Practical combination: path slug + per-partner secret + same JSON body format")

    pdf.section_title("4. JSON format: exact copy vs adapter?")
    pdf.sub_title("Scenario A - All partners accept the same spec (ideal)")
    pdf.body("The format standardized for Hyper becomes the Carland Partner Webhook Spec:")
    pdf.code_block(
        '{ "plate": "...", "vin": "...", '
        '"serviceHistory": [{ "recordId": ..., "services": [...] }] }'
    )
    pdf.bullet("Toyota sends the same shape -> one mapper, one validator")
    pdf.bullet("Lowest maintenance cost")
    pdf.bullet("Partner onboarding = secret + DB row + path")

    pdf.sub_title("Scenario B - Partners send different JSON (realistic)")
    pdf.code_block(
        "Toyota JSON -> ToyotaWebhookAdapter -> Canonical PartnerVisitPayload -> DB\n"
        "Hyper JSON  -> HyperWebhookAdapter   -> Canonical PartnerVisitPayload -> DB"
    )
    pdf.bullet("Internal model is partner-agnostic; adapters sit at the edge")
    pdf.bullet("Hyper adapter effectively exists today; naming should become generic")
    pdf.body(
        "Recommendation: Publish the spec and ask new partners to implement it; add adapters "
        "only for partners that cannot comply. Use the Hyper format as the reference implementation."
    )

    pdf.section_title("5. Percentage / universalServiceId")
    pdf.body(
        "HyperServiceMapping today maps Hyper's universalServiceId to our name_en. "
        "If Toyota uses a different catalog:"
    )
    pdf.bullet("Partner-scoped mapping table: (partner_id, external_service_id) -> service_name_en")
    pdf.bullet("Or migrate all partners to a shared catalog (cleanest long term)")
    pdf.bullet(
        "Percentage.partnerRecordId exists but which partner applied it is not stored - "
        "with multiple partners, partnerId should be stored too"
    )
    pdf.body(
        "The percentage sync rule (only refresh from the newest visit) should be partner-aware. "
        "Service mapping must be partner-scoped."
    )

    pdf.section_title("6. Mobile / history API")
    pdf.body("GET .../service-history/v2 today:")
    pdf.bullet("If cache exists -> reads from DB (all partners' visits)")
    pdf.bullet("If no cache -> pulls only from Hyper API")
    pdf.body(
        "Even if Toyota sends visits via webhook, when a customer adds a car the live pull still "
        "goes to Hyper. With multiple partners:"
    )
    pdf.bullet("Remove live pull (webhook + cache only), or")
    pdf.bullet("Add a per-partner pull adapter, or")
    pdf.bullet("Treat webhook as primary source; pull only for backfill/onboarding")

    pdf.section_title("7. Recommended roadmap")
    phases = [
        ("Phase 1", "(car_id, partner_id, record_id) unique constraint + migration", "Small, critical"),
        ("Phase 2", "Partner slug in path + per-partner webhook secret", "Medium"),
        ("Phase 3", "Rename Hyper* -> Partner*; partner from request context", "Medium"),
        ("Phase 4", "Partner-aware percentage / service mapping", "Medium"),
        ("Phase 5", "Toyota adapter (if JSON differs)", "Per partner"),
        ("Phase 6", "Clarify live history pull strategy", "Product decision"),
    ]
    for phase, work, effort in phases:
        pdf.set_x(pdf.l_margin)
        pdf.set_font("Helvetica", "B", 10)
        pdf.cell(22, 5, phase + ":", ln=0)
        pdf.set_font("Helvetica", "", 10)
        pdf.multi_cell(pdf.epw - 22, 5, f"{work}  [{effort}]")

    pdf.section_title("8. Minimum to add Toyota tomorrow")
    for item in [
        "Row in partners + EnumPartnerId.TOYOTA_ABSHERON(3) (or DB-only id)",
        "Separate webhook secret + path slug",
        "Fix DB unique constraint (partner-scoped recordId)",
        "In ingest: replace DEFAULT_PARTNER = HYPER with partner id from request context",
    ]:
        pdf.bullet(item)

    pdf.section_title("9. Conclusion")
    pdf.body("Infrastructure ~40% ready:")
    pdf.bullet("Partner table, visit serviceCenterId, mobile partner display")
    pdf.body("~60% gap:")
    pdf.bullet(
        "Webhook auth/partner separation, recordId scope, Hyper-centric code, "
        "percentage mapping, live fetch strategy"
    )
    pdf.body(
        "Infrastructure is partially ready; integration is Hyper-centric today."
    )

    pdf.section_title("10. Open product question")
    pdf.body(
        "Before implementation, clarify: Will new partners adopt the same JSON spec as Hyper, "
        "or will each send its own format? That answer determines whether adapters are required; "
        "the rest of the architecture stays the same."
    )

    pdf.ln(4)
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "I", 9)
    pdf.set_text_color(100, 100, 100)
    pdf.multi_cell(pdf.epw, 5, "Generated for the Carland Service project - multi-partner webhook advisory.")

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
