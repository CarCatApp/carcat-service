"""Generate partner webhook security proposal PDF for PO review."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "partner-webhook-security-proposal.pdf"


class ProposalPDF(FPDF):
    def header(self):
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "Carland Service - Partner Webhook Security Proposal", align="R")
        self.ln(4)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def section(self, title: str):
        self.ln(3)
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 12)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 6, title)
        self.ln(1)

    def sub(self, title: str):
        self.ln(1)
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(40, 40, 40)
        self.multi_cell(self.epw, 5, title)
        self.ln(1)

    def p(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 5, text)
        self.ln(1)

    def bullet(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.multi_cell(self.epw, 5, f"  -  {text}")

    def code(self, text: str):
        self.set_x(self.l_margin)
        self.set_font("Courier", "", 8)
        self.set_fill_color(245, 245, 245)
        self.multi_cell(self.epw, 4, text, fill=True)
        self.ln(1)


def build():
    pdf = ProposalPDF()
    pdf.set_margins(18, 18, 18)
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()

    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "B", 17)
    pdf.multi_cell(pdf.epw, 9, "Partner Webhook Security Proposal")
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(80, 80, 80)
    pdf.multi_cell(pdf.epw, 5, "PO Brief - Carland Service - Proposed approach for alignment")
    pdf.ln(2)

    pdf.section("1. Executive summary")
    pdf.p(
        "Secure multi-partner webhooks via Carland-controlled partner onboarding, one secret "
        "key per partner, and HMAC signature over the full JSON body including a Carland-assigned "
        "partnerId. Appropriate for service history data sensitivity. Stronger than a single "
        "shared secret or IP-only identification."
    )
    pdf.p("Recommendation: Approve as standard model for Hyper and future partners.")

    pdf.section("2. Problem today")
    for t in [
        "Single WEBHOOK_SECRET - one leak affects all partners",
        "Partner identity not tied to authentication - hard to scale",
        "recordId scoped by car only - collision risk across partners",
    ]:
        pdf.bullet(t)

    pdf.section("3. Proposed model")
    pdf.code(
        "1. Carland registers partner (manual / API / future admin UI)\n"
        "2. Carland assigns partnerId + secret key\n"
        "3. Partner sends JSON with partnerId + X-Signature (HMAC-SHA256, raw body)\n"
        "4. Webhook gateway verifies signature with that partner secret\n"
        "5. Carland ingests if partner active and payload valid"
    )
    pdf.p(
        "Trust rule: partnerId in body is trusted ONLY after signature verifies with that "
        "partner's secret."
    )

    pdf.section("4. Partner onboarding")
    pdf.bullet("Now: manual DB insert or internal Postman / admin API")
    pdf.bullet("Next: Admin UI - create partner, rotate secret, activate/deactivate")
    pdf.p(
        "Each partner: partnerId, name, source/slug, active flag, secret (never re-shown in full)."
    )

    pdf.section("5. Secret key management")
    pdf.sub("Storage")
    pdf.bullet("v1 (few partners): .env - WEBHOOK_SECRET_HYPER, WEBHOOK_SECRET_TOYOTA, etc.")
    pdf.bullet("Scale: encrypted DB + admin UI - rotate without redeploy")
    pdf.sub("Rotation")
    pdf.bullet("Compromise or loss: new secret immediately; disable old after grace period")
    pdf.bullet("Optional policy: weekly or monthly rotation per agreement")
    pdf.bullet("Best practice: accept old + new secret for 7-14 days during rotation")

    pdf.section("6. Request format")
    pdf.p("Canonical JSON (single visit in serviceHistory) plus partnerId:")
    pdf.code(
        '{ "partnerId": 1, "plate": "...", "vin": "...", '
        '"serviceHistory": [{ "recordId": ..., "services": [...] }] }'
    )
    pdf.p("X-Signature = HMAC-SHA256(secret, exact raw body bytes). Compact JSON required.")
    pdf.p("Verify at webhook gateway before carland internal processing.")

    pdf.section("7. Security assessment")
    pdf.sub("Protected")
    for t in [
        "Fake visits from random attackers (401 without signature)",
        "Body tampering (signature fails)",
        "Cross-partner impact from one secret leak (isolated secrets)",
        "Partner impersonation without victim secret",
    ]:
        pdf.bullet(t)
    pdf.sub("Residual risks (accepted)")
    for t in [
        "Replay - mitigated by idempotent recordId",
        "Stolen secret - rotation + monitoring",
        "DDoS - rate limits separate from auth",
        "Malicious insider at partner - B2B trust boundary",
    ]:
        pdf.bullet(t)
    pdf.p("Conclusion: industry-standard and sufficient for service history data.")
    pdf.p("Optional v1 add-on: IP allowlist at nginx (not replacement for HMAC).")

    pdf.section("8. Comparison")
    rows = [
        ("Single shared secret", "Poor scale, low security"),
        ("IP whitelist only", "Fragile, high ops effort"),
        ("Per-partner secret + HMAC (proposed)", "Good balance - RECOMMENDED"),
        ("mTLS per partner", "Highest security, highest effort"),
    ]
    for name, note in rows:
        pdf.bullet(f"{name}: {note}")

    pdf.section("9. Decisions needed from PO")
    decisions = [
        ("Approve per-partner HMAC model", "Yes (proposed)"),
        ("partnerId in body", "Required"),
        ("Secret storage v1", ".env for 2-5 partners, then DB + admin UI"),
        ("Rotation schedule", "Optional; mandatory on compromise"),
        ("IP allowlist", "Optional per partner, not core"),
        ("Admin UI", "Later - manual/API for pilots"),
    ]
    for q, ans in decisions:
        pdf.bullet(f"{q}: {ans}")

    pdf.section("10. Implementation phases")
    phases = [
        ("Phase 1", "Partner registry + partnerId in spec"),
        ("Phase 2", "Per-partner secret + gateway verification"),
        ("Phase 3", "DB unique (car_id, partner_id, record_id)"),
        ("Phase 4", "Rotation procedure + dual-secret grace window"),
        ("Phase 5", "Admin UI for partner management"),
    ]
    for ph, desc in phases:
        pdf.bullet(f"{ph}: {desc}")

    pdf.section("11. Success criteria")
    for t in [
        "Unique partnerId and secret per active partner",
        "Invalid signature rejected (401)",
        "Invalid/inactive partnerId rejected (400/403)",
        "Secret rotation without affecting other partners",
        "One-page partner integration guide for onboarding",
    ]:
        pdf.bullet(t)

    pdf.section("12. PO sign-off summary")
    pdf.p(
        "Carland-owned partner registry; one secret per partner; partnerId in signed JSON; "
        "HMAC verification at webhook gateway. Simple for partners, scalable for Carland, "
        "adequate for service-history webhooks."
    )

    pdf.ln(3)
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "I", 9)
    pdf.set_text_color(100, 100, 100)
    pdf.multi_cell(pdf.epw, 5, "Carland Service - Partner webhook security proposal for PO review.")

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
