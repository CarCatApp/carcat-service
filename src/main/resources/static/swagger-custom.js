/* Carland Swagger: login → Bearer + refresh + defaults (context-path safe) */
(function () {
  const STORAGE = {
    access: 'carland.swagger.accessToken',
    refresh: 'carland.swagger.refreshToken',
    expAt: 'carland.swagger.accessExpAt'
  };

  const KONG_HEADERS = ['X-User-Id', 'phoneNumber', 'role', 'inviterId'];

  function qs(sel, root) { return (root || document).querySelector(sel); }

  /** e.g. /carland-docs when UI is under /carland-docs/swagger-ui/... */
  function appBase() {
    var path = window.location.pathname || '';
    var idx = path.indexOf('/swagger-ui');
    if (idx > 0) return path.substring(0, idx);
    idx = path.indexOf('/swagger-ui.html');
    if (idx > 0) return path.substring(0, idx);
    return '';
  }

  function authorizeBearer(token) {
    if (!token || !window.ui) return;
    try {
      if (ui.authActions && typeof ui.authActions.authorize === 'function') {
        ui.authActions.authorize({
          bearerAuth: {
            name: 'bearerAuth',
            schema: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
            value: token
          }
        });
      }
      if (typeof ui.preauthorizeApiKey === 'function') {
        ui.preauthorizeApiKey('bearerAuth', token);
      }
    } catch (e) { /* ignore */ }
  }

  async function loadConfig() {
    try {
      var res = await fetch(appBase() + '/swagger-auth-config', { credentials: 'same-origin' });
      if (!res.ok) throw new Error('config ' + res.status);
      return await res.json();
    } catch (e) {
      return {
        loginUrl: 'https://digital-innovation.agency/auth/server/api/v1/users/login',
        refreshUrl: 'https://digital-innovation.agency/auth/server/api/v1/users/refresh',
        acceptLanguage: 'az',
        accessTtlSeconds: 900
      };
    }
  }

  function persistTokens(access, refresh, ttlSeconds) {
    localStorage.setItem(STORAGE.access, access);
    if (refresh) localStorage.setItem(STORAGE.refresh, refresh);
    localStorage.setItem(STORAGE.expAt, String(Date.now() + (ttlSeconds - 30) * 1000));
  }

  async function login(cfg, phoneNumber, password) {
    var res = await fetch(cfg.loginUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': cfg.acceptLanguage || 'az'
      },
      body: JSON.stringify({ phoneNumber: phoneNumber, password: password })
    });
    var body = await res.json().catch(function () { return {}; });
    if (!res.ok) {
      throw new Error(body.message || body.error || ('Login failed: ' + res.status));
    }
    if (!body.accessToken) throw new Error('Login response has no accessToken');
    persistTokens(body.accessToken, body.refreshToken, cfg.accessTtlSeconds || 900);
    authorizeBearer(body.accessToken);
    // Swagger UI sometimes mounts auth after definition switch — re-apply shortly
    setTimeout(function () { authorizeBearer(body.accessToken); }, 300);
    setTimeout(function () { authorizeBearer(body.accessToken); }, 1200);
    return body;
  }

  async function refresh(cfg) {
    var refreshToken = localStorage.getItem(STORAGE.refresh);
    if (!refreshToken) return false;
    var bearer = refreshToken.startsWith('Bearer ') ? refreshToken : ('Bearer ' + refreshToken);
    var res = await fetch(cfg.refreshUrl, {
      method: 'POST',
      headers: {
        'Authorization': bearer,
        'Accept-Language': cfg.acceptLanguage || 'az'
      }
    });
    var body = await res.json().catch(function () { return {}; });
    if (!res.ok || !body.accessToken) return false;
    persistTokens(body.accessToken, body.refreshToken || refreshToken, cfg.accessTtlSeconds || 900);
    authorizeBearer(body.accessToken);
    return true;
  }

  function applyDefaultHeaders(headers, cfg) {
    var access = localStorage.getItem(STORAGE.access);
    if (access) {
      headers.set('Authorization', 'Bearer ' + access);
    }
    headers.set('Accept-Language', cfg.acceptLanguage || 'az');
    headers.set('X-Client-Timezone', 'Asia/Baku');

    KONG_HEADERS.forEach(function (name) {
      var value = headers.get(name);
      if (value == null || value === '' || value === 'injected-by-kong-from-jwt' || value === 'string') {
        headers.delete(name);
      }
    });
  }

  function fillTryItOutDefaults() {
    // Best-effort: fill visible Try-it-out header inputs after Expand
    document.querySelectorAll('.parameters input[type="text"], .parameters input:not([type])').forEach(function (input) {
      var row = input.closest('tr') || input.closest('.parameter');
      var label = (row && row.textContent) ? row.textContent : '';
      if (/Accept-Language/i.test(label) && (!input.value || input.value === 'string')) {
        input.value = 'az';
        input.dispatchEvent(new Event('input', { bubbles: true }));
      }
      if (/X-Client-Timezone/i.test(label) && (!input.value || input.value === 'string')) {
        input.value = 'Asia/Baku';
        input.dispatchEvent(new Event('input', { bubbles: true }));
      }
    });
  }

  function ensurePanel(cfg) {
    if (qs('#carland-swagger-login')) return;
    var topbar = qs('.swagger-ui .topbar') || qs('.swagger-ui');
    if (!topbar) return;

    var panel = document.createElement('div');
    panel.id = 'carland-swagger-login';
    panel.style.cssText = 'display:flex;flex-wrap:wrap;gap:8px;align-items:center;padding:10px 16px;background:#1b1b1b;color:#fff;font-family:sans-serif;font-size:13px;';
    panel.innerHTML =
      '<strong style="margin-right:8px;">Carland Login</strong>' +
      '<span style="opacity:.9;margin-right:8px;">Login olun veya Auth definition icinde register olun</span>' +
      '<input id="cl-phone" placeholder="phoneNumber" style="padding:6px 8px;border-radius:6px;border:1px solid #444;background:#111;color:#fff;min-width:160px;" />' +
      '<input id="cl-pass" type="password" placeholder="password" style="padding:6px 8px;border-radius:6px;border:1px solid #444;background:#111;color:#fff;min-width:140px;" />' +
      '<button id="cl-login" type="button" style="padding:6px 12px;border:none;border-radius:6px;background:#2563eb;color:#fff;font-weight:700;cursor:pointer;">Login & Authorize</button>' +
      '<button id="cl-refresh" type="button" style="padding:6px 12px;border:none;border-radius:6px;background:#16a34a;color:#fff;font-weight:700;cursor:pointer;">Refresh token</button>' +
      '<button id="cl-logout" type="button" style="padding:6px 12px;border:none;border-radius:6px;background:#525252;color:#fff;cursor:pointer;">Clear</button>' +
      '<span id="cl-status" style="opacity:.85;"></span>';

    if (topbar.classList && topbar.classList.contains('topbar')) {
      topbar.parentNode.insertBefore(panel, topbar.nextSibling);
    } else {
      topbar.insertBefore(panel, topbar.firstChild);
    }

    var status = qs('#cl-status', panel);
    qs('#cl-login', panel).onclick = async function () {
      status.textContent = 'Logging in…';
      try {
        await login(cfg, qs('#cl-phone', panel).value.trim(), qs('#cl-pass', panel).value);
        status.textContent = 'Authorized — Bearer set (~15 min). Auto-refresh on 401.';
      } catch (e) {
        status.textContent = e.message || String(e);
      }
    };
    qs('#cl-refresh', panel).onclick = async function () {
      status.textContent = 'Refreshing…';
      var ok = await refresh(cfg);
      status.textContent = ok ? 'Access token renewed' : 'Refresh failed — login again';
    };
    qs('#cl-logout', panel).onclick = function () {
      localStorage.removeItem(STORAGE.access);
      localStorage.removeItem(STORAGE.refresh);
      localStorage.removeItem(STORAGE.expAt);
      try {
        if (window.ui && ui.authActions) ui.authActions.logout(['bearerAuth']);
      } catch (e) { /* ignore */ }
      status.textContent = 'Tokens cleared';
    };
  }

  function installFetchInterceptor(cfg) {
    if (window.__carlandFetchWrapped) return;
    window.__carlandFetchWrapped = true;
    var originalFetch = window.fetch.bind(window);
    window.fetch = async function (input, init) {
      init = init || {};
      var headers = new Headers(init.headers || {});
      var url = typeof input === 'string' ? input : (input && input.url) || '';
      var isDocOrAsset = /\/v3\/api-docs|swagger-config|swagger-ui|swagger-custom|swagger-auth-config/i.test(url);

      if (!isDocOrAsset) {
        applyDefaultHeaders(headers, cfg);
      }
      init.headers = headers;

      var response = await originalFetch(input, init);
      var status = response.status;
      if ((status === 401 || status === 403) && !isDocOrAsset) {
        var isAuthCall = url.indexOf('/users/login') !== -1 || url.indexOf('/users/refresh') !== -1;
        if (!isAuthCall) {
          var ok = await refresh(cfg);
          if (ok) {
            var retryHeaders = new Headers(init.headers || {});
            applyDefaultHeaders(retryHeaders, cfg);
            init.headers = retryHeaders;
            response = await originalFetch(input, init);
          }
        }
      }
      return response;
    };
  }

  async function boot() {
    var cfg = await loadConfig();
    installFetchInterceptor(cfg);

    var existing = localStorage.getItem(STORAGE.access);
    if (existing) authorizeBearer(existing);

    var timer = setInterval(async function () {
      ensurePanel(cfg);
      fillTryItOutDefaults();
      if (window.ui && localStorage.getItem(STORAGE.access)) {
        authorizeBearer(localStorage.getItem(STORAGE.access));
      }
      var expAt = Number(localStorage.getItem(STORAGE.expAt) || 0);
      if (expAt && Date.now() >= expAt) {
        await refresh(cfg);
      }
    }, 1000);

    setTimeout(function () { ensurePanel(cfg); }, 800);
    setTimeout(function () {
      if (localStorage.getItem(STORAGE.access)) authorizeBearer(localStorage.getItem(STORAGE.access));
    }, 1500);

    window.addEventListener('beforeunload', function () { clearInterval(timer); });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
