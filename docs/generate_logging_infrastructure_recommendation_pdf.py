"""PO-facing PDF: logging infrastructure recommendation under current host limits."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "logging_infrastructure_recommendation.pdf"
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
        self.cell(0, 8, "CarCat — Logging infrastructure recommendation", align="R")
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

    pdf.h1("Logging infrastructure recommendation")
    pdf.p(
        "Audience: Product / operations. "
        "Purpose: agree a safe logging approach for carland_auth, carland_service, "
        "webhook, and future services — under current host limits and zero tooling budget."
    )
    pdf.p("Date context: production host already runs the application stack.")

    pdf.h2("1. Current situation")
    pdf.b("Services: carland_auth, carland_service, webhook (+ future services).")
    pdf.b("Host: DigitalOcean Basic — 1 vCPU / 2 GB RAM / 50 GB disk.")
    pdf.b("Budget for logging tooling: none (no paid managed plan, no second server).")
    pdf.b("Primary need: investigate errors and reconstruct “what happened”.")
    pdf.b("Not required yet: long-term compliance archive, full-text analytics at scale.")
    pdf.note(
        "Constraint: the same 2 GB host also runs application containers and supporting "
        "infrastructure. Any heavy logging stack competes directly with product traffic."
    )

    pdf.h2("2. What was evaluated")
    pdf.b("Elasticsearch + Logstash/Filebeat + Kibana (ELK) — strong search UX, high RAM cost.")
    pdf.b("Filebeat → Elasticsearch → Kibana without Logstash — still ES-heavy.")
    pdf.b("Grafana Loki + Promtail — lighter than ES, still non-trivial on 2 GB.")
    pdf.b("Free-tier managed log clouds — possible later; quotas and retention limits apply.")
    pdf.b("Local Docker/container logs + rotation — lowest resource cost.")

    pdf.h2("3. Recommendation (now)")
    pdf.p(
        "Do not deploy Elasticsearch / full ELK / large Loki on the current 2 GB host. "
        "Risk: memory pressure, OOM kills, downtime for auth/service/webhook."
    )
    pdf.p("Adopt a phased approach:")
    pdf.code(
        "Phase 0 (now, $0, same host)\n"
        "  • Structured application logs (JSON) to container stdout\n"
        "  • Short retention via log rotation (e.g. 3–7 days) + compression\n"
        "  • Shared fields: service, env, level, timestamp, request/trace id\n"
        "  • Never log secrets, selfie/livePhoto, raw FIN, HMAC keys\n"
        "\n"
        "Phase 1 (still $0, optional)\n"
        "  • Ship ERROR/WARN only to a free-tier cloud log product\n"
        "    (e.g. Grafana Cloud Loki free) if quota allows\n"
        "  • Keep debug/info local and short-lived\n"
        "\n"
        "Phase 2 (when budget or capacity exists)\n"
        "  • Separate small logging host, or RAM upgrade (recommended >= 4–8 GB),\n"
        "    or paid managed logging — then ELK/Loki becomes viable"
    )

    pdf.h2("4. Why not ELK on this host?")
    pdf.b("Elasticsearch alone often needs ~1–2 GB RAM — most of the machine.")
    pdf.b("Apps + DB/redis/proxy + ES on 2 GB is operationally unsafe.")
    pdf.b("50 GB disk fills quickly if retention is long and logs are verbose.")
    pdf.b("ELK remains a valid future architecture — wrong fit for Phase 0.")

    pdf.h2("5. Design principles (all phases)")
    pdf.b("One shape for all services: consistent JSON fields and levels.")
    pdf.b("Correlation: request id / trace id across auth, service, webhook.")
    pdf.b("Privacy by default: mask phone/FIN; never store biometric payloads.")
    pdf.b("Prefer stdout over ad-hoc log files (simpler ops under Docker).")
    pdf.b("Retention explicit: hot window short; archive only when capacity exists.")
    pdf.b("Volume control: sample or drop noisy INFO in production if needed.")

    pdf.h2("6. “Where can we send logs for free?”")
    pdf.p(
        "There is no unlimited free off-box archive. Options under zero budget:"
    )
    pdf.b("Same host, rotated local logs — safest for stability (recommended default).")
    pdf.b(
        "Free-tier managed logging — “aside” storage with monthly GB / retention caps; "
        "suitable for errors only, not full debug history."
    )
    pdf.b(
        "True long-term central logging requires either a second host, a RAM upgrade, "
        "or a paid plan later."
    )

    pdf.h2("7. Decision summary for Product")
    pdf.code(
        "Ask now:\n"
        "  Accept Phase 0 (local structured logs + short retention)?\n"
        "\n"
        "Optional:\n"
        "  Trial free-tier ERROR shipping (Phase 1) without installing ES locally?\n"
        "\n"
        "Later investment trigger:\n"
        "  Need multi-week search across all services → upgrade host or buy managed logging."
    )

    pdf.h2("8. Outcome if we follow this")
    pdf.b("Production stability preserved on the current 2 GB droplet.")
    pdf.b("Engineers can still investigate recent failures via rotated logs.")
    pdf.b("Clear upgrade path when budget or incident volume requires central search.")
    pdf.b("Avoids building a logging stack that competes with customer-facing services.")

    pdf.ln(3)
    pdf.set_font(pdf._f, "I", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(
        pdf.epw,
        5,
        "Internal recommendation — CarCat platform. "
        "Regenerate: python docs/generate_logging_infrastructure_recommendation_pdf.py",
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
