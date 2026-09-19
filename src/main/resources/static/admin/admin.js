(function (global) {
  "use strict";

  var MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

  function pad(n) {
    return n < 10 ? "0" + n : String(n);
  }

  function parseClock(value) {
    var raw = (value || "09:00").trim();
    var parts = raw.split(":");
    var h = parseInt(parts[0], 10);
    var m = parseInt(parts[1], 10);
    if (isNaN(h) || h < 0 || h > 23) h = 9;
    if (isNaN(m) || m < 0 || m > 59) m = 0;
    return { h: h, m: m };
  }

  function fillTimeCol(col, max, selected) {
    if (!col) return;
    col.innerHTML = "";
    for (var i = 0; i < max; i++) {
      var opt = document.createElement("button");
      opt.type = "button";
      opt.className = "time-opt" + (i === selected ? " is-on" : "");
      opt.textContent = pad(i);
      opt.dataset.val = String(i);
      col.appendChild(opt);
    }
  }

  function syncTimeWheel(root) {
    var hidden = root.querySelector("input[type='hidden']");
    var face = root.querySelector(".time-wheel-face");
    var clock = parseClock(hidden.value);
    hidden.value = pad(clock.h) + ":" + pad(clock.m);
    if (face) face.textContent = hidden.value;
    root.querySelectorAll(".time-col[data-part='h'] .time-opt").forEach(function (el) {
      el.classList.toggle("is-on", parseInt(el.dataset.val, 10) === clock.h);
    });
    root.querySelectorAll(".time-col[data-part='m'] .time-opt").forEach(function (el) {
      el.classList.toggle("is-on", parseInt(el.dataset.val, 10) === clock.m);
    });
  }

  function closeTimeWheels(except) {
    document.querySelectorAll(".time-wheel.is-open").forEach(function (el) {
      if (el === except) return;
      el.classList.remove("is-open");
      var pop = el.querySelector(".time-wheel-pop");
      var btn = el.querySelector(".time-wheel-toggle");
      if (pop) pop.hidden = true;
      if (btn) btn.setAttribute("aria-expanded", "false");
    });
  }

  function initTimeWheels() {
    document.querySelectorAll(".time-wheel[data-time]").forEach(function (root) {
      var hidden = root.querySelector("input[type='hidden']");
      if (root.getAttribute("data-default") && (!hidden.value || !hidden.value.trim())) {
        hidden.value = root.getAttribute("data-default");
      }
      var clock = parseClock(hidden.value || root.getAttribute("data-default"));
      fillTimeCol(root.querySelector(".time-col[data-part='h']"), 24, clock.h);
      fillTimeCol(root.querySelector(".time-col[data-part='m']"), 60, clock.m);
      hidden.value = pad(clock.h) + ":" + pad(clock.m);
      syncTimeWheel(root);

      var toggle = root.querySelector(".time-wheel-toggle");
      var pop = root.querySelector(".time-wheel-pop");
      toggle.addEventListener("click", function (e) {
        e.stopPropagation();
        var open = !root.classList.contains("is-open");
        closeTimeWheels(open ? root : null);
        root.classList.toggle("is-open", open);
        pop.hidden = !open;
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) {
          ["h", "m"].forEach(function (part) {
            var col = root.querySelector(".time-col[data-part='" + part + "']");
            var on = col && col.querySelector(".time-opt.is-on");
            if (col && on) col.scrollTop = Math.max(0, on.offsetTop - col.clientHeight / 2 + on.clientHeight / 2);
          });
        }
      });
      pop.addEventListener("click", function (e) { e.stopPropagation(); });
      root.querySelectorAll(".time-col").forEach(function (col) {
        col.addEventListener("click", function (e) {
          var opt = e.target.closest(".time-opt");
          if (!opt) return;
          e.stopPropagation();
          var current = parseClock(hidden.value);
          var n = parseInt(opt.dataset.val, 10);
          if (col.getAttribute("data-part") === "h") current.h = n;
          else current.m = n;
          hidden.value = pad(current.h) + ":" + pad(current.m);
          syncTimeWheel(root);
        });
      });
    });
    document.addEventListener("click", function () { closeTimeWheels(null); });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") closeTimeWheels(null);
    });
  }

  function formatAdminDate(value) {
    if (!value) return "";
    var d = value instanceof Date ? value : new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return pad(d.getDate()) + " " + MONTHS[d.getMonth()] + " " + d.getFullYear() + ", " + pad(d.getHours()) + ":" + pad(d.getMinutes());
  }

  function ensureModal() {
    var existing = document.getElementById("adminConfirmModal");
    if (existing) return existing;
    var wrap = document.createElement("div");
    wrap.id = "adminConfirmModal";
    wrap.className = "admin-modal-backdrop";
    wrap.hidden = true;
    wrap.setAttribute("aria-hidden", "true");
    wrap.innerHTML =
      '<div class="admin-modal" role="dialog" aria-modal="true" aria-labelledby="adminConfirmTitle">' +
      '<h3 id="adminConfirmTitle"></h3>' +
      '<p id="adminConfirmBody"></p>' +
      '<div class="admin-modal-actions">' +
      '<button type="button" class="btn btn-ghost" data-cancel>Cancel</button>' +
      '<button type="button" class="btn btn-danger" data-ok>Confirm</button>' +
      "</div></div>";
    document.body.appendChild(wrap);
    return wrap;
  }

  function confirmModal(title, message) {
    return new Promise(function (resolve) {
      var el = ensureModal();
      var previous = document.activeElement;
      var okBtn = el.querySelector("[data-ok]");
      var cancelBtn = el.querySelector("[data-cancel]");
      el.querySelector("#adminConfirmTitle").textContent = title || "Confirm";
      el.querySelector("#adminConfirmBody").textContent = message || "This action cannot be undone.";
      el.hidden = false;
      el.removeAttribute("aria-hidden");
      var done = false;
      function finish(ok) {
        if (done) return;
        done = true;
        el.hidden = true;
        el.setAttribute("aria-hidden", "true");
        el.removeEventListener("click", onBackdrop);
        document.removeEventListener("keydown", onKey);
        okBtn.onclick = null;
        cancelBtn.onclick = null;
        if (previous && typeof previous.focus === "function") {
          try { previous.focus(); } catch (e) { /* ignore */ }
        }
        resolve(ok);
      }
      function onBackdrop(e) {
        if (e.target === el) finish(false);
      }
      function onKey(e) {
        if (e.key === "Escape") finish(false);
      }
      cancelBtn.onclick = function () { finish(false); };
      okBtn.onclick = function (e) {
        e.preventDefault();
        e.stopPropagation();
        finish(true);
      };
      el.addEventListener("click", onBackdrop);
      document.addEventListener("keydown", onKey);
      okBtn.focus();
    });
  }

  function applyColumnVisibility(table, visible) {
    var headers = table.querySelectorAll("thead th");
    headers.forEach(function (th, i) {
      var show = visible[i] !== false;
      th.classList.toggle("col-hidden", !show);
      table.querySelectorAll("tbody tr").forEach(function (tr) {
        if (tr.classList.contains("empty-row")) {
          var cell = tr.querySelector("td");
          if (cell) cell.colSpan = visible.filter(function (v) { return v !== false; }).length || headers.length;
          return;
        }
        var td = tr.children[i];
        if (td) td.classList.toggle("col-hidden", !show);
      });
    });
  }

  function initColumnToggle(table, storageKey, labels, defaultHidden) {
    if (!table) return;
    var headers = table.querySelectorAll("thead th");
    var count = headers.length;
    var visible = [];
    try {
      var saved = JSON.parse(localStorage.getItem(storageKey) || "null");
      if (Array.isArray(saved) && saved.length === count) visible = saved;
    } catch (e) { /* ignore */ }
    if (visible.length !== count) {
      visible = [];
      for (var i = 0; i < count; i++) visible.push(!(defaultHidden && defaultHidden.indexOf(i) >= 0));
    }
    applyColumnVisibility(table, visible);

    var host = table.closest(".card");
    if (!host) return;
    var wrap = host.querySelector(".table-wrap");
    var bar = document.createElement("div");
    bar.className = "table-toolbar";
    var details = document.createElement("details");
    details.className = "col-picker";
    details.innerHTML = "<summary>Columns</summary>";
    var menu = document.createElement("div");
    menu.className = "col-picker-menu";
    labels.forEach(function (label, idx) {
      var row = document.createElement("label");
      var cb = document.createElement("input");
      cb.type = "checkbox";
      cb.checked = visible[idx] !== false;
      cb.onchange = function () {
        visible[idx] = cb.checked;
        localStorage.setItem(storageKey, JSON.stringify(visible));
        applyColumnVisibility(table, visible);
      };
      row.appendChild(cb);
      row.appendChild(document.createTextNode(" " + label));
      menu.appendChild(row);
    });
    details.appendChild(menu);
    bar.appendChild(details);
    host.insertBefore(bar, wrap);
  }

  function initExpandCells(selector) {
    var sel = selector || ".cell-clip";
    function collapse(el) {
      if (!el) return;
      el.classList.remove("is-open");
      el.setAttribute("aria-expanded", "false");
      var td = el.closest("td");
      if (td) td.classList.remove("is-open");
    }
    function collapseAll(except) {
      document.querySelectorAll(sel + ".is-open").forEach(function (el) {
        if (el !== except) collapse(el);
      });
    }
    document.addEventListener("click", function (e) {
      var cell = e.target.closest(sel);
      if (cell) {
        var opening = !cell.classList.contains("is-open");
        collapseAll(cell);
        if (opening) {
          cell.classList.add("is-open");
          cell.setAttribute("aria-expanded", "true");
          var td = cell.closest("td");
          if (td) td.classList.add("is-open");
        } else {
          collapse(cell);
        }
        return;
      }
      collapseAll(null);
    });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") collapseAll(null);
    });
  }

  global.CarlandAdmin = {
    formatAdminDate: formatAdminDate,
    confirmModal: confirmModal,
    initColumnToggle: initColumnToggle,
    initExpandCells: initExpandCells,
    initTimeWheels: initTimeWheels
  };
})(window);
