document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errorAlert = document.getElementById('error-alert');
    errorAlert.classList.add('d-none');

    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: document.getElementById('email').value,
            password: document.getElementById('password').value
        })
    });

    if (response.ok) {
        const data = await response.json();
        localStorage.setItem('jwt_token', data.token);
        window.location.href = '/map';
    } else {
        const err = await response.json();
        errorAlert.textContent = err.error || 'Ошибка входа';
        errorAlert.classList.remove('d-none');
    }
});