let filterId = '';

async function loadMe() {
  const me = await api('/api/auth/me');
  if (me.role !== 'ADMIN') { location.href = 'dashboard.html'; return; }
  document.getElementById('who').textContent = me.loginId;
}

async function loadUsers() {
  const list = await api('/api/admin/users');
  document.getElementById('statUsers').textContent = list.length;
  document.getElementById('statLive').textContent = list.filter(u => u.monitoring).length;
  document.getElementById('statTotal').textContent = list.reduce((a, u) => a + u.total, 0);
  document.getElementById('statSpam').textContent = list.reduce((a, u) => a + u.spam, 0);
  document.getElementById('usersBody').innerHTML = list.map(u => `
    <tr>
      <td>${esc(u.loginId)}</td>
      <td><span class="badge ${u.role === 'ADMIN' ? 'bg-danger' : 'bg-primary'}">${esc(u.role)}</span></td>
      <td class="small">${esc(u.mailAddress || '-')}</td>
      <td>${u.monitoring ? '<span class="badge bg-success">Live</span>' : '<span class="badge bg-secondary">Off</span>'}</td>
      <td>${u.total}</td>
      <td>${u.spam}</td>
      <td class="small">${esc(fmtTime(u.createdAt))}</td>
      <td><button class="btn btn-sm btn-outline-primary" data-id="${esc(u.loginId)}">View history</button></td>
    </tr>`).join('');
  document.querySelectorAll('#usersBody button').forEach(b => {
    b.onclick = () => { filterId = b.dataset.id; applyFilterUi(); loadHistory(); };
  });
}

function applyFilterUi() {
  const lbl = document.getElementById('filterLabel');
  lbl.textContent = filterId;
  lbl.classList.toggle('d-none', !filterId);
  document.getElementById('clearFilter').classList.toggle('d-none', !filterId);
}

async function loadHistory() {
  const list = await api('/api/admin/history' + (filterId ? '?loginId=' + encodeURIComponent(filterId) : ''));
  document.getElementById('historyBody').innerHTML = list.length ? list.map(l => `
    <tr class="${l.label === 'SPAM' ? 'row-spam' : ''}">
      <td class="small">${esc(fmtTime(l.scannedAt))}</td>
      <td>${esc(l.loginId)}</td>
      <td class="small">${esc(l.sender)}</td>
      <td class="small">${esc(l.source)}</td>
      <td>${labelBadge(l.label)}</td>
      <td class="small">${(l.score * 100).toFixed(1)}%</td>
    </tr>`).join('') : '<tr><td colspan="6" class="text-muted">No history yet.</td></tr>';
}

document.getElementById('clearFilter').onclick = () => { filterId = ''; applyFilterUi(); loadHistory(); };

async function refresh() {
  try { await loadUsers(); await loadHistory(); } catch (e) { /* redirect handled in api() */ }
}

loadMe().then(() => { refresh(); setInterval(refresh, 10000); });
