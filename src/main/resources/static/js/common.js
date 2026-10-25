async function api(url, method = 'GET', body) {
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { location.href = 'index.html'; throw new Error('Please log in'); }
  if (res.status === 403) { location.href = 'dashboard.html'; throw new Error('Not allowed'); }
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (!res.ok) throw new Error((data && data.message) || ('Error ' + res.status));
  return data;
}

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtTime(t) {
  if (!t) return '';
  return new Date(t).toLocaleString();
}

function labelBadge(label) {
  return label === 'SPAM'
    ? '<span class="badge badge-spam">SPAM</span>'
    : '<span class="badge badge-safe">SAFE</span>';
}

function showAlert(id, msg, type = 'danger') {
  const el = document.getElementById(id);
  el.className = 'alert alert-' + type;
  el.textContent = msg;
  el.classList.remove('d-none');
}

async function logout() {
  try { await api('/api/auth/logout', 'POST'); } catch (e) { /* ignore */ }
  location.href = 'index.html';
}
