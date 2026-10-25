let mode = 'login';
const tabLogin = document.getElementById('tabLogin');
const tabRegister = document.getElementById('tabRegister');
const submitBtn = document.getElementById('submitBtn');

function setMode(m) {
  mode = m;
  tabLogin.classList.toggle('active', m === 'login');
  tabRegister.classList.toggle('active', m === 'register');
  submitBtn.textContent = m === 'login' ? 'Login' : 'Create account';
  document.getElementById('msg').classList.add('d-none');
}
tabLogin.onclick = () => setMode('login');
tabRegister.onclick = () => setMode('register');

async function submit() {
  const loginId = document.getElementById('loginId').value.trim();
  const password = document.getElementById('password').value;
  try {
    if (mode === 'register') {
      const r = await api('/api/auth/register', 'POST', { loginId, password });
      showAlert('msg', r.message, 'success');
      setMode('login');
      showAlert('msg', r.message, 'success');
    } else {
      const r = await api('/api/auth/login', 'POST', { loginId, password });
      location.href = r.role === 'ADMIN' ? 'admin.html' : 'dashboard.html';
    }
  } catch (e) {
    showAlert('msg', e.message);
  }
}
submitBtn.onclick = submit;
document.getElementById('password').addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
