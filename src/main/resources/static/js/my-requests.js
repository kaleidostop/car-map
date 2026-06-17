let stompClient = null;

const token = localStorage.getItem('jwt_token');
if (!token) {
    window.location.href = '/login';
}

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = StompJs.Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/queue/request-status', function(message) {
            const body = JSON.parse(message.body);
            alert(body.message);
            loadMyRequests(); 
        });
    }, function(error) {
        console.error('WebSocket error:', error);
    });
}

async function loadMyRequests() {
    const response = await fetchWithAuth('/api/requests/my');
    const container = document.getElementById('requests-container');
    if (!response.ok) {
        container.innerHTML = '<p class="text-danger">Ошибка загрузки заявок</p>';
        return;
    }
    const requests = await response.json();
    if (requests.length === 0) {
        container.innerHTML = '<p>У вас пока нет заявок.</p>';
        return;
    }
    container.innerHTML = requests.map(r => `
        <div class="card mb-3">
            <div class="card-body">
                <h5>${r.departureAddress} → ${r.officeName}</h5>
                <p><strong>Водитель:</strong> ${r.driverName}</p>
                <p><strong>Отправление:</strong> ${new Date(r.departureTime).toLocaleString()}</p>
                <p><strong>Статус:</strong> <span class="badge bg-${statusBadge(r.status)}">${statusLabel(r.status)}</span></p>
                <p><strong>Осталось мест:</strong> ${r.seatsAvailable}</p>
            </div>
        </div>
    `).join('');
}

function statusBadge(status) {
    switch(status) {
        case 'PENDING': return 'warning';
        case 'ACCEPTED': return 'success';
        case 'REJECTED': return 'danger';
        default: return 'secondary';
    }
}

function statusLabel(status) {
    switch(status) {
        case 'PENDING': return 'Ожидает';
        case 'ACCEPTED': return 'Принята';
        case 'REJECTED': return 'Отклонена';
        default: return status;
    }
}

document.getElementById('logout-btn').addEventListener('click', () => {
    if (stompClient) stompClient.disconnect();
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

connectWebSocket();
loadMyRequests();