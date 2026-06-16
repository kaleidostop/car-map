
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

async function loadMyRides() {
    const response = await fetchWithAuth('/api/rides/my');
    if (!response.ok) {
        document.getElementById('rides-container').innerHTML = '<p class="text-danger">Ошибка загрузки поездок</p>';
        return;
    }
    const rides = await response.json();
    renderRides(rides);
}

function renderRides(rides) {
    const container = document.getElementById('rides-container');
    if (rides.length === 0) {
        container.innerHTML = '<p>У вас пока нет созданных поездок.</p>';
        return;
    }
    container.innerHTML = '';
    rides.forEach(ride => {
        const card = document.createElement('div');
        card.className = 'card mb-3';
        card.innerHTML = `
            <div class="card-body">
                <h5>${ride.departureAddress} → ${ride.officeName}</h5>
                <p>${new Date(ride.departureTime).toLocaleString()}, 
                   мест: <span id="seats-${ride.id}">${ride.seatsAvailable}/${ride.seatsTotal}</span></p>
                <button class="btn btn-sm btn-info load-requests" data-ride-id="${ride.id}">Заявки</button>
                <div id="requests-${ride.id}" class="mt-2"></div>
            </div>
        `;
        container.appendChild(card);
    });

    document.querySelectorAll('.load-requests').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const rideId = e.target.dataset.rideId;
            loadRequestsForRide(rideId);
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
            await handleRequest(rideId, requestId, 'accept');
            loadRequestsForRide(rideId);
            loadMyRides();
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
    }
}

document.getElementById('logout-btn').addEventListener('click', () => {
    if (stompClient) stompClient.disconnect();
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

connectWebSocket();
loadMyRides();