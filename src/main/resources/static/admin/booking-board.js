(function () {
  const chips = [
    { id: "pending", label: "Təsdiq gözləyən", count: "pending" },
    { id: "ready", label: "Xidmətə hazır", count: "ready" },
    { id: "completed", label: "Tamamlanmış", count: "completed" },
    { id: "rejected", label: "Rədd edilmiş", count: "rejected" },
  ];
  const state = { status: "pending", page: 1, filters: {} };
  const errBox = document.getElementById("errBox");
  const dialog = document.getElementById("dialog");

  function esc(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function showError(message) {
    if (!message) {
      errBox.hidden = true;
      errBox.textContent = "";
      return;
    }
    errBox.hidden = false;
    errBox.textContent = message;
  }

  async function api(url, options) {
    const res = await fetch(url, {
      credentials: "same-origin",
      headers: { Accept: "application/json", ...(options && options.body ? { "Content-Type": "application/json" } : {}) },
      ...options,
    });
    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (e) {
      data = { message: text };
    }
    if (!res.ok) throw new Error((data && data.message) || String(res.status));
    return data;
  }

  function query() {
    const params = new URLSearchParams();
    params.set("status", state.status);
    params.set("page", String(state.page));
    Object.entries(state.filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    return params.toString();
  }

  function dayLabel(iso) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || "");
    return match ? match[3] + "." + match[2] + "." + match[1] : iso || "";
  }

  function actions(row) {
    const bits = [];
    if (row.status === "pending") {
      bits.push('<button type="button" data-act="accept">Təsdiqlə</button>');
      bits.push('<button type="button" data-act="reject" class="danger">Rədd et</button>');
    }
    if (row.status === "confirmed" || row.status === "auto_accepted") {
      bits.push('<button type="button" data-act="complete">Tamamla</button>');
      bits.push('<button type="button" data-act="noshow" class="danger">Gəlmədi</button>');
    }
    bits.push('<button type="button" data-act="history">VIN</button>');
    bits.push('<button type="button" data-act="chat">Çat</button>');
    bits.push('<button type="button" data-act="hide" class="danger">Gizlət</button>');
    return bits.join("");
  }

  function render(body) {
    document.getElementById("chips").innerHTML = chips.map((chip) => {
      const active = chip.id === state.status ? " active" : "";
      return '<button type="button" class="board-chip' + active + '" data-status="' + chip.id + '">'
        + esc(chip.label) + " <strong>" + esc(body[chip.count] || 0) + "</strong></button>";
    }).join("");
    const items = body.items || [];
    if (!items.length) {
      document.getElementById("list").innerHTML = '<p class="muted">Bu filtrdə rezerv yoxdur.</p>';
    } else {
      let html = "";
      let lastDay = null;
      items.forEach((row) => {
        if (row.day !== lastDay) {
          if (lastDay != null) html += "</section>";
          html += '<section class="board-day"><h2>' + esc(dayLabel(row.day)) + "</h2>";
          lastDay = row.day;
        }
        const service = row.serviceLabel || row.packageName || "";
        html += '<article class="board-card" data-id="' + esc(row.bookingId) + '">'
          + '<div><span class="board-branch">' + esc(row.branchName || "—") + "</span></div>"
          + "<div><strong>" + esc(row.start || "") + (row.end ? "–" + esc(row.end) : "") + "</strong> "
          + esc(service)
          + '<div class="board-meta">'
          + esc(row.customerName || "") + " · " + esc(row.phone || "") + "<br>"
          + esc(row.carBrand || "") + " " + esc(row.carModel || "") + " · " + esc(row.plateNumber || "")
          + "<br>VIN " + esc(row.vin || "")
          + (row.cancelNote ? "<br>" + esc(row.cancelNote) : "")
          + "</div></div>"
          + '<div class="board-actions" data-id="' + esc(row.bookingId) + '" data-car="' + esc(row.carId || "")
          + '" data-vin="' + esc(row.vin || "") + '" data-mode="' + esc(row.completionMode || "") + '">'
          + actions(row) + "</div></article>";
      });
      html += "</section>";
      document.getElementById("list").innerHTML = html;
    }
    const pager = document.getElementById("pager");
    const pages = body.totalPages || 1;
    pager.innerHTML = '<button type="button" id="prev" ' + (state.page <= 1 ? "disabled" : "") + ">Əvvəl</button>"
      + "<span>" + state.page + " / " + pages + "</span>"
      + '<button type="button" id="next" ' + (state.page >= pages ? "disabled" : "") + ">Sonra</button>";
  }

  async function load() {
    showError("");
    try {
      render(await api("/admin/booking-board/data?" + query()));
    } catch (err) {
      showError(err.message || "");
    }
  }

  function closeDialog() {
    dialog.hidden = true;
    dialog.innerHTML = "";
  }

  function openSheet(html) {
    dialog.innerHTML = '<div class="sheet">' + html + "</div>";
    dialog.hidden = false;
    dialog.querySelector("[data-close]").addEventListener("click", closeDialog);
  }

  async function noteDialog(title, kind, path) {
    const notes = await api("/admin/booking-board/notes?kind=" + kind);
    const options = (notes.items || []).map((note) =>
      '<option value="' + esc(note.id) + '">' + esc(note.title) + "</option>").join("");
    openSheet("<h3>" + esc(title) + "</h3>"
      + "<label>Səbəb</label><select id=\"noteId\"><option value=\"\">Əl ilə</option>" + options + "</select>"
      + "<label>Mətn</label><textarea id=\"reason\" rows=\"3\"></textarea>"
      + '<div class="row"><button type="button" data-close>Bağla</button><button type="button" id="save" class="btn btn-primary">Saxla</button></div>');
    dialog.querySelector("#save").addEventListener("click", async () => {
      const noteId = dialog.querySelector("#noteId").value;
      const reason = dialog.querySelector("#reason").value.trim();
      try {
        await api(path, { method: "POST", body: JSON.stringify(noteId ? { noteId: Number(noteId) } : { reason }) });
        closeDialog();
        load();
      } catch (err) {
        showError(err.message || "");
      }
    });
  }

  async function completeDialog(id, mode) {
    if (mode !== "detailed") {
      openSheet("<h3>Xidməti tamamla</h3><p>Bu şöbə webhook ilə tamamlanır.</p>"
        + '<div class="row"><button type="button" data-close>Bağla</button><button type="button" id="save">Tamamla</button></div>');
      dialog.querySelector("#save").addEventListener("click", async () => {
        try {
          await api("/admin/booking-board/" + id + "/complete", { method: "POST", body: "{}" });
          closeDialog();
          load();
        } catch (err) {
          showError(err.message || "");
        }
      });
      return;
    }
    const draft = await api("/admin/booking-board/" + id + "/draft");
    const cards = (draft.cards || []).map((card, index) =>
      '<label>' + esc(card.title || card.kind) + "</label>"
      + (card.priceIncluded
        ? '<input data-card="' + index + '" data-field="price" value="0" disabled>'
        : '<input data-card="' + index + '" data-field="price" inputmode="decimal" placeholder="Qiymət, AZN">')
    ).join("");
    openSheet("<h3>Xidməti tamamla</h3>"
      + "<label>Yürüş, km</label><input id=\"mileage\" inputmode=\"numeric\" value=\"" + esc(draft.carMileage || "") + "\">"
      + cards
      + '<div class="row"><button type="button" data-close>Bağla</button><button type="button" id="save">Tamamla</button></div>');
    dialog.querySelector("#save").addEventListener("click", async () => {
      const mileage = Number(dialog.querySelector("#mileage").value);
      const lines = (draft.cards || []).map((card, index) => {
        const price = dialog.querySelector('[data-card="' + index + '"][data-field="price"]');
        return {
          kind: card.kind,
          offeredServiceId: card.offeredServiceId,
          individualServiceId: card.individualServiceId,
          title: card.title,
          price: price && price.value !== "" ? Number(price.value) : null,
        };
      });
      try {
        await api("/admin/booking-board/" + id + "/complete", {
          method: "POST",
          body: JSON.stringify({ mileage, lines }),
        });
        closeDialog();
        load();
      } catch (err) {
        showError(err.message || "");
      }
    });
  }

  async function historyDialog(id, carId, vin) {
    const params = new URLSearchParams();
    if (carId) params.set("carId", carId);
    if (vin) params.set("vin", vin);
    const body = await api("/admin/booking-board/" + id + "/history?" + params.toString());
    const rows = (body.items || []).map((item) =>
      "<p><strong>" + esc(item.date || "") + "</strong> · " + esc(item.mileage == null ? "" : item.mileage + " km")
      + "<br>" + esc((item.serviceGroups || []).join(", ")) + "</p>").join("") || "<p>Qeyd yoxdur.</p>";
    openSheet("<h3>Servis tarixçəsi</h3><div class=\"chat-log\">" + rows + "</div>"
      + '<div class="row"><button type="button" data-close>Bağla</button></div>');
  }

  async function chatDialog(id) {
    const paint = (rows) => (rows || []).map((row) =>
      '<p class="' + (row.fromRole === "branch" ? "mine" : "") + '"><strong>'
      + (row.fromRole === "branch" ? "Filial" : "Müştəri") + "</strong> · " + esc(row.createdAt)
      + "<br>" + esc(row.body) + "</p>").join("") || "<p>Mesaj yoxdur.</p>";
    const rows = await api("/admin/booking-board/" + id + "/messages");
    openSheet("<h3>Çat</h3><div class=\"chat-log\" id=\"log\">" + paint(rows) + "</div>"
      + "<label>Mesaj</label><textarea id=\"body\" rows=\"3\"></textarea>"
      + '<div class="row"><button type="button" data-close>Bağla</button><button type="button" id="save">Göndər</button></div>');
    dialog.querySelector("#save").addEventListener("click", async () => {
      try {
        await api("/admin/booking-board/" + id + "/messages", {
          method: "POST",
          body: JSON.stringify({ body: dialog.querySelector("#body").value }),
        });
        const next = await api("/admin/booking-board/" + id + "/messages");
        dialog.querySelector("#log").innerHTML = paint(next);
        dialog.querySelector("#body").value = "";
      } catch (err) {
        showError(err.message || "");
      }
    });
  }

  function confirmDialog(id) {
    openSheet("<h3>Rezervi təsdiqlə</h3><p>Təsdiqdən sonra müştəriyə bildiriş gedəcək.</p>"
      + '<label><input type="checkbox" id="c1"> Tarix və saatın doğru olduğunu yoxladım</label>'
      + '<label><input type="checkbox" id="c2"> Müştəri və avtomobil məlumatlarını təsdiqləyirəm</label>'
      + '<div class="row"><button type="button" data-close>Bağla</button><button type="button" id="save" disabled>Təsdiqi tamamla</button></div>');
    const save = dialog.querySelector("#save");
    const sync = () => {
      save.disabled = !(dialog.querySelector("#c1").checked && dialog.querySelector("#c2").checked);
    };
    dialog.querySelector("#c1").addEventListener("change", sync);
    dialog.querySelector("#c2").addEventListener("change", sync);
    save.addEventListener("click", async () => {
      try {
        await api("/admin/booking-board/" + id + "/accept", { method: "POST" });
        closeDialog();
        load();
      } catch (err) {
        showError(err.message || "");
      }
    });
  }

  document.getElementById("chips").addEventListener("click", (event) => {
    const button = event.target.closest("[data-status]");
    if (!button) return;
    state.status = button.getAttribute("data-status");
    state.page = 1;
    load();
  });

  document.getElementById("search").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.target);
    state.filters = Object.fromEntries(data.entries());
    state.page = 1;
    load();
  });

  document.getElementById("pager").addEventListener("click", (event) => {
    if (event.target.id === "prev") state.page -= 1;
    if (event.target.id === "next") state.page += 1;
    load();
  });

  document.getElementById("list").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-act]");
    if (!button) return;
    const box = button.parentElement;
    const id = box.getAttribute("data-id");
    const act = button.getAttribute("data-act");
    try {
      if (act === "accept") confirmDialog(id);
      else if (act === "reject") await noteDialog("Rezervi rədd et", "reject", "/admin/booking-board/" + id + "/reject");
      else if (act === "noshow") await noteDialog("Gəlmədi", "no_show", "/admin/booking-board/" + id + "/no-show");
      else if (act === "complete") await completeDialog(id, box.getAttribute("data-mode"));
      else if (act === "history") await historyDialog(id, box.getAttribute("data-car"), box.getAttribute("data-vin"));
      else if (act === "chat") await chatDialog(id);
      else if (act === "hide") {
        await api("/admin/booking-board/" + id + "/hide", { method: "POST" });
        load();
      }
    } catch (err) {
      showError(err.message || "");
    }
  });

  load();
})();
