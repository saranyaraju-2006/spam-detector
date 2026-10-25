async function loadStatus() {
  const s = await api('/api/user/status');
  document.getElementById('who').textContent = s.loginId;
  const dot = document.getElementById('liveDot');
  const txt = document.getElementById('liveText');
  if (s.monitoring) {
    dot.classList.add('on');
    txt.textContent = 'Live: monitoring ' + s.mailAddress;
  } else {
    dot.classList.remove('on');
    txt.textContent = s.mailAddress ? 'Paused (' + s.mailAddress + ')' : 'Not connected';
  }
}

async function loadHistory() {
  const list = await api('/api/user/history');
  const spam = list.filter(l => l.label === 'SPAM').length;
  document.getElementById('statTotal').textContent = list.length;
  document.getElementById('statSpam').textContent = spam;
  document.getElementById('statSafe').textContent = list.length - spam;

  const body = document.getElementById('historyBody');
  if (!list.length) {
    body.innerHTML = '<tr><td colspan="5" class="text-muted">No emails scanned yet.</td></tr>';
    return;
  }
  body.innerHTML = list.map(l => `
    <tr class="${l.label === 'SPAM' ? 'row-spam' : ''}">
      <td class="small">${esc(fmtTime(l.scannedAt))}</td>
      <td class="small">${esc(l.sender)}</td>
      <td class="small">${esc(l.subject)}</td>
      <td>${labelBadge(l.label)}</td>
      <td class="small">${(l.score * 100).toFixed(1)}%</td>
    </tr>`).join('');
}

async function refresh() {
  try { await loadStatus(); await loadHistory(); } catch (e) { /* redirect handled in api() */ }
}

document.getElementById('connectBtn').onclick = async () => {
  const btn = document.getElementById('connectBtn');
  btn.disabled = true; btn.textContent = 'Connecting...';
  try {
    const r = await api('/api/user/mail', 'POST', {
      mailAddress: document.getElementById('mailAddress').value,
      appPassword: document.getElementById('appPassword').value,
      imapHost: document.getElementById('imapHost').value
    });
    document.getElementById('appPassword').value = '';
    showAlert('mailMsg', r.message, 'success');
    refresh();
  } catch (e) {
    showAlert('mailMsg', e.message);
  } finally {
    btn.disabled = false; btn.textContent = 'Connect & start monitoring';
  }
};

document.getElementById('stopBtn').onclick = async () => {
  try {
    const r = await api('/api/user/mail/stop', 'POST');
    showAlert('mailMsg', r.message, 'info');
    refresh();
  } catch (e) { showAlert('mailMsg', e.message); }
};

document.getElementById('checkBtn').onclick = async () => {
  const out = document.getElementById('checkResult');
  try {
    const r = await api('/api/user/check', 'POST', { text: document.getElementById('checkText').value });
    out.innerHTML = labelBadge(r.label) + ' <span class="ms-2">Spam probability: ' + (r.score * 100).toFixed(1) + '%</span>';
    refresh();
  } catch (e) {
    out.innerHTML = '<span class="text-danger">' + esc(e.message) + '</span>';
  }
};

refresh();
setInterval(refresh, 5000);
