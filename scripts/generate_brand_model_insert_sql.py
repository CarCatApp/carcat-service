#!/usr/bin/env python3
"""Generate brand/model INSERT SQL from marka_modellllllllllll.json."""

import json
import sys
from pathlib import Path

DEFAULT_JSON = Path(r"C:\Users\Aziz\Desktop\marka_modellllllllllll.json")
OUTPUT = Path(__file__).resolve().parent / "brand_model_insert.sql"
ISNEW = "."
STATUS = "ACTIVE"


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def main() -> int:
    json_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JSON
    data = json.loads(json_path.read_text(encoding="utf-8"))

    brand_names = [entry["marka"] for entry in data]
    if len(brand_names) != len(set(brand_names)):
        duplicates = sorted({n for n in brand_names if brand_names.count(n) > 1})
        print("ERROR: duplicate brand names in JSON:", duplicates, file=sys.stderr)
        return 1

    model_count = sum(len(entry["modeller"]) for entry in data)

    lines: list[str] = [
        "-- Generated from marka_modellllllllllll.json",
        f"-- brands: {len(data)}, models: {model_count}",
        "-- Run: psql -f brand_model_insert.sql",
        "",
        "BEGIN;",
        "",
        "INSERT INTO brands (brand_name, status, isnew) VALUES",
    ]

    brand_rows = [
        f"({sql_literal(entry['marka'])}, {sql_literal(STATUS)}, {sql_literal(ISNEW)})"
        for entry in data
    ]
    lines.append(",\n".join(brand_rows) + ";")
    lines.append("")
    lines.append("INSERT INTO models (model_name, brand_id, status, isnew) VALUES")

    model_rows: list[str] = []
    for entry in data:
        brand = entry["marka"]
        brand_subquery = (
            f"(SELECT brand_id FROM brands WHERE brand_name = {sql_literal(brand)} "
            f"AND isnew = {sql_literal(ISNEW)} LIMIT 1)"
        )
        for model in entry["modeller"]:
            model_rows.append(
                f"({sql_literal(model)}, {brand_subquery}, {sql_literal(STATUS)}, {sql_literal(ISNEW)})"
            )

    lines.append(",\n".join(model_rows) + ";")
    lines.extend(
        [
            "",
            "-- verify before COMMIT",
            f"-- expected brands: {len(data)}, models: {model_count}",
            "-- SELECT COUNT(*) FROM brands WHERE isnew = '.';",
            "-- SELECT COUNT(*) FROM models WHERE isnew = '.';",
            "",
            "COMMIT;",
            "",
        ]
    )

    OUTPUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUTPUT}")
    print(f"brands: {len(data)}, models: {model_count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
