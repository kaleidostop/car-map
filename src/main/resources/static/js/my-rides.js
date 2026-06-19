
let stompClient = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = StompJs.Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('WebSocket connected: ' + frame);
        stompClient.subscribe('/user/queue/requests', function(message) {
            const body = JSON.parse(message.body);
            alert(`Новая заявка от ${body.passengerName} на поездку #${body.rideId}`);
            loadMyRides();
        });
    }, function(error) {
        console.error('WebSocket error:', error);
    });
}

function createRideCard(ride) {
    const template = document.getElementById('ride-card-template');
    const clone = template.content.cloneNode(true);
    
    clone.querySelector('.ride-title').textContent = `${ride.departureAddress} → ${ride.officeName}`;
    clone.querySelector('.ride-info').innerHTML = 
        `${new Date(ride.departureTime).toLocaleString()}, 
         мест: <span id="seats-${ride.id}">${ride.seatsAvailable}/${ride.seatsTotal}</span>`;
        
    if (ride.status === 'ACTIVE' || ride.status === 'FULL') {
        clone.querySelector('.load-requests').style.display = 'inline-block';
        clone.querySelector('.load-requests').dataset.rideId = ride.id;

        clone.querySelector('.cancel-ride-btn').style.display = 'inline-block';
        clone.querySelector('.cancel-ride-btn').dataset.rideId = ride.id;
    }

    if (ride.passengers && ride.passengers.length > 0) {
        const pList = clone.querySelector('.passengers-list');
        pList.innerHTML = `<strong>Пассажиры:</strong> ${ride.passengers.map(p => p.name).join(', ')}`;
        pList.style.display = 'block';
    }

    const statusClasses = {
        'ACTIVE': 'border-primary',
        'FULL': 'border-primary',
        'IN_PROGRESS': 'border-success',
        'COMPLETED': 'border-purple', 
        'CANCELLED': 'border-danger'
    };
    const cardDiv = clone.querySelector('.card');
    cardDiv.classList.add(statusClasses[ride.status] || '');
    
    clone.querySelector('.requests-container').id = `requests-${ride.id}`;
    return clone;
}

function renderRides(rides) {
    const groups = {
        'ACTIVE': { container: 'rides-active-or-full', title: 'Запланированные' },
        'FULL': { container: 'rides-active-or-full', title: 'Запланированные' },
        'IN_PROGRESS': { container: 'rides-in-progress', title: 'В процессе' },
        'COMPLETED': { container: 'rides-completed', title: 'Завершённые' },
        'CANCELLED': { container: 'rides-cancelled', title: 'Отменённые' }
    };
    Object.values(groups).forEach(g => {
        const el = document.getElementById(g.container);
        if (el) el.innerHTML = '';
    });
    
    if (rides.length === 0) {
        document.getElementById('rides-active').innerHTML = '<p>У вас пока нет созданных поездок.</p>';
        return;
    }

    rides.forEach(ride => {
        const group = groups[ride.status];
        if (!group) return;
        const container = document.getElementById(group.container);
        if (!container) return;
        const card = createRideCard(ride);
        container.appendChild(card);
    });
    
    Object.values(groups).forEach(g => {
        const el = document.getElementById(g.container);
        if (el && el.children.length === 0) {
            el.innerHTML = '<p class="text-muted">Нет поездок</p>';
        }
    });

    attachEventHandlers();
}

async function loadMyRides() {
    const response = await fetchWithAuth('/api/rides/my');
    if (!response.ok) {
        document.getElementById('rides-container').innerHTML = '<p class="text-danger">Ошибка загрузки поездок</p>';
        return;
    }
    const rides = await response.json();
    renderRides(rides);
}

function attachEventHandlers() {
    document.querySelectorAll('.load-requests').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const rideId = e.target.dataset.rideId;
            loadRequestsForRide(rideId);
        });
    });
    document.querySelectorAll('.cancel-ride-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const rideId = e.target.dataset.rideId;
            if (confirm('Отменить поездку?')) {
                await fetchWithAuth(`/api/rides/${rideId}/cancel`, { method: 'PATCH' });
                loadMyRides();
            }
        });
    });
}

async function loadRequestsForRide(rideId) {
    const reqDiv = document.getElementById('requests-' + rideId);
    reqDiv.innerHTML = '<p>Загрузка заявок...</p>';
    const response = await fetchWithAuth(`/api/rides/${rideId}/requests`);
    if (!response.ok) {
        reqDiv.innerHTML = '<p class="text-danger">Ошибка загрузки заявок</p>';
        return;
    }
    const requests = await response.json();
    renderRequests(rideId, requests);
}

function renderRequests(rideId, requests) {
    const reqDiv = document.getElementById('requests-' + rideId);
    if (requests.length === 0) {
        reqDiv.innerHTML = '<p>Нет новых заявок</p>';
        return;
    }
    reqDiv.innerHTML = requests.map(r => `
        <div class="d-flex align-items-center justify-content-between border p-2">
            <span>${r.passengerName} (${r.passengerLat}, ${r.passengerLon})</span>
            <div>
                <button class="btn btn-success btn-sm accept-btn" data-request-id="${r.id}" data-ride-id="${rideId}">Принять</button>
                <button class="btn btn-danger btn-sm reject-btn" data-request-id="${r.id}" data-ride-id="${rideId}">Отклонить</button>
            </div>
        </div>
    `).join('');

    attachRequestButtons();
}

function attachRequestButtons() {
    document.querySelectorAll('.accept-btn').forEach(btn => {
        btn.onclick = async (e) => {
            const requestId = e.target.dataset.requestId;
            const rideId = e.target.dataset.rideId;
            const resp = await handleRequest(rideId, requestId, 'accept');
            if (resp && resp.seatsAvailable !== undefined) {
                document.getElementById(`seats-${rideId}`).textContent = 
                    `${resp.seatsAvailable}/${document.getElementById(`seats-${rideId}`).textContent.split('/')[1]}`;
            }
            loadRequestsForRide(rideId);
        };
    });

    document.querySelectorAll('.reject-btn').forEach(btn => {
        btn.onclick = async (e) => {
            const requestId = e.target.dataset.requestId;
            const rideId = e.target.dataset.rideId;
            await handleRequest(rideId, requestId, 'reject');
            loadRequestsForRide(rideId);
        };
    });
}

async function handleRequest(rideId, requestId, action) {
    const response = await fetchWithAuth(`/api/rides/${rideId}/requests/${requestId}?action=${action}`, {
        method: 'PATCH'
    });
    if (!response.ok) {
        const err = await response.json();
        alert(err.error || 'Ошибка обработки заявки');
        return null;
    }
    return await response.json(); 
}

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

document.getElementById('logout-btn').addEventListener('click', () => {
    if (stompClient) stompClient.disconnect();
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

connectWebSocket();
loadMyRides();
