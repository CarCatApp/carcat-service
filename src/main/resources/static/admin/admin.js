(function (global) {
  "use strict";

  var MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

  function pad(n) {
    return n < 10 ? "0" + n : String(n);
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
    initExpandCells: initExpandCells
  };
})(window);
