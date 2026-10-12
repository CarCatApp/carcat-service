#!/bin/sh
# Docker's default ndots:5 makes short external names fail inside Alpine
# (apps.lsim.az, vpic.nhtsa.dot.gov → UnknownHostException).
# Rewrite resolv.conf before Java starts. Keep the container DNS so
# service names such as carland-db still resolve.
conf=/etc/resolv.conf
if [ -f "$conf" ]; then
  ns=$(awk '/^nameserver / { print; exit }' "$conf" 2>/dev/null || true)
  tmp=$(mktemp)
  if [ -n "$ns" ]; then
    printf '%s\n' "$ns" > "$tmp"
  else
    printf '%s\n' "nameserver 127.0.0.11" > "$tmp"
  fi
  printf '%s\n' \
    "nameserver 8.8.8.8" \
    "nameserver 1.1.1.1" \
    "options ndots:1 timeout:2 attempts:2" >> "$tmp"
  if ! cp "$tmp" "$conf" 2>/dev/null; then
    echo "dns: resolv.conf is not writable" >&2
  fi
  rm -f "$tmp"
fi
exec java -jar /app/app.jar
