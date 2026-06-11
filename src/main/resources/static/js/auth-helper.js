async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        window.location.href = '/login';
        return;
    }
    options.headers = {
        ...options.headers,
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    };
    const response = await fetch(url, options);
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
    }
    return response;
}
