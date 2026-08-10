"""Generate PO-facing PDF explaining HyperServiceMapping (universalServiceId -> name_en)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
SRC = DOCS.parent / "src/main/java/com/carland/carland_service/enums/HyperServiceMapping.java"
OUT = DOCS / "hyper-service-mapping.pdf"

CODE = SRC.read_text(encoding="utf-8")


class MappingPDF(FPDF):
    def header(self):
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "Carland Service - Internal / Product documentation", align="R")
        self.ln(4)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def section_title(self, title: str):
        self.ln(3)
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 13)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 7, title)
        self.ln(1)

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
        self.set_font("Courier", "", 7.5)
        self.set_text_color(25, 25, 25)
        self.set_fill_color(245, 245, 245)
        # Core PDF fonts are latin-1; strip non-latin-1 (e.g. Turkish javadoc chars)
        safe = (
            text.replace("\t", "    ")
            .replace("\r", "")
            .replace("\u2014", "-")
            .replace("\u2013", "-")
            .replace("\u201c", '"')
            .replace("\u201d", '"')
            .replace("\u2018", "'")
            .replace("\u2019", "'")
        )
        safe = safe.encode("latin-1", errors="replace").decode("latin-1")
        self.multi_cell(self.epw, 3.8, safe, fill=True)
        self.ln(2)


def build():
    pdf = MappingPDF()
    pdf.set_margins(16, 16, 16)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "B", 18)
    pdf.set_text_color(10, 10, 10)
    pdf.multi_cell(pdf.epw, 9, "Hyper universalServiceId Mapping")
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "", 11)
    pdf.set_text_color(60, 60, 60)
    pdf.multi_cell(pdf.epw, 6, "How Carland maps partner service ids to our maintenance catalogue")
    pdf.ln(2)
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "I", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        5,
        "Audience: Product Owner / stakeholders. Source of truth in code: "
        "com.carland.carland_service.enums.HyperServiceMapping",
    )
    pdf.ln(2)

    pdf.section_title("1. Short answer")
    pdf.body(
        "Yes - we have a mapping service-side. It lives in the HyperServiceMapping enum. "
        "Hyper (and webhook ingest) sends a string field called universalServiceId on each "
        "service line. Carland does NOT convert that string into a numeric database id. "
        "Instead, it matches the string to our catalogue English name (services.name_en / "
        "Percentage.serviceNameEn)."
    )
    pdf.body(
        "Example: Hyper sends universalServiceId = \"Engine oil & filter\". "
        "That maps to the catalogue row whose name_en is also \"Engine oil & filter\" "
        "(enum constant ENGINE_OIL)."
    )

    pdf.section_title("2. Why this exists")
    pdf.bullet(
        "Partner visits arrive with Hyper's own service identifiers (universalServiceId)."
    )
    pdf.bullet(
        "Our app tracks maintenance progress per car using Percentage / services rows keyed by name_en."
    )
    pdf.bullet(
        "The mapping connects partner visit lines to the correct maintenance item so last/next "
        "km and dates can be synced for the driver."
    )

    pdf.section_title("3. How the logic works")
    pdf.sub_title("3.1 What is stored")
    pdf.body(
        "On ingest, Carland stores the Hyper universalServiceId mostly as-is on VisitServiceLine "
        "(raw partner value, with light normalization such as trimming / treating \"other\" as empty). "
        "The mapping enum is used later when syncing partner history onto Percentage rows."
    )

    pdf.sub_title("3.2 Matching rules")
    pdf.bullet(
        "Primary key in the enum = canonical DB name_en (exact English catalogue name)."
    )
    pdf.bullet(
        "Optional extraHyperIds = alternate Hyper spellings for the same catalogue row "
        "(example: Tyres vs Tires)."
    )
    pdf.bullet(
        "matches(hyperUniversalServiceId, percentageNameEn) returns true when the Hyper id "
        "belongs to that catalogue name (case-insensitive)."
    )
    pdf.bullet(
        "toNameEn(hyperUniversalServiceId) resolves a Hyper id to name_en when unambiguous; "
        "returns empty if unknown or ambiguous."
    )
    pdf.bullet(
        "Unmapped values (for example \"other\") are skipped silently - no percentage update."
    )

    pdf.sub_title("3.3 Where it is used in the product flow")
    pdf.body(
        "HyperPercentageSyncService walks the car's Visit / VisitServiceLine history and, for each "
        "Percentage row, finds the latest partner line whose universalServiceId matches that row's "
        "serviceNameEn via HyperServiceMapping.matches(...). That match drives last/next service "
        "km and dates shown for the car."
    )

    pdf.section_title("4. Mapping table (current enum)")
    pdf.body("Hyper universalServiceId (and aliases) -> our services.name_en:")
    rows = [
        ("Air filter", "Air filter"),
        ("Battery", "Battery"),
        ("Brake fluid", "Brake fluid"),
        ("Brake pads", "Brake pads"),
        ("Cabin filter", "Cabin filter"),
        ("Coolant (antifreeze)", "Coolant (antifreeze)"),
        ("Engine oil & filter", "Engine oil & filter"),
        ("Fuel filter", "Fuel filter"),
        ("Gas filter", "Gas filter"),
        ("Gas injectors", "Gas injectors"),
        ("Glow plugs", "Glow plugs"),
        ("HV battery / power-electronics coolant", "HV battery / power-electronics coolant"),
        ("Inverter Coolant (antifreeze)", "Inverter Coolant (antifreeze)"),
        ("Power steering fluid", "Power steering fluid"),
        ("Reduction-gear oil", "Reduction-gear oil"),
        ("Spark plugs", "Spark plugs"),
        ("Timing belt", "Timing belt"),
        ("Transmission fluid", "Transmission fluid"),
        ("Tyres  (alias: Tires)", "Tyres"),
        ("Vaporiser service", "Vaporiser service"),
        ("Wheel alignment", "Wheel alignment"),
        ("Wheel balancing & rotation", "Wheel balancing & rotation"),
        ("Wheel balancing&rotation  (alias of balancing)", "Wheel balancing & rotation"),
    ]
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_x(pdf.l_margin)
    pdf.cell(pdf.epw * 0.55, 6, "Hyper universalServiceId / aliases", border=1)
    pdf.cell(pdf.epw * 0.45, 6, "Our name_en", border=1, ln=1)
    pdf.set_font("Helvetica", "", 8)
    for left, right in rows:
        pdf.set_x(pdf.l_margin)
        pdf.cell(pdf.epw * 0.55, 5, left, border=1)
        pdf.cell(pdf.epw * 0.45, 5, right, border=1, ln=1)

    pdf.ln(2)
    pdf.section_title("5. Worked example for PO")
    pdf.body(
        "Partner payload service line includes: universalServiceId = \"Engine oil & filter\"."
    )
    pdf.bullet("Enum entry: ENGINE_OIL(\"Engine oil & filter\")")
    pdf.bullet("Resolved catalogue name_en: Engine oil & filter")
    pdf.bullet(
        "If the car has a Percentage / service row with serviceNameEn = \"Engine oil & filter\", "
        "sync will attach this visit line's last/next mileage and dates to that maintenance item."
    )
    pdf.bullet(
        "There is no separate numeric \"convert to id\" step in this mapping layer - the bridge "
        "is the English service name used in our catalogue."
    )

    pdf.section_title("6. Important product notes")
    pdf.bullet(
        "This mapping is Hyper-oriented today. Adding another partner with different ids would "
        "require extending or generalizing this table."
    )
    pdf.bullet(
        "If Hyper sends a new universalServiceId that is not listed, it will not update any "
        "Percentage until engineering adds a mapping (or alias)."
    )
    pdf.bullet(
        "Alias support exists for spelling variants (Tyres/Tires, compact wheel-balancing spelling)."
    )

    pdf.add_page()
    pdf.section_title("7. Source code (as in repository)")
    pdf.body(
        "File: src/main/java/com/carland/carland_service/enums/HyperServiceMapping.java"
    )
    pdf.code_block(CODE)

    pdf.ln(2)
    pdf.set_font("Helvetica", "I", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        5,
        "Generated for Product Owner review from the live carland_service source. "
        "If the enum changes in git, regenerate this PDF from docs/generate_hyper_service_mapping_pdf.py.",
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
