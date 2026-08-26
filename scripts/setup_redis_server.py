#!/usr/bin/env python3
"""One-shot: append REDIS_* to /root/carland.env and add redis service to compose.
Password is read from stdin (first line). Never printed."""
from pathlib import Path
import sys

ENV_PATH = Path("/root/carland.env")
COMPOSE_PATH = Path("/root/docker-compose.yml")

password = sys.stdin.readline().rstrip("\n\r")
if not password:
    raise SystemExit("missing redis password on stdin")

env_text = ENV_PATH.read_text(encoding="utf-8")
if "REDIS_PASSWORD=" not in env_text:
    ENV_PATH.write_text(
        env_text.rstrip()
        + "\n\nREDIS_HOST=redis\nREDIS_PORT=6379\nREDIS_PASSWORD="
        + password
        + "\n",
        encoding="utf-8",
    )
    print("env: redis vars added")
else:
    print("env: redis vars already present")

compose = COMPOSE_PATH.read_text(encoding="utf-8")
if "\n  redis:\n" in compose or compose.startswith("  redis:"):
    print("compose: redis already present")
    sys.exit(0)

old = """  carland-service:
    image: azizjava91/carland-service:latest
    container_name: carland-service
    restart: always
    ports:
      - "9091:9091"
    env_file:
      - /root/carland.env
    depends_on:
      - carlanddb
    networks:
      - carland-network
    volumes:
      - /root/secrets/firebase.json:/app/firebase.json
"""
new = """  redis:
    image: redis:7-alpine
    container_name: carland-redis
    restart: always
    command:
      - redis-server
      - --requirepass
      - ${REDIS_PASSWORD}
      - --maxmemory
      - 512mb
      - --maxmemory-policy
      - volatile-lru
      - --save
      - ""
      - --appendonly
      - "no"
    env_file:
      - /root/carland.env
    networks:
      - carland-network

  carland-service:
    image: azizjava91/carland-service:latest
    container_name: carland-service
    restart: always
    ports:
      - "9091:9091"
    env_file:
      - /root/carland.env
    depends_on:
      - carlanddb
      - redis
    networks:
      - carland-network
    volumes:
      - /root/secrets/firebase.json:/app/firebase.json
"""
if old not in compose:
    raise SystemExit("compose: carland-service block not found (exact match failed)")
COMPOSE_PATH.write_text(compose.replace(old, new, 1), encoding="utf-8")
print("compose: redis service added")
