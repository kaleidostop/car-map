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
        if (stompClient && stompClient.connected) {
            showToast('Сессия истекла, пожалуйста, войдите снова', 'danger');
        } else {
            window.location.href = '/login';
        }
    }
    return response;
}

function showToast(message, type = 'primary') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('data-bs-delay', 0);
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body fs-5">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>`;
    container.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { autohide: false });
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

let stompClient = null;

function connectWebSocket({ onRequest, onStatus } = {}) {
    const token = localStorage.getItem('jwt_token');
    if (!token) return;
    const socket = new SockJS('/ws?access_token=' + token);
    stompClient = StompJs.Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        stompClient.subscribe('/user/queue/requests', function(message) {
            const body = JSON.parse(message.body);
            showToast(body.message || `Новая заявка на поездку #${body.rideId}`);
            if (onRequest) onRequest(body);
        });
        stompClient.subscribe('/user/queue/request-status', function(message) {
            const body = JSON.parse(message.body);
            let type = 'primary';
            if (body.status === 'ACCEPTED') type = 'success';
            else if (body.status === 'REJECTED') type = 'danger';
            else if (body.status === 'PENDING') type = 'warning';
            showToast(body.message, type);
            if (onStatus) onStatus(body);
        });
    }, function(error) {
        console.error('WebSocket error:', error);
    });
}