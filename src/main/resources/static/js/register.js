document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errorAlert = document.getElementById('error-alert');
    const successAlert = document.getElementById('success-alert');
    errorAlert.classList.add('d-none');
    successAlert.classList.add('d-none');

    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: document.getElementById('email').value,
            password: document.getElementById('password').value,
            fullName: document.getElementById('fullName').value,
            role: document.getElementById('role').value
        })
    });

    if (response.ok) {
        const data = await response.json();
        successAlert.textContent = 'Регистрация успешна! Сейчас вы будете перенаправлены...';
        successAlert.classList.remove('d-none');
        localStorage.setItem('jwt_token', data.token);
        setTimeout(() => window.location.href = '/map', 1500);
    } else {
        const err = await response.json();
        errorAlert.textContent = err.error || 'Ошибка регистрации';
        errorAlert.classList.remove('d-none');
    }
});