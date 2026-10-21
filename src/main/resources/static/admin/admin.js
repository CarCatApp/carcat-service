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

  function parseParts(value) {
    var raw = (value || "").trim();
    var bits = raw.split(":");
    if (bits.length < 2 || bits[0] === "" || bits[1] === "") return null;
    var h = parseInt(bits[0], 10);
    var m = parseInt(bits[1], 10);
    if (isNaN(h) || h < 0 || h > 23 || isNaN(m) || m < 0 || m > 59) return null;
    return { h: h, m: m };
  }

  function fillClockSelect(select, max, selected) {
    select.innerHTML = "";
    var blank = document.createElement("option");
    blank.value = "";
    blank.textContent = "--";
    select.appendChild(blank);
    for (var i = 0; i < max; i++) {
      var opt = document.createElement("option");
      opt.value = pad(i);
      opt.textContent = pad(i);
      select.appendChild(opt);
    }
    select.value = selected == null ? "" : pad(selected);
  }

  function addClockSelects(wrap, parts, hidden) {
    if (wrap.querySelector("select")) return;
    var hour = document.createElement("select");
    hour.className = "clock24-h";
    hour.setAttribute("aria-label", "Saat");
    var colon = document.createElement("span");
    colon.className = "clock24-colon";
    colon.setAttribute("aria-hidden", "true");
    colon.textContent = ":";
    var minute = document.createElement("select");
    minute.className = "clock24-m";
    minute.setAttribute("aria-label", "Dəqiqə");
    fillClockSelect(hour, 24, parts ? parts.h : null);
    fillClockSelect(minute, 60, parts ? parts.m : null);
    wrap.appendChild(hour);
    wrap.appendChild(colon);
    wrap.appendChild(minute);
    function write() {
      if (hidden) hidden.value = hour.value && minute.value ? hour.value + ":" + minute.value : "";
    }
    hour.addEventListener("change", function () {
      if (!hour.value) minute.value = "";
      else if (!minute.value) minute.value = "00";
      write();
    });
    minute.addEventListener("change", function () {
      if (!minute.value) hour.value = "";
      else if (!hour.value) hour.value = "00";
      write();
    });
  }

  function upgradeTimeInputs() {
    document.querySelectorAll("input[type='time']").forEach(function (input) {
      var wrap = document.createElement("span");
      wrap.className = "clock24";
      wrap.setAttribute("data-clock", "");
      var hidden = document.createElement("input");
      hidden.type = "hidden";
      if (input.name) hidden.name = input.name;
      if (input.id) hidden.id = input.id;
      var formId = input.getAttribute("form");
      if (formId) hidden.setAttribute("form", formId);
      hidden.value = input.value || "";
      wrap.appendChild(hidden);
      input.replaceWith(wrap);
    });
  }

  function enhanceClocks() {
    document.querySelectorAll(".clock24[data-clock]").forEach(function (wrap) {
      var hidden = wrap.querySelector("input[type='hidden']");
      if (!hidden) return;
      var parts = parseParts(hidden.value);
      hidden.value = parts ? pad(parts.h) + ":" + pad(parts.m) : "";
      addClockSelects(wrap, parts, hidden);
    });
  }

  function mountDateTime(root) {
    if (!root || root.querySelector(".dt24-date")) return;
    var key = root.getAttribute("data-dt24") || "";
    var date = document.createElement("input");
    date.type = "date";
    date.className = "dt24-date";
    date.autocomplete = "off";
    if (key) date.id = key + "Date";
    var clock = document.createElement("span");
    clock.className = "clock24";
    var hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.className = "dt24-time";
    hidden.value = "";
    clock.appendChild(hidden);
    root.appendChild(date);
    root.appendChild(clock);
    addClockSelects(clock, null, hidden);
  }

  function upgradeDateTimes() {
    document.querySelectorAll("input[type='datetime-local']").forEach(function (input) {
      var root = document.createElement("div");
      root.className = "dt24";
      if (input.id) root.setAttribute("data-dt24", input.id);
      var match = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/.exec(input.value || "");
      input.replaceWith(root);
      mountDateTime(root);
      if (match) writeDateTime(input.id, match[1] + "T" + match[2]);
    });
    document.querySelectorAll("[data-dt24]").forEach(mountDateTime);
  }

  function readDateTime(key) {
    var root = document.querySelector('[data-dt24="' + key + '"]');
    if (!root) return "";
    var date = root.querySelector(".dt24-date");
    var hour = root.querySelector(".clock24-h");
    var minute = root.querySelector(".clock24-m");
    if (!date || !date.value || !hour || !hour.value || !minute || !minute.value) return "";
    return date.value + "T" + hour.value + ":" + minute.value;
  }

  function writeDateTime(key, value) {
    var root = document.querySelector('[data-dt24="' + key + '"]');
    if (!root) return;
    var date = root.querySelector(".dt24-date");
    var hour = root.querySelector(".clock24-h");
    var minute = root.querySelector(".clock24-m");
    var hidden = root.querySelector(".dt24-time");
    var match = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})/.exec(value || "");
    if (date) date.value = match ? match[1] : "";
    if (hour) hour.value = match ? match[2] : "";
    if (minute) minute.value = match ? match[3] : "";
    if (hidden) hidden.value = match ? match[2] + ":" + match[3] : "";
  }

  function upgradeClocks() {
    upgradeTimeInputs();
    enhanceClocks();
    upgradeDateTimes();
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
    upgradeClocks();
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
    initTimeWheels: initTimeWheels,
    upgradeClocks: upgradeClocks,
    readDateTime: readDateTime,
    writeDateTime: writeDateTime
  };
})(window);
