

const S = {
  token: localStorage.getItem('gp.t') || '',
  refreshToken: localStorage.getItem('gp.rt') || '',
  user: JSON.parse(localStorage.getItem('gp.u') || 'null'),
  matches: [], teams: [], players: [], leagues: [],
  standings: [], favorites: [], notifications: [],
  selectedMatch: null
};
const $ = id => document.getElementById(id);

/* ── Date in masthead ── */
const now = new Date();
$('mastDate').textContent = now.toLocaleDateString('en-GB', { weekday:'long', day:'numeric', month:'long', year:'numeric' }).toUpperCase();

/* ── API ── */
async function api(path, opts = {}) {
  const h = { 'Content-Type':'application/json', ...(opts.headers||{}) };
  if (S.token) h.Authorization = `Bearer ${S.token}`;
  const r = await fetch(`/api/v1${path}`, { ...opts, headers: h });
  if (r.headers.get('content-type')?.includes('text/csv')) {
    if (!r.ok) throw new Error('Export failed');
    return r.text();
  }
  const b = await r.json().catch(() => ({ success:false, message:r.statusText }));
  if (!r.ok || b.success === false) throw new Error(b.message || r.statusText);
  return Object.hasOwn(b, 'data') ? b.data : b;
}

/* ── Session ── */
function setSession(d) {
  S.token = d.token; S.refreshToken = d.refreshToken||''; S.user = d.user;
  localStorage.setItem('gp.t', S.token);
  localStorage.setItem('gp.rt', S.refreshToken);
  localStorage.setItem('gp.u', JSON.stringify(S.user));
  renderSession();
}
function renderSession() {
  $('sessionInfo').textContent = S.user ? `${S.user.username} · ${S.user.role}` : 'Guest';
  $('logoutBtn').classList.toggle('hidden', !S.user);
}

function toast(msg) { $('toast').textContent=msg; $('toast').classList.remove('hidden'); setTimeout(()=>$('toast').classList.add('hidden'),3200); }
function showMessage(msg) { $('authMessage').textContent=msg; if(msg) toast(msg); }

function esc(v) { return String(v).replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/'/g,'&#39;').replace(/</g,'&lt;'); }

function livePill(min) {
  return `<span class="live-pill"><span class="live-dot"></span>Live · ${min}'</span>`;
}

function crestEl(team) {
  if (team.logoUrl) {
    const src = `/api/v1/assets/crest?url=${encodeURIComponent(team.logoUrl)}`;
    return `<img src="${src}" alt="${esc(team.shortName)}" style="width:80%;height:80%;object-fit:contain" onerror="this.parentElement.textContent='${esc(team.shortName)}'">`;
  }
  return team.shortName;
}

function empty(msg) { return `<div class="empty-state">${msg}</div>`; }

/* ════ FRONT PAGE ════ */
function renderFront() {
  const live = S.matches.filter(m => m.status === 'live');
  const finished = S.matches.filter(m => m.status === 'finished');
  const all = [...live, ...finished, ...S.matches.filter(m => m.status === 'scheduled')];
  $('liveCount').textContent = live.length;

  const lead = all[0];
  if (!lead) { $('leadStory').innerHTML = empty('No match data available.'); return; }

  // Get events for lead match
  const leadMin = lead.status === 'live' ? 67 : (lead.status === 'finished' ? 90 : 0);

  $('leadStory').innerHTML = `
    <div class="lead-grid">
      <div>
        <div style="margin-bottom:8px"><span class="mono upper" style="font-size:11px;color:var(--muted)">${lead.homeTeam.league}</span></div>
        <h2 class="lead-headline">
          ${lead.homeTeam.name} <span style="color:var(--red);font-style:italic">${lead.homeScore > lead.awayScore ? 'lead' : lead.homeScore < lead.awayScore ? 'trail' : 'level with'}</span> ${lead.awayTeam.name}
        </h2>
        <p class="lead-dek">${lead.homeTeam.shortName} ${lead.homeScore}–${lead.awayScore} ${lead.awayTeam.shortName} at ${lead.venue}</p>
        <div class="lead-score-block">
          <div style="text-align:right">
            <div class="mono upper" style="font-size:10px;color:var(--muted);margin-bottom:4px">Home</div>
            <div class="serif" style="font-size:30px;line-height:1">${lead.homeTeam.name}</div>
          </div>
          <div style="text-align:center">
            <div class="lead-score-num">${lead.homeScore}<span style="color:var(--muted);margin:0 12px;font-style:normal">–</span>${lead.awayScore}</div>
            <div style="margin-top:8px">${lead.status === 'live' ? livePill(leadMin) : `<span class="mono upper" style="font-size:10px;color:var(--muted);font-weight:700">${lead.status}</span>`}</div>
          </div>
          <div style="text-align:left">
            <div class="mono upper" style="font-size:10px;color:var(--muted);margin-bottom:4px">Away</div>
            <div class="serif" style="font-size:30px;line-height:1">${lead.awayTeam.name}</div>
          </div>
        </div>
        <div class="lead-body">
          <p><span class="lead-dropcap">A</span> compelling fixture at ${lead.venue} with ${lead.homeTeam.name} and ${lead.awayTeam.name} producing a match full of quality and intensity across both halves.</p>
          <p>The scoreline currently reads ${lead.homeScore}–${lead.awayScore} ${lead.status === 'live' ? 'with the match still in progress' : 'at the final whistle'}. ${lead.referee ? `Referee ${lead.referee} oversees proceedings.` : ''}</p>
        </div>
      </div>
      <aside class="km-sidebar">
        <div class="mono upper" style="font-size:11px;font-weight:700;margin-bottom:4px">Key Moments</div>
        <div class="serif-body" style="font-size:13px;font-style:italic;color:var(--muted);margin-bottom:18px">Click to see full match detail</div>
        <button class="ghost-btn" onclick="openMatchPage(${lead.id})" style="margin-bottom:16px;width:100%;text-align:center">View Full Report →</button>
        <div style="margin-top:16px;padding:16px;background:var(--paper-2);border:1px solid var(--rule)">
          <div class="mono upper" style="font-size:10px;font-weight:700;color:var(--red);margin-bottom:6px">Venue</div>
          <div class="serif" style="font-size:22px;line-height:1.1;margin-bottom:4px">${lead.venue}</div>
          <div class="mono" style="font-size:11px;color:var(--muted)">${lead.referee || 'TBA'}</div>
        </div>
      </aside>
    </div>`;

  // Secondary matches
  const secondary = all.slice(1, 4);
  $('secondaryMatches').innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:20px">
      <h3 class="serif" style="font-size:28px;font-style:italic;font-weight:400">Also today</h3>
      <span class="mono upper" style="font-size:10px;color:var(--muted)">${all.length} total matches</span>
    </div>
    <div class="sec-matches">
      ${secondary.map((m, i) => `
        <article class="sec-match" onclick="openMatchPage(${m.id})" style="cursor:pointer">
          <div class="mono upper" style="font-size:10px;color:var(--muted);margin-bottom:10px">${m.homeTeam.league}</div>
          <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:8px">
            <h4 class="serif" style="font-size:22px;font-weight:400;line-height:1.05">
              ${m.homeTeam.name}<br><span style="font-style:italic;color:var(--muted)">v</span> ${m.awayTeam.name}
            </h4>
            <div style="text-align:right">
              <div class="sec-score">${m.homeScore}<span style="color:var(--muted);margin:0 4px;font-style:normal">–</span>${m.awayScore}</div>
              ${m.status === 'live' ? livePill(67) : `<span class="mono upper" style="font-size:10px;color:var(--muted);font-weight:700">${m.status}</span>`}
            </div>
          </div>
        </article>`).join('')}
    </div>`;
}

/* ════ MATCH DETAIL PAGE ════ */
async function openMatchPage(id) {
  switchView('match');
  const m = await api(`/matches/${id}`);
  S.selectedMatch = m;
  const icons = { goal:'⚽', yellow_card:'🟨', red_card:'🟥', substitution:'🔄', kickoff:'▶', full_time:'⏹' };
  const stats = [
    ['Possession', m.stats.possessionHome, 100 - m.stats.possessionHome],
    ['Shots', m.stats.shotsHome, m.stats.shotsAway],
    ['Corners', m.stats.cornersHome, m.stats.cornersAway],
  ];

  $('matchDetailContent').innerHTML = `
    <div class="kicker-bar">
      <span class="mono upper kicker-live">◆ Match Report</span>
      <button class="ghost-btn" onclick="switchView('front')">← Back</button>
    </div>
    <div class="rule-thick"></div>
    <div style="padding:32px 0 24px;text-align:center">
      <div class="mono upper" style="font-size:11px;color:var(--muted);margin-bottom:10px">
        ${m.homeTeam.league} · ${m.venue}${m.referee ? ' · ' + m.referee : ''}
      </div>
      <h2 class="serif-display" style="font-size:44px;font-style:italic;line-height:1;margin-bottom:20px;letter-spacing:-.5px">
        ${m.homeTeam.name} <span style="color:var(--red)">—</span> ${m.awayTeam.name}
      </h2>
      <div style="display:grid;grid-template-columns:1fr auto 1fr;align-items:center;gap:32px;max-width:800px;margin:0 auto">
        <div style="text-align:right">
          <div class="serif" style="font-size:34px;line-height:1">${m.homeTeam.name}</div>
          <div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">${m.homeTeam.shortName}</div>
        </div>
        <div class="match-hero-score">${m.homeScore}<span style="color:var(--muted);margin:0 16px;font-style:normal">–</span>${m.awayScore}</div>
        <div style="text-align:left">
          <div class="serif" style="font-size:34px;line-height:1">${m.awayTeam.name}</div>
          <div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">${m.awayTeam.shortName}</div>
        </div>
      </div>
      <div style="margin-top:16px">${m.status === 'live' ? livePill(67) : `<span class="mono upper" style="font-size:10px;color:var(--muted);font-weight:700">${m.status}</span>`}</div>
    </div>
    <div class="rule-double"></div>
    <div class="match-body-grid">
      <div class="match-col">
        <h3 class="serif" style="font-size:28px;font-style:italic;margin-bottom:14px">Report</h3>
        <div style="column-count:2;column-gap:24px;font:16px/1.55 var(--serif-b)">
          <p style="margin-bottom:10px"><span class="lead-dropcap" style="font-size:50px">${m.homeTeam.name[0]}</span>${m.homeTeam.name} hosted ${m.awayTeam.name} at ${m.venue} in what proved to be a compelling fixture. The home side ${m.homeScore > m.awayScore ? 'secured the three points' : m.homeScore < m.awayScore ? 'fell to defeat' : 'shared the spoils'}.</p>
          <p style="margin-bottom:10px">The scoreline of ${m.homeScore}–${m.awayScore} ${m.status === 'finished' ? 'reflects the balance of play' : 'may yet change'}. ${m.events.filter(e=>e.eventType==='goal').length} goals were recorded in the match events.</p>
        </div>
      </div>
      <div class="match-divider"></div>
      <div class="match-col">
        <div class="mono upper" style="font-size:11px;font-weight:700;margin-bottom:14px">Timeline</div>
        ${m.events.length ? m.events.map(e => `
          <div style="margin-bottom:12px">
            <div style="display:flex;gap:10px;align-items:baseline;margin-bottom:3px">
              <span class="serif" style="font-style:italic;font-size:22px;line-height:1;color:var(--red)">${e.minute}'</span>
              <span class="mono upper" style="font-size:10px;color:var(--muted);font-weight:700">${e.eventType.replace('_',' ')}</span>
            </div>
            <div class="serif-body" style="font-size:14px;font-style:italic;color:var(--ink-soft);line-height:1.35">
              <strong style="font-style:normal;font-family:var(--sans);font-weight:600">${e.player}</strong> — ${e.detail}
            </div>
          </div>`).join('') : empty('No events recorded.')}
      </div>
      <div class="match-divider"></div>
      <div class="match-col">
        <div class="mono upper" style="font-size:11px;font-weight:700;margin-bottom:14px">Match Figures</div>
        ${stats.map(([label, h, a]) => {
          const t = h + a || 1;
          return `<div class="stat-row">
            <div class="stat-row-header">
              <span class="stat-val tabular${h>a?' win':''}" style="text-align:right">${h}</span>
              <span class="mono upper" style="font-size:9px;color:var(--muted)">${label}</span>
              <span class="stat-val tabular${a>h?' win':''}">${a}</span>
            </div>
            <div class="stat-bar"><div class="stat-bar-h" style="flex:${h/t}"></div><div class="stat-bar-a" style="flex:${a/t}"></div></div>
          </div>`;
        }).join('')}
        ${m.lineups.length ? `
          <div class="mono upper" style="font-size:11px;font-weight:700;margin:20px 0 10px">Lineups</div>
          ${m.lineups.map(p => `<div style="display:flex;gap:8px;padding:4px 0;border-bottom:1px solid var(--rule);font-size:13px">
            <span class="mono" style="color:var(--muted);min-width:20px">#${p.shirtNumber}</span>
            <span>${p.name}</span>
            <span style="margin-left:auto;color:var(--muted);font-size:11px">${p.position}</span>
          </div>`).join('')}` : ''}
      </div>
    </div>`;
}

/* ════ STANDINGS ════ */
async function renderStandings() {
  S.standings = await api('/standings');
  const total = S.standings.length;
  const favIds = new Set(S.favorites.map(t => t.id));
  $('standingsTable').innerHTML = `
    <div class="std-grid std-head">
      <div>Pos</div><div>Club</div><div style="text-align:center">Pl</div><div style="text-align:center">W</div>
      <div style="text-align:center">D</div><div style="text-align:center">L</div><div style="text-align:center">GD</div><div style="text-align:right">Pts</div>
    </div>
    ${S.standings.map(r => {
      const isTop = r.rank <= 4;
      const isRel = r.rank >= total - 1;
      const zoneStyle = isTop || isRel ? `border-left:3px solid ${isRel ? 'var(--red)' : 'var(--ink)'};padding-left:12px` : '';
      return `<div class="std-grid" style="${zoneStyle}${favIds.has(r.team.id)?';background:var(--paper-2)':''}">
        <div class="std-pos tabular${isTop?' top':''}${isRel?' rel':''}">${r.rank}</div>
        <div class="std-name">${r.team.name}</div>
        <div class="std-stat tabular">${r.played}</div><div class="std-stat tabular">${r.won}</div>
        <div class="std-stat tabular">${r.drawn}</div><div class="std-stat tabular">${r.lost}</div>
        <div class="std-stat tabular" style="color:${r.goalDifference>=0?'var(--ink)':'var(--red)'}">${r.goalDifference>0?'+':''}${r.goalDifference}</div>
        <div class="std-pts tabular">${r.points}</div>
      </div>`;
    }).join('')}`;
}

/* ════ SCHEDULE ════ */
function renderSchedule() {
  const d = $('dateFilter').value;
  const list = S.matches.filter(m => m.date.startsWith(d));
  const grouped = {};
  list.forEach(m => {
    const day = new Date(m.date).toLocaleDateString('en-GB', { weekday:'short', day:'numeric' });
    (grouped[day] = grouped[day] || []).push(m);
  });
  if (!list.length) { $('scheduleContent').innerHTML = empty('No fixtures on this date.'); return; }
  $('scheduleContent').innerHTML = Object.keys(grouped).map(day => `
    <div class="sched-day-head">
      <h3>${day}</h3><div class="line"></div>
      <span class="mono upper" style="font-size:10px;color:var(--muted)">${grouped[day].length} fixtures</span>
    </div>
    <div class="sched-grid">
      ${grouped[day].map(m => {
        const t = new Date(m.date).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'});
        const live = m.status === 'live';
        return `<div class="sched-item${live?' marquee':''}" onclick="openMatchPage(${m.id})" style="cursor:pointer">
          <div class="sched-time${live?' marquee':''}">${t}</div>
          <div class="sched-teams">${m.homeTeam.name} <span class="serif-body" style="font-style:italic;color:var(--muted)">vs</span> ${m.awayTeam.name}</div>
          <div class="sched-league">${live?'<span style="color:var(--red);margin-right:6px">◆</span>':''}${m.homeTeam.league}</div>
        </div>`;
      }).join('')}
    </div>`).join('');
}

/* ════ CLUBS ════ */
function renderClubs() {
  $('clubContent').innerHTML = S.teams.map(t => {
    const roster = S.players.filter(p => p.teamId === t.id);
    const matches = S.matches.filter(m => m.homeTeam.id === t.id || m.awayTeam.id === t.id);
    const wins = matches.filter(m => m.status==='finished' && ((m.homeTeam.id===t.id && m.homeScore>m.awayScore) || (m.awayTeam.id===t.id && m.awayScore>m.homeScore))).length;
    return `
    <div class="club-card">
      <div style="text-align:center">
        <div class="club-crest">${crestEl(t)}</div>
        <div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:12px">Est. ${t.founded}</div>
        <button class="ghost-btn" style="margin-top:8px" onclick="toggleFav(${t.id})">☆ Favorite</button>
      </div>
      <div>
        <div class="mono upper" style="font-size:11px;color:var(--muted);margin-bottom:8px">${t.league} · ${t.city}</div>
        <h2 class="serif-display" style="font-size:48px;font-style:italic;line-height:.95;letter-spacing:-1px;margin-bottom:12px">${t.name}</h2>
        <p class="serif-body" style="font-size:17px;font-style:italic;color:var(--ink-soft);line-height:1.4;margin-bottom:16px">
          ${t.coach}'s side, playing out of ${t.stadium}.
        </p>
        <div class="club-stats-row">
          <div class="club-stat"><div class="club-stat-val tabular">${matches.length}</div><div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">Matches</div></div>
          <div class="club-stat"><div class="club-stat-val tabular accent">${wins}</div><div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">Wins</div></div>
          <div class="club-stat"><div class="club-stat-val tabular">${roster.length}</div><div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">Squad</div></div>
          <div class="club-stat"><div class="club-stat-val tabular">${t.stadium}</div><div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:6px">Ground</div></div>
        </div>
        ${roster.length ? `
          <div style="margin-top:20px">
            <div class="squad-row squad-head"><div>Sq</div><div>Pos</div><div>Player</div><div style="text-align:center">Nat</div></div>
            ${roster.map(p => `<div class="squad-row">
              <div class="serif tabular" style="font-style:italic;font-size:20px;color:var(--muted)">${p.shirtNumber}</div>
              <div class="mono upper" style="font-size:11px;font-weight:700;color:var(--ink-soft)">${p.position.substring(0,3).toUpperCase()}</div>
              <div class="serif" style="font-size:18px">${p.name}</div>
              <div class="mono" style="text-align:center;font-size:12px;color:var(--muted)">${p.nationality}</div>
            </div>`).join('')}
          </div>` : ''}
      </div>
    </div>`;
  }).join('');
}

/* ════ FAVORITES ════ */
async function renderPrivate() {
  try {
    [S.favorites, S.notifications] = await Promise.all([api('/favorites'), api('/notifications')]);
    $('favContent').innerHTML = S.favorites.length ? S.favorites.map(t => `
      <div class="fav-card">
        <div style="display:flex;justify-content:space-between;align-items:baseline">
          <h3 class="serif" style="font-size:24px;font-style:italic">${t.name}</h3>
          <button class="ghost-btn" onclick="unfav(${t.id})">Remove</button>
        </div>
        <div class="mono upper" style="font-size:10px;color:var(--muted);margin-top:4px">${t.league} · ${t.stadium}</div>
        ${S.matches.filter(m => m.homeTeam.id===t.id||m.awayTeam.id===t.id).map(m => `
          <div style="display:flex;gap:12px;padding:6px 0;font:14px var(--serif-b)">
            <span class="mono" style="font-size:11px;color:var(--muted);min-width:60px">${m.status}</span>
            <span>${m.homeTeam.shortName} ${m.homeScore}–${m.awayScore} ${m.awayTeam.shortName}</span>
          </div>`).join('')}
      </div>`).join('') : empty('Log in and mark teams as favorites.');
    $('notifContent').innerHTML = S.notifications.length ? S.notifications.map(n => `
      <div class="notif-item"><strong>${n.eventType}</strong><p>${n.message}</p></div>`).join('') : empty('No notifications.');
    if (S.user?.role === 'admin') await renderAudit();
  } catch(e) { $('favContent').innerHTML = empty(e.message); }
}

/* ════ ADMIN ════ */
async function renderAudit() {
  const a = await api('/admin/audit-logs');
  $('ctrAudit').textContent = a.length;
  $('auditLog').innerHTML = a.length ? a.map(r => `
    <div class="notif-item"><strong>${r.action} ${r.tableName}</strong><p>${r.oldValue} → ${r.newValue} · ${r.ipAddress}</p></div>`).join('') : empty('No audit entries.');
}

function renderAdminChoices() {
  $('adminMatchSelect').innerHTML = S.matches.map(m => `<option value="${m.id}">${m.homeTeam.shortName} vs ${m.awayTeam.shortName}</option>`).join('');
  $('playerSelect').innerHTML = S.players.length
    ? S.players.map(p => `<option value="${p.id}">${p.name} (${p.team})</option>`).join('')
    : '<option value="0">No players</option>';
  $('ctrMatch').textContent = S.matches.length;
  $('ctrTeam').textContent = S.teams.length;
  $('adminCrud').innerHTML = `
    <table class="crud-tbl"><thead><tr><th>Team</th><th>League</th><th>Stadium</th><th></th></tr></thead>
    <tbody>${S.teams.map(t => `<tr><td>${t.name}</td><td>${t.league}</td><td>${t.stadium}</td><td><button class="ghost-btn" onclick="delEntity('teams',${t.id})">Del</button></td></tr>`).join('')}</tbody></table>
    <table class="crud-tbl"><thead><tr><th>Player</th><th>Team</th><th>Position</th><th></th></tr></thead>
    <tbody>${S.players.map(p => `<tr><td>#${p.shirtNumber} ${p.name}</td><td>${p.team}</td><td>${p.position}</td><td><button class="ghost-btn" onclick="delEntity('players',${p.id})">Del</button></td></tr>`).join('') || '<tr><td colspan="4" class="empty-state">No data</td></tr>'}</tbody></table>`;
}

async function delEntity(type, id) {
  if (!confirm(`Delete?`)) return;
  try { await api(`/admin/${type}/${id}`, {method:'DELETE'}); showMessage('Deleted.'); await loadAll(); } catch(e){ showMessage(e.message); }
}

function renderLeagues() {
  $('leagueSelect').innerHTML = '<option value="all">All leagues</option>' + S.leagues.map(l => `<option value="${l.id}">${l.name}</option>`).join('');
}

/* ════ LOAD ALL ════ */
async function loadAll() {
  [S.matches, S.teams, S.players, S.leagues] = await Promise.all([api('/matches'), api('/teams'), api('/players'), api('/leagues')]);
  if (S.user) await renderPrivate();
  renderFront();
  renderClubs();
  renderSchedule();
  renderAdminChoices();
  renderLeagues();
  await renderStandings();
}

async function toggleFav(teamId) {
  try { await api(`/favorites/teams/${teamId}`, {method:'POST'}); await renderPrivate(); renderFront(); await renderStandings(); toast('Favorite saved.'); } catch(e){ toast(e.message); }
}
async function unfav(teamId) {
  await api(`/favorites/teams/${teamId}`, {method:'DELETE'}); await renderPrivate(); renderFront();
}

/* ════ NAV ════ */
function switchView(v) {
  document.querySelectorAll('.nav-link').forEach(b => b.classList.toggle('active', b.dataset.view === v));
  document.querySelectorAll('.page').forEach(p => p.classList.toggle('active', p.id === v));
}
document.querySelectorAll('.nav-link').forEach(b => b.addEventListener('click', () => switchView(b.dataset.view)));

/* Auth */
$('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  try { setSession(await api('/auth/login', {method:'POST',body:JSON.stringify(Object.fromEntries(new FormData(e.target)))})); showMessage('Logged in.'); await loadAll(); } catch(err) { showMessage(err.message); }
});
$('registerForm').addEventListener('submit', async e => {
  e.preventDefault();
  try { const b=Object.fromEntries(new FormData(e.target)); if(b.password.length<8) throw new Error('Password ≥ 8 chars.'); setSession(await api('/auth/register', {method:'POST',body:JSON.stringify(b)})); showMessage('Registered.'); await loadAll(); } catch(err) { showMessage(err.message); }
});
$('logoutBtn').addEventListener('click', () => {
  localStorage.removeItem('gp.t'); localStorage.removeItem('gp.rt'); localStorage.removeItem('gp.u');
  S.token=''; S.refreshToken=''; S.user=null; S.favorites=[]; S.notifications=[];
  renderSession(); loadAll(); toast('Logged out.');
});

/* Search */
$('searchInput').addEventListener('input', async e => {
  const q = e.target.value.trim();
  if (!q) { $('searchResults').classList.add('hidden'); return; }
  const r = await api(`/search?q=${encodeURIComponent(q)}`);
  $('searchResults').classList.remove('hidden');
  $('searchResults').innerHTML = r.map(x => {
    const label = x.item.name || `${x.item.homeTeam?.name} vs ${x.item.awayTeam?.name}`;
    return `<div class="notif-item" style="cursor:pointer"><strong>${x.type}</strong><p>${label}</p></div>`;
  }).join('') || empty('No results.');
});
document.addEventListener('click', e => { if (!e.target.closest('.search-strip')) $('searchResults').classList.add('hidden'); });

/* Misc */
$('dateFilter').addEventListener('change', renderSchedule);
$('themeBtn').addEventListener('click', () => document.body.classList.toggle('dark'));
$('exportBtn').addEventListener('click', async () => {
  try { const csv = await api('/export/standings.csv'); const a=document.createElement('a'); a.href=URL.createObjectURL(new Blob([csv],{type:'text/csv'})); a.download='standings.csv'; a.click(); } catch(e){ toast(e.message); }
});
$('scoreForm').addEventListener('submit', async e => {
  e.preventDefault();
  try { const b=Object.fromEntries(new FormData(e.target)); await api(`/admin/matches/${b.matchId}`, {method:'PUT',body:JSON.stringify(b)}); showMessage('Score updated.'); await loadAll(); } catch(err){ showMessage(err.message); }
});
$('eventForm').addEventListener('submit', async e => {
  e.preventDefault();
  try { const b=Object.fromEntries(new FormData(e.target)); const mid=$('adminMatchSelect').value; await api(`/admin/matches/${mid}/events`, {method:'POST',body:JSON.stringify(b)}); showMessage('Event added.'); await loadAll(); await openMatchPage(parseInt(mid)); } catch(err){ showMessage(err.message); }
});

renderSession();
loadAll().catch(e => toast(e.message));