import json
import urllib.error
import urllib.request

ADMIN = "http://127.0.0.1:8001"
LUA = """local path = kong.request.get_path()
local rest = path:match("^/server%-carland(/api/v1/feature%-flags/public/promo.*)$")
if rest then
  kong.service.request.set_path(rest)
end
"""


def call(method, url, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return exc.code, parsed


status, routes = call("GET", ADMIN + "/routes?size=1000")
existing = None
for route in routes.get("data", []):
    if route.get("name") == "public-promo-flag":
        existing = route
        break

if existing:
    route_id = existing["id"]
    print("EXISTS", route_id, existing.get("paths"))
else:
    status, created = call("POST", ADMIN + "/services/carland-service/routes", {
        "name": "public-promo-flag",
        "paths": ["~/server-carland/api/v1/feature-flags/public/promo"],
        "methods": ["GET", "OPTIONS"],
        "protocols": ["http", "https"],
        "regex_priority": 100,
        "strip_path": False,
        "path_handling": "v0",
        "request_buffering": True,
        "response_buffering": True,
        "https_redirect_status_code": 426,
        "preserve_host": False,
    })
    print("CREATE", status, created.get("id"), created.get("name") or created.get("message"))
    route_id = created.get("id")

if not route_id:
    raise SystemExit(1)

status, plugins = call("GET", ADMIN + "/routes/" + route_id + "/plugins")
names = [plugin.get("name") for plugin in plugins.get("data", [])]
print("PLUGINS", names)
if "jwt" in names:
    raise SystemExit("route has jwt")
if "pre-function" not in names:
    status, plug = call("POST", ADMIN + "/routes/" + route_id + "/plugins", {
        "name": "pre-function",
        "config": {"access": [LUA]},
    })
    print("PLUGIN", status, plug.get("id"), plug.get("name") or plug.get("message"))
