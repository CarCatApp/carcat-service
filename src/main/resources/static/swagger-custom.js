/* Carland Swagger: login → Bearer authorize + refresh-token renew */
(function () {
  const STORAGE = {
    access: 'carland.swagger.accessToken',
    refresh: 'carland.swagger.refreshToken',
    expAt: 'carland.swagger.accessExpAt'
  };

  function qs(sel, root) { return (root || document).querySelector(sel); }

  function authorizeBearer(token) {
    if (!token || !window.ui || !ui.authActions) return;
    ui.authActions.authorize({
      bearerAuth: {
        name: 'bearerAuth',
        schema: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
        value: token
      }
    });
  }

  async function loadConfig() {
    try {
      const res = await fetch('/swagger-auth-config', { credentials: 'same-origin' });
      if (!res.ok) throw new Error('config ' + res.status);
      return await res.json();
    } catch (e) {
      return {
        loginUrl: '/api/v1/users/login',
        refreshUrl: '/api/v1/users/refresh',
        acceptLanguage: 'az',
        accessTtlSeconds: 900
      };
    }
  }

  async function login(cfg, phoneNumber, password) {
    const res = await fetch(cfg.loginUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': cfg.acceptLanguage || 'az'
      },
      body: JSON.stringify({ phoneNumber: phoneNumber, password: password })
    });
    const body = await res.json().catch(function () { return {}; });
    if (!res.ok) {
      throw new Error(body.message || body.error || ('Login failed: ' + res.status));
    }
    if (!body.accessToken) throw new Error('Login response has no accessToken');
    persistTokens(body.accessToken, body.refreshToken, cfg.accessTtlSeconds || 900);
    authorizeBearer(body.accessToken);
    return body;
  }

  function persistTokens(access, refresh, ttlSeconds) {
    localStorage.setItem(STORAGE.access, access);
    if (refresh) localStorage.setItem(STORAGE.refresh, refresh);
    localStorage.setItem(STORAGE.expAt, String(Date.now() + (ttlSeconds - 30) * 1000));
  }

  async function refresh(cfg) {
    const refreshToken = localStorage.getItem(STORAGE.refresh);
    if (!refreshToken) return false;
    const bearer = refreshToken.startsWith('Bearer ') ? refreshToken : ('Bearer ' + refreshToken);
    const res = await fetch(cfg.refreshUrl, {
      method: 'POST',
      headers: {
        'Authorization': bearer,
        'Accept-Language': cfg.acceptLanguage || 'az'
      }
    });
    const body = await res.json().catch(function () { return {}; });
    if (!res.ok || !body.accessToken) return false;
    persistTokens(body.accessToken, body.refreshToken || refreshToken, cfg.accessTtlSeconds || 900);
    authorizeBearer(body.accessToken);
    return true;
  }

  function ensurePanel(cfg) {
    if (qs('#carland-swagger-login')) return;
    const topbar = qs('.swagger-ui .topbar') || qs('.swagger-ui');
    if (!topbar) return;

    const panel = document.createElement('div');
    panel.id = 'carland-swagger-login';
    panel.style.cssText = 'display:flex;flex-wrap:wrap;gap:8px;align-items:center;padding:10px 16px;background:#1b1b1b;color:#fff;font-family:sans-serif;font-size:13px;';
    panel.innerHTML =
      '<strong style="margin-right:8px;">Carland Login</strong>' +
      '<input id="cl-phone" placeholder="phoneNumber" style="padding:6px 8px;border-radius:6px;border:1px solid #444;background:#111;color:#fff;min-width:160px;" />' +
      '<input id="cl-pass" type="password" placeholder="password" style="padding:6px 8px;border-radius:6px;border:1px solid #444;background:#111;color:#fff;min-width:140px;" />' +
      '<button id="cl-login" type="button" style="padding:6px 12px;border:none;border-radius:6px;background:#2563eb;color:#fff;font-weight:700;cursor:pointer;">Login & Authorize</button>' +
      '<button id="cl-refresh" type="button" style="padding:6px 12px;border:none;border-radius:6px;background:#16a34a;color:#fff;font-weight:700;cursor:pointer;">Refresh token</button>' +
      '<span id="cl-status" style="opacity:.85;"></span>';

    if (topbar.classList && topbar.classList.contains('topbar')) {
      topbar.parentNode.insertBefore(panel, topbar.nextSibling);
    } else {
      topbar.insertBefore(panel, topbar.firstChild);
    }

    const status = qs('#cl-status', panel);
    qs('#cl-login', panel).onclick = async function () {
      status.textContent = 'Logging in…';
      try {
        await login(cfg, qs('#cl-phone', panel).value.trim(), qs('#cl-pass', panel).value);
        status.textContent = 'Authorized (Bearer set)';
      } catch (e) {
        status.textContent = e.message || String(e);
      }
    };
    qs('#cl-refresh', panel).onclick = async function () {
      status.textContent = 'Refreshing…';
      const ok = await refresh(cfg);
      status.textContent = ok ? 'Access token renewed' : 'Refresh failed — login again';
    };
  }

  function installFetchInterceptor(cfg) {
    if (window.__carlandFetchWrapped) return;
    window.__carlandFetchWrapped = true;
    const originalFetch = window.fetch.bind(window);
    window.fetch = async function (input, init) {
      init = init || {};
      const headers = new Headers(init.headers || {});
      const access = localStorage.getItem(STORAGE.access);
      if (access && !headers.has('Authorization')) {
        headers.set('Authorization', 'Bearer ' + access);
      }
      init.headers = headers;

      let response = await originalFetch(input, init);
      if (response.status === 401) {
        const ok = await refresh(cfg);
        if (ok) {
          const retryHeaders = new Headers(init.headers || {});
          retryHeaders.set('Authorization', 'Bearer ' + localStorage.getItem(STORAGE.access));
          init.headers = retryHeaders;
          response = await originalFetch(input, init);
        }
      }
      return response;
    };
  }

  async function boot() {
    const cfg = await loadConfig();
    installFetchInterceptor(cfg);

    const existing = localStorage.getItem(STORAGE.access);
    if (existing && window.ui) authorizeBearer(existing);

    const timer = setInterval(async function () {
      ensurePanel(cfg);
      if (window.ui && localStorage.getItem(STORAGE.access)) {
        authorizeBearer(localStorage.getItem(STORAGE.access));
      }
      const expAt = Number(localStorage.getItem(STORAGE.expAt) || 0);
      if (expAt && Date.now() >= expAt) {
        await refresh(cfg);
      }
      if (qs('#carland-swagger-login') && window.ui) {
        /* keep trying until panel exists; leave interval for refresh */
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
