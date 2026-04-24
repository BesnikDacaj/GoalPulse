const state = {
  token: localStorage.getItem("goalpulse.token") || "",
  refreshToken: localStorage.getItem("goalpulse.refreshToken") || "",
  user: JSON.parse(localStorage.getItem("goalpulse.user") || "null"),
  matches: [],
  teams: [],
  players: [],
  leagues: [],
  standings: [],
  favorites: [],
  notifications: [],
  activeFilter: "all",
  standingsSort: "points"
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`/api/v1${path}`, { ...options, headers });
  if (response.headers.get("content-type")?.includes("text/csv")) {
    if (!response.ok) throw new Error("Export failed");
    return response.text();
  }
  const payload = await response.json().catch(() => ({ success: false, message: response.statusText }));
  if (!response.ok || payload.success === false) throw new Error(payload.message || response.statusText);
  return Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
}

function setSession(data) {
  state.token = data.token;
  state.refreshToken = data.refreshToken || "";
  state.user = data.user;
  localStorage.setItem("goalpulse.token", state.token);
  localStorage.setItem("goalpulse.refreshToken", state.refreshToken);
  localStorage.setItem("goalpulse.user", JSON.stringify(state.user));
  renderSession();
}

function renderSession() {
  $("sessionLabel").textContent = state.user ? `${state.user.username} · ${state.user.role}` : "Guest";
  $("logoutBtn").classList.toggle("hidden", !state.user);
}

function toast(message) {
  $("toast").textContent = message;
  $("toast").classList.remove("hidden");
  setTimeout(() => $("toast").classList.add("hidden"), 3200);
}

function showMessage(message) {
  $("authMessage").textContent = message;
  if (message) toast(message);
}

function escapeAttr(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll('"', "&quot;").replaceAll("'", "&#39;").replaceAll("<", "&lt;");
}

function logo(team) {
  if (team.logoUrl) {
    const src = `/api/v1/assets/crest?url=${encodeURIComponent(team.logoUrl)}`;
    return `<span class="logo image-logo"><img src="${src}" alt="${escapeAttr(team.name)} logo" loading="lazy" onerror="this.remove();this.parentElement.textContent='${escapeAttr(team.shortName)}'"></span>`;
  }
  const darkText = team.color === "#f7f7f7" || team.color === "#fdeb00";
  return `<span class="logo" style="background:${team.color};color:${darkText ? "#071016" : "#fff"}">${team.shortName}</span>`;
}

function statusLabel(match) {
  return match.status === "live" ? `<span class="pulse"></span> ${match.status}` : match.status;
}

function matchMinute(match) {
  if (match.status === "live") return "67'";
  if (match.status === "finished") return "FT";
  return new Date(match.date).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function matchCard(match) {
  const favorite = state.favorites.some(team => team.id === match.homeTeam.id || team.id === match.awayTeam.id);
  return `
    <article class="card ${match.status === "live" ? "live-card" : ""}">
      <div class="card-head">
        <span class="status">${statusLabel(match)} · ${matchMinute(match)}</span>
        <button class="ghost" onclick="favorite(${match.homeTeam.id})" title="Favorite home team">${favorite ? "★" : "☆"}</button>
      </div>
      <div class="team-line">${logo(match.homeTeam)}<strong>${match.homeTeam.name}</strong><strong>${match.homeScore}</strong></div>
      <div class="team-line">${logo(match.awayTeam)}<strong>${match.awayTeam.name}</strong><strong>${match.awayScore}</strong></div>
      <p class="meta">${match.homeTeam.league} · ${match.venue}</p>
      <button class="secondary" onclick="loadMatchDetail(${match.id})">Match Center</button>
    </article>`;
}

function groupByLeague(matches) {
  return matches.reduce((groups, match) => {
    const league = match.homeTeam.league;
    groups[league] = groups[league] || [];
    groups[league].push(match);
    return groups;
  }, {});
}

function filteredMatches() {
  if (state.activeFilter === "live") return state.matches.filter(match => match.status === "live");
  if (state.activeFilter === "finished") return state.matches.filter(match => match.status === "finished");
  if (state.activeFilter === "tomorrow") return state.matches.filter(match => match.date.startsWith("2026-04-25"));
  if (state.activeFilter === "favorites") return state.matches.filter(match => state.favorites.some(team => team.id === match.homeTeam.id || team.id === match.awayTeam.id));
  return state.matches;
}

function renderMatches() {
  const matches = filteredMatches();
  $("matchStrip").innerHTML = state.matches.slice(0, 3).map(match => `
    <button class="score-tile ${match.status === "live" ? "live" : ""}" onclick="loadMatchDetail(${match.id})">
      <span class="status">${statusLabel(match)} · ${match.homeTeam.league}</span>
      <span class="score-row"><span>${match.homeTeam.shortName}</span><strong>${match.homeScore}</strong></span>
      <span class="score-row"><span>${match.awayTeam.shortName}</span><strong>${match.awayScore}</strong></span>
    </button>`).join("");

  const grouped = groupByLeague(matches);
  $("matchesByLeague").innerHTML = Object.keys(grouped).map(league => `
    <section>
      <div class="league-title"><h3>${league}</h3><span class="meta">${grouped[league].length} matches</span></div>
      <div class="grid">${grouped[league].map(matchCard).join("")}</div>
    </section>`).join("") || emptyState("No matches for this filter.");
}

async function loadMatchDetail(id) {
  const match = await api(`/matches/${id}`);
  const eventIcon = { goal: "⚽", yellow_card: "YC", red_card: "RC", substitution: "↔", kickoff: "KO", full_time: "FT", score: "★" };
  $("matchDetail").innerHTML = `
    <article class="card">
      <div class="match-hero">
        <div>${logo(match.homeTeam)}<h3>${match.homeTeam.name}</h3></div>
        <div><div class="score">${match.homeScore}-${match.awayScore}</div><span class="status">${statusLabel(match)}</span><p>${match.venue} · ${match.referee}</p></div>
        <div>${logo(match.awayTeam)}<h3>${match.awayTeam.name}</h3></div>
      </div>
      <div class="sub-tabs"><button class="active">Summary</button><button>Timeline</button><button>Lineups</button><button>Stats</button><button>H2H</button></div>
      <h3>Timeline</h3>
      <div class="timeline">
        ${match.events.map(event => `<div class="timeline-item" data-icon="${eventIcon[event.eventType] || "•"}"><strong>${event.minute}' ${event.eventType.replace("_", " ")}</strong> · ${event.player}<p>${event.detail}</p></div>`).join("") || emptyState("No events yet.")}
      </div>
      <h3>Stats</h3>
      <div class="grid">
        <div class="notice"><strong>Possession</strong><p>${match.stats.possessionHome}% - ${100 - match.stats.possessionHome}%</p></div>
        <div class="notice"><strong>Shots</strong><p>${match.stats.shotsHome} - ${match.stats.shotsAway}</p></div>
        <div class="notice"><strong>Corners</strong><p>${match.stats.cornersHome} - ${match.stats.cornersAway}</p></div>
      </div>
      <h3>Lineups</h3>
      <div class="grid">${match.lineups.map(player => playerCard(player, true)).join("")}</div>
    </article>`;
  document.querySelector("#matchDetail").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function renderStandings() {
  state.standings = await api("/standings");
  const favoriteIds = new Set(state.favorites.map(team => team.id));
  $("standingsBody").innerHTML = state.standings.map(row => {
    const zone = row.rank <= 4 ? "top-four" : row.rank >= state.standings.length - 1 ? "relegation" : "";
    const fav = favoriteIds.has(row.team.id) ? "favorite-row" : "";
    return `
      <tr class="${fav}">
        <td class="${zone}">${row.rank}</td><td>${logo(row.team)} ${row.team.name}</td><td>${row.played}</td><td>${row.won}</td><td>${row.drawn}</td><td>${row.lost}</td>
        <td>${row.goalsFor}</td><td>${row.goalsAgainst}</td><td>${row.goalDifference}</td><td><strong>${row.points}</strong></td>
      </tr>`;
  }).join("");
}

function renderTeams() {
  $("teamGrid").innerHTML = state.teams.map(team => {
    const roster = state.players.filter(player => player.teamId === team.id);
    return `
      <article class="card">
        <div class="card-head"><h3>${logo(team)} ${team.name}</h3><button class="ghost" onclick="favorite(${team.id})">☆</button></div>
        <p>${team.league} · ${team.city} · Founded ${team.founded}</p>
        <p><strong>Stadium:</strong> ${team.stadium}<br><strong>Coach:</strong> ${team.coach}</p>
        <div class="chips"><span class="chip win">W</span><span class="chip win">W</span><span class="chip">D</span><span class="chip loss">L</span><span class="chip win">W</span></div>
        <h3>Squad</h3>
        <div class="list">${roster.map(player => playerCard(player, true)).join("")}</div>
      </article>`;
  }).join("");
}

function playerCard(player, compact = false) {
  if (!player) return "";
  return `
    <div class="notice">
      <strong>#${player.shirtNumber} ${player.name}</strong>
      <p>${player.position} · ${player.nationality} · ${player.team}</p>
      ${compact ? "" : `<div class="chips"><span class="chip">${player.position === "Forward" ? "16 goals" : "4 goals"}</span><span class="chip">${player.position === "Midfielder" ? "11 assists" : "3 assists"}</span><span class="chip win">7.6 rating</span></div>`}
    </div>`;
}

function renderPlayers() {
  if (!state.players.length) {
    $("playerGrid").innerHTML = emptyState("The online match feed does not include full squads on the free daily match endpoint. Seeded mode includes sample player profiles.");
    return;
  }
  const byPosition = ["Goalkeeper", "Defender", "Midfielder", "Forward"];
  $("playerGrid").innerHTML = byPosition.map(position => {
    const players = state.players.filter(player => player.position === position);
    if (!players.length) return "";
    return `<article class="card"><h3>${position}s</h3>${players.map(player => playerCard(player)).join("")}</article>`;
  }).join("");
}

function renderSchedule() {
  const date = $("dateFilter").value;
  const matches = state.matches.filter(match => match.date.startsWith(date));
  $("scheduleList").innerHTML = matches.map(match => `
    <div class="notice">
      <strong>${new Date(match.date).toLocaleString([], { dateStyle: "medium", timeStyle: "short" })}</strong>
      <p>${match.homeTeam.name} vs ${match.awayTeam.name} · ${match.venue} · ${match.status}</p>
    </div>`).join("") || emptyState("No fixtures on this date.");
}

async function renderPrivate() {
  try {
    [state.favorites, state.notifications] = await Promise.all([api("/favorites"), api("/notifications")]);
    $("favoriteGrid").innerHTML = state.favorites.map(team => `
      <article class="card">
        <div class="card-head"><h3>${logo(team)} ${team.name}</h3><button class="ghost" onclick="unfavorite(${team.id})">Remove</button></div>
        <p>${team.league} · ${team.stadium}</p>
        <h3>Next / Recent Matches</h3>
        <div class="list">${state.matches.filter(match => match.homeTeam.id === team.id || match.awayTeam.id === team.id).map(match => `<div class="notice">${match.homeTeam.shortName} ${match.homeScore}-${match.awayScore} ${match.awayTeam.shortName}<p>${match.status} · ${match.venue}</p></div>`).join("")}</div>
      </article>`).join("") || emptyState("Log in and favorite teams to personalize this dashboard.");
    $("notifications").innerHTML = state.notifications.map(note => `<div class="notice"><strong>${note.eventType}</strong><p>${note.message}</p></div>`).join("") || emptyState("No unread notifications.");
    if (state.user?.role === "admin") await renderAudit();
  } catch (error) {
    $("favoriteGrid").innerHTML = emptyState(error.message);
  }
}

async function renderAudit() {
  const audit = await api("/admin/audit-logs");
  $("adminAuditCount").textContent = audit.length;
  $("auditLog").innerHTML = audit.map(row => `<div class="notice"><strong>${row.action} ${row.tableName}</strong><p>${row.oldValue} to ${row.newValue} · ${row.ipAddress}</p></div>`).join("") || emptyState("No audit entries yet.");
}

function renderAdminChoices() {
  $("adminMatchSelect").innerHTML = state.matches.map(match => `<option value="${match.id}">${match.homeTeam.shortName} vs ${match.awayTeam.shortName}</option>`).join("");
  $("playerSelect").innerHTML = state.players.length
    ? state.players.map(player => `<option value="${player.id}">${player.name} (${player.team})</option>`).join("")
    : `<option value="0">Online feed player unavailable</option>`;
  $("adminMatchCount").textContent = state.matches.length;
  $("adminTeamCount").textContent = state.teams.length;
  $("adminCrud").innerHTML = `
    <div class="table-wrap compact-table">
      <table>
        <thead><tr><th>Teams</th><th>League</th><th>Stadium</th><th>Action</th></tr></thead>
        <tbody>${state.teams.map(team => `<tr><td>${logo(team)} ${team.name}</td><td>${team.league}</td><td>${team.stadium}</td><td><button class="ghost" onclick="deleteAdminEntity('teams', ${team.id})">Delete</button></td></tr>`).join("")}</tbody>
      </table>
    </div>
    <div class="table-wrap compact-table">
      <table>
        <thead><tr><th>Players</th><th>Team</th><th>Position</th><th>Action</th></tr></thead>
        <tbody>${state.players.map(player => `<tr><td>#${player.shirtNumber} ${player.name}</td><td>${player.team}</td><td>${player.position}</td><td><button class="ghost" onclick="deleteAdminEntity('players', ${player.id})">Delete</button></td></tr>`).join("") || `<tr><td colspan="4">Online feed does not include full squads.</td></tr>`}</tbody>
      </table>
    </div>`;
}

async function deleteAdminEntity(type, id) {
  if (!confirm(`Delete this ${type.slice(0, -1)}?`)) return;
  try {
    await api(`/admin/${type}/${id}`, { method: "DELETE" });
    showMessage("Admin record deleted and audited.");
    await loadAll();
  } catch (error) { showMessage(error.message); }
}

function renderLeagues() {
  $("leagueSelect").innerHTML = `<option value="all">All leagues</option>` + state.leagues.map(league => `<option value="${league.id}">${league.name}</option>`).join("");
}

function renderStats() {
  $("statLive").textContent = state.matches.filter(match => match.status === "live").length;
  $("statTeams").textContent = state.teams.length;
  $("statEvents").textContent = state.matches.reduce((sum, match) => sum + (match.status === "live" ? 3 : 1), 0);
}

function emptyState(message) {
  return `<div class="notice"><strong>Nothing here yet</strong><p>${message}</p></div>`;
}

async function loadAll() {
  [state.matches, state.teams, state.players, state.leagues] = await Promise.all([
    api("/matches"),
    api("/teams"),
    api("/players"),
    api("/leagues")
  ]);
  if (state.user) await renderPrivate();
  renderStats();
  renderMatches();
  renderTeams();
  renderPlayers();
  renderSchedule();
  renderAdminChoices();
  renderLeagues();
  await renderStandings();
}

async function favorite(teamId) {
  try {
    await api(`/favorites/teams/${teamId}`, { method: "POST" });
    await renderPrivate();
    renderMatches();
    await renderStandings();
    showMessage("Favorite saved.");
  } catch (error) { showMessage(error.message); }
}

async function unfavorite(teamId) {
  await api(`/favorites/teams/${teamId}`, { method: "DELETE" });
  await renderPrivate();
  renderMatches();
}

document.querySelectorAll(".tabs button").forEach(button => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".tabs button").forEach(tab => tab.classList.remove("active"));
    document.querySelectorAll(".view").forEach(view => view.classList.remove("active"));
    button.classList.add("active");
    $(button.dataset.view).classList.add("active");
  });
});

document.querySelectorAll("#matchFilters button").forEach(button => {
  button.addEventListener("click", () => {
    document.querySelectorAll("#matchFilters button").forEach(tab => tab.classList.remove("active"));
    button.classList.add("active");
    state.activeFilter = button.dataset.filter;
    renderMatches();
  });
});

$("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    setSession(await api("/auth/login", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(event.target))) }));
    showMessage("Logged in successfully.");
    await loadAll();
  } catch (error) { showMessage(error.message); }
});

$("registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = Object.fromEntries(new FormData(event.target));
    if (body.password.length < 8) throw new Error("Password must be at least 8 characters.");
    setSession(await api("/auth/register", { method: "POST", body: JSON.stringify(body) }));
    showMessage("Registered and logged in.");
    await loadAll();
  } catch (error) { showMessage(error.message); }
});

$("logoutBtn").addEventListener("click", () => {
  localStorage.clear();
  state.token = "";
  state.refreshToken = "";
  state.user = null;
  state.favorites = [];
  state.notifications = [];
  renderSession();
  loadAll();
  showMessage("Logged out.");
});

$("searchInput").addEventListener("input", async (event) => {
  const q = event.target.value.trim();
  if (!q) {
    $("searchResults").innerHTML = "";
    return;
  }
  const results = await api(`/search?q=${encodeURIComponent(q)}`);
  $("searchResults").innerHTML = results.map(result => {
    const item = result.item;
    const label = item.name || `${item.homeTeam?.name} vs ${item.awayTeam?.name}`;
    return `<div class="notice"><strong>${result.type}</strong><p>${label}</p></div>`;
  }).join("") || emptyState("No results found.");
});

document.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    $("searchInput").focus();
  }
});

$("dateFilter").addEventListener("change", renderSchedule);
$("themeBtn").addEventListener("click", () => document.body.classList.toggle("light"));

$("exportBtn").addEventListener("click", async () => {
  try {
    const csv = await api("/export/standings.csv");
    const blob = new Blob([csv], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "goalpulse-standings.csv";
    a.click();
  } catch (error) { showMessage(error.message); }
});

$("scoreForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = Object.fromEntries(new FormData(event.target));
    await api(`/admin/matches/${body.matchId}`, { method: "PUT", body: JSON.stringify(body) });
    showMessage("Score updated and audited.");
    await loadAll();
  } catch (error) { showMessage(error.message); }
});

$("eventForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = Object.fromEntries(new FormData(event.target));
    const matchId = $("adminMatchSelect").value;
    await api(`/admin/matches/${matchId}/events`, { method: "POST", body: JSON.stringify(body) });
    showMessage("Event added. Favorite-team notifications generated.");
    await loadAll();
    await loadMatchDetail(matchId);
  } catch (error) { showMessage(error.message); }
});

renderSession();
loadAll().catch(error => showMessage(error.message));
