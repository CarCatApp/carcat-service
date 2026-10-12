import subprocess
import sys

SQL = r"""
WITH pkgs AS (
  SELECT p.id, p.name, p.price, p.active
  FROM branch_care_packages p
  WHERE p.branch_id = 7
),
usage AS (
  SELECT p.id,
         (SELECT count(*) FROM branch_care_package_items i WHERE i.package_id = p.id) AS items,
         (SELECT count(*) FROM ranges r WHERE r.care_package_id = p.id) AS slots,
         (SELECT count(*) FROM bookings b WHERE b.care_package_id = p.id) AS bookings
  FROM pkgs p
)
SELECT p.id, p.name, p.price, p.active, u.items, u.slots, u.bookings
FROM pkgs p
JOIN usage u ON u.id = p.id
ORDER BY u.slots, u.bookings, p.id DESC;
"""


def psql(sql):
    proc = subprocess.run(
        [
            "docker", "exec", "-i", "carland-db",
            "psql", "-U", "username", "-d", "carlanddatabase",
            "-v", "ON_ERROR_STOP=1",
            "-c", sql,
        ],
        capture_output=True,
        text=True,
    )
    sys.stdout.write(proc.stdout)
    sys.stderr.write(proc.stderr)
    if proc.returncode != 0:
        sys.exit(proc.returncode)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "delete":
        psql(
            """
            WITH ranked AS (
              SELECT p.id
              FROM branch_care_packages p
              WHERE p.branch_id = 7
                AND NOT EXISTS (SELECT 1 FROM ranges r WHERE r.care_package_id = p.id)
                AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.care_package_id = p.id)
              ORDER BY p.id DESC
              LIMIT 2
            ),
            dropped_items AS (
              DELETE FROM branch_care_package_items i
              WHERE i.package_id IN (SELECT id FROM ranked)
              RETURNING i.package_id
            )
            DELETE FROM branch_care_packages p
            WHERE p.id IN (SELECT id FROM ranked)
            RETURNING p.id, p.name;
            """
        )
        print("--- remaining ---")
    psql(SQL)


if __name__ == "__main__":
    main()
