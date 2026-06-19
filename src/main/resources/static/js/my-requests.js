let stompClient = null;

const token = localStorage.getItem('jwt_token');
if (!token) {
    window.location.href = '/login';
}

const groups = {
    'PENDING': { container: 'requests-pending', color: 'border-warning' },
    'ACCEPTED': { container: 'requests-accepted', color: 'border-success' },
    'REJECTED': { container: 'requests-rejected', color: 'border-danger' }
};

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

function renderRequests(requests) {
    Object.values(groups).forEach(g => {
        document.getElementById(g.container).innerHTML = '';
    });

    if (requests.length === 0) {
        document.getElementById('requests-pending').innerHTML = '<p>У вас пока нет заявок.</p>';
        return;
    }

    requests.forEach(req => {
        const group = groups[req.status];
        if (!group) return;
        const card = createRequestCard(req, group.color);
        document.getElementById(group.container).appendChild(card);
    });

    Object.values(groups).forEach(g => {
        const el = document.getElementById(g.container);
        if (el.children.length === 0) {
            el.innerHTML = '<p class="text-muted">Нет заявок</p>';
        }
    });
}

function createRequestCard(req, borderColor) {
    const template = document.getElementById('request-card-template');
    const clone = template.content.cloneNode(true);
    
    clone.querySelector('.card').classList.add(borderColor);
    clone.querySelector('.ride-info').textContent = `${req.departureAddress} → ${req.officeName}`;
    clone.querySelector('.driver-name').textContent = `Водитель: ${req.driverName}`;
    clone.querySelector('.departure-time').textContent = 
        `Отправление: ${new Date(req.departureTime).toLocaleString()}`;
    clone.querySelector('.seats-info').textContent = `Осталось мест: ${req.seatsAvailable}`;
    
    return clone;
}


async function loadMyRequests() {
    const response = await fetchWithAuth('/api/requests/my');
    if (!response.ok) {
        document.getElementById('requests-pending').innerHTML = '<p class="text-danger">Ошибка загрузки</p>';
        return;
    }
    const requests = await response.json();
    renderRequests(requests);
}

document.getElementById('logout-btn').addEventListener('click', () => {
    if (stompClient) stompClient.disconnect();
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

connectWebSocket();
loadMyRequests();