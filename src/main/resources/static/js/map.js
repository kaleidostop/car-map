const DEFAULT_CENTER = [59.939, 30.316];  // центр Санкт-Петербурга
const DEFAULT_ZOOM = 12;
let officeMarkers = []; 
let rideMarkers = [];
let selectedRideId = null;
let selectedLayers = []; 

let pickMode = false;
let pickModeCallback = null;
let tempMarker = null;

let currentUserEmail = null;
let currentUserRole = null;

const token = localStorage.getItem('jwt_token');
if (!token) {
    window.location.href = '/login';
}

let pendingCount = 0;
const badge = document.getElementById('requests-badge');

// Функция обновления бейджа
function updateBadge(count) {
    if (badge) {
        if (count > 0) {
            badge.textContent = count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    currentUserEmail = payload.sub;
    currentUserRole = payload.role;

    if (currentUserRole === 'ROLE_ADMIN') {
        document.getElementById('admin-link').style.display = 'inline-block';
    }
    if (currentUserRole === 'ROLE_DRIVER' || currentUserRole === 'ROLE_ADMIN') {
        document.getElementById('create-ride-btn').style.display = 'inline-block';
        document.getElementById('my-rides-btn').style.display = 'inline-block';
    }
    document.getElementById('user-info').textContent = 
        `Вы вошли как: ${currentUserEmail} (роль: ${currentUserRole})`;
} catch(e) {
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
}

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

const map = L.map('map', {attributionControl: false}).setView(DEFAULT_CENTER, DEFAULT_ZOOM);
var myAttrControl = L.control.attribution().addTo(map);
myAttrControl.setPrefix('<a href="https://leafletjs.com/">Leaflet</a>');

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org">OpenStreetMap</a>',
    maxZoom: 19
}).addTo(map);


function enterPickMode(callback) {
    pickMode = true;
    pickModeCallback = callback;
    map.getContainer().style.cursor = 'crosshair';
    document.getElementById('pick-controls').style.display = 'block';
    document.getElementById('pick-instruction').textContent = 'Кликните по карте, чтобы выбрать точку';
}

function exitPickMode() {
    pickMode = false;
    pickModeCallback = null;
    map.getContainer().style.cursor = '';
    document.getElementById('pick-controls').style.display = 'none';
    if (tempMarker) {
        map.removeLayer(tempMarker);
        tempMarker = null;
    }
}

map.on('click', function(e) {
    if (pickMode) {
        const latlng = e.latlng;
        if (tempMarker) {
            tempMarker.setLatLng(latlng);
        } else {
            tempMarker = L.marker(latlng, {
                icon: L.icon({
                    iconUrl: '../img/marker-icon-2x-red.png',
                    shadowUrl: '../img/marker-shadow.png',
                    iconSize: [25, 41],
                    iconAnchor: [12, 41],
                    popupAnchor: [1, -34],
                    shadowSize: [41, 41]
                })
            }).addTo(map);
        }
        document.getElementById('pick-instruction').textContent = 'Точка выбрана. Можно подтвердить или кликнуть ещё раз.';
    } else {
        clearSelection();
    }
});

function clearSelection() {
    if (selectedLayers.length) {
        selectedLayers.forEach(layer => {
            if (layer.setStyle && !layer.setLatLng) {
                layer.setStyle({ color: '#999', weight: 4 });
            }
        });
    }

    rideMarkers.forEach(layer => {
        if (layer._rideId !== undefined) {
            map.removeLayer(layer);
        }
    });

    document.querySelectorAll('#ride-list .list-group-item').forEach(el => el.classList.remove('active'));
    
    selectedRideId = null;
    selectedLayers = [];
}

const urlParams = new URLSearchParams(window.location.search);
const pickModeParam = urlParams.get('mode');
const returnUrl = urlParams.get('returnUrl');

const initLat = parseFloat(urlParams.get('lat'));
const initLng = parseFloat(urlParams.get('lng'));
if (!isNaN(initLat) && !isNaN(initLng)) {
    map.setView([initLat, initLng], 15);
}

if (pickModeParam && returnUrl) {
    enterPickMode(async function(lat, lng) {
        let address = '';
        try {
            const resp = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`);
            const data = await resp.json();
            address = data.display_name || `${lat}, ${lng}`;
        } catch(e) {
            address = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
        }
        const pickedData = { lat, lng, address };
        sessionStorage.setItem('pickedCoordinates', JSON.stringify(pickedData));
        window.location.href = returnUrl;
    });
    document.getElementById('pick-cancel-btn').onclick = function() {
        exitPickMode();
        window.location.href = returnUrl; 
    };
}

document.getElementById('pick-confirm-btn').addEventListener('click', function() {
    if (!tempMarker) {
        alert('Пожалуйста, сначала кликните по карте, чтобы выбрать точку.');
        return;
    }
    const latlng = tempMarker.getLatLng();
    const callback = pickModeCallback; 
    exitPickMode();
    if (callback) {
        callback(latlng.lat, latlng.lng);
    }
});

document.getElementById('pick-cancel-btn').addEventListener('click', function() {
    exitPickMode();
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && pickMode) {
        exitPickMode();
    }
});


async function loadOffices() {
    const response = await fetchWithAuth('/api/offices');
    if (!response.ok) {
        console.error('Failed to load offices');
        return;
    }
    const offices = await response.json();

    officeMarkers.forEach(m => map.removeLayer(m));
    officeMarkers = [];

    const filterSelect = document.getElementById('office-filter');

    offices.forEach(office => {
        const marker = L.marker([office.latitude, office.longitude])
            .addTo(map)
            .bindPopup(`<b>${office.name}</b><br>${office.address}`);
        officeMarkers.push(marker);

        const option = document.createElement('option');
        option.value = office.id;
        option.textContent = office.name;
        filterSelect.appendChild(option);
    });

    filterSelect.addEventListener('change', () => {
        if (pickMode) return;  
        loadRides(filterSelect.value);
    });
}

async function loadRides(officeId = null) {
    let url = '/api/rides';
    if (officeId) url += '?officeId=' + officeId;

    const response = await fetchWithAuth(url);
    if (!response.ok) {
        console.error('Failed to load rides');
        return;
    }
    const rides = await response.json();

    rideMarkers.forEach(m => map.removeLayer(m));
    rideMarkers = [];
    selectedLayers = [];
    selectedRideId = null;

    const rideList = document.getElementById('ride-list');
    if (rideList) rideList.innerHTML = '';

    rides.forEach(ride => {
        const duration = ride.durationSeconds ? Math.round(ride.durationSeconds / 60) + ' мин' : 'неизвестно';

        const isOwnRide = (ride.driverEmail === currentUserEmail);

        let joinButton = '';
        if (isOwnRide) {
            joinButton = '<button class="btn btn-sm btn-warning" style="cursor:default;">Моя поездка</button>';
        } else {
            joinButton = `<button class="btn btn-sm btn-success" onclick="joinRide(${ride.id})">Присоединиться</button>`;
        }

        const popupContent = `
            <b>Поездка #${ride.id}</b><br>
            Водитель: ${ride.driverName}<br>
            Офис: ${ride.officeName}<br>
            Место отправления:  ${ride.departureAddress}<br>
            Время: ${new Date(ride.departureTime).toLocaleString()}<br>
            В пути: ${duration}<br>
            Мест: ${ride.seatsAvailable} из ${ride.seatsTotal}<br>
            ${joinButton}
        `;

        const startMarker = L.marker([ride.departureLat, ride.departureLon], {
            icon: L.icon({
                iconUrl: '../img/marker-icon-2x-green.png',
                shadowUrl: '../img/marker-shadow.png',
                iconSize: [25, 41],
                iconAnchor: [12, 41],
                popupAnchor: [1, -34],
                shadowSize: [41, 41]
            })
        }).bindPopup(popupContent);

        if (ride.passengers && ride.passengers.length > 0) {
            ride.passengers.forEach(p => {
                const passengerMarker = L.marker([p.lat, p.lng], {
                    icon: L.icon({
                        iconUrl: '../img/marker-icon-2x-yellow.png',
                        shadowUrl: '../img/marker-shadow.png',
                        iconSize: [25, 41],
                        iconAnchor: [12, 41],
                        popupAnchor: [1, -34],
                        shadowSize: [41, 41]
                    })
                }).bindPopup(`<b>Посадка пассажира</b><br>${p.name}`);

                passengerMarker._rideId = ride.id;
                rideMarkers.push(passengerMarker);
            });
        }

        let polyline;
        if (ride.routeGeometry) {
            polyline = L.geoJSON(ride.routeGeometry, {
                style: { color: '#999', weight: 5 }
            }).bindPopup(popupContent);
        } else {
            polyline = L.polyline([
                [ride.departureLat, ride.departureLon],
                [ride.officeLat, ride.officeLon]
            ], { color: '#999', dashArray: '5,5' }).bindPopup(popupContent);
        }

        const selectThisRide = () => {
            clearSelection();

            selectedRideId = ride.id;
            selectedLayers = [startMarker, polyline];

            if (polyline.setStyle) {
                polyline.setStyle({ color: 'green', weight: 6 });
                polyline.bringToFront();
            }
            if (startMarker.bringToFront) {
                startMarker.bringToFront(); 
            }
            rideMarkers.forEach(layer => {
                if (layer._rideId === ride.id) {
                    layer.addTo(map);
                    if (layer.bringToFront) {
                        layer.bringToFront();
                    }
                }
            });

            document.querySelectorAll('#ride-list .list-group-item').forEach(el => el.classList.remove('active'));
            const rideItem = document.getElementById('ride-item-' + ride.id);
            if (rideItem) rideItem.classList.add('active');
        };

        startMarker.on('click', function(e) {
            if (pickMode) {
                this.closePopup();    
                return;
            }        
            selectThisRide();
        });

        if (polyline.on) {
            polyline.on('click',  function(e) {
                if (pickMode) {
                    this.closePopup();    
                    return;
                }       
                selectThisRide();   
                this.openPopup(e.latlng);  
            });
        }
        
        startMarker.addTo(map);
        if (polyline.addTo) {
            polyline.addTo(map);
        }

        rideMarkers.push(startMarker);
        rideMarkers.push(polyline);

        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = `
            <strong>${ride.driverName}</strong><br>
            ${ride.departureAddress} → ${ride.officeName}<br>
            <small>${new Date(ride.departureTime).toLocaleString()}</small><br>
            Свободных мест: <span class="badge bg-success">${ride.seatsAvailable}</span>
        `;
        li.addEventListener('click', function(e) {
            if (pickMode) return;
            selectThisRide();
        });
        rideList.appendChild(li);
    });

    const allMarkers = [...officeMarkers, ...rideMarkers];
    if (allMarkers.length > 0) {
        const group = L.featureGroup(allMarkers);
        map.fitBounds(group.getBounds().pad(0.1));
    }
}

function joinRide(rideId) {
    enterPickMode(async function(lat, lng) {
        try {
            const response = await fetchWithAuth(`/api/rides/${rideId}/join`, {
                method: 'POST',
                body: JSON.stringify({
                    passengerLat: lat,
                    passengerLon: lng
                })
            });
            const text = await response.text();
            let data;
            try {
                data = JSON.parse(text);
            } catch (e) {
                data = { error: text || 'Неизвестная ошибка' };
            }
            if (!response.ok) {
                alert(data.error || 'Ошибка при отправке заявки');
            } else {
                let message = data.message || 'Заявка отправлена';
                if (data.warning) {
                    message += '\n' + data.warning;
                }
                alert(message);
                loadRides();
            }
        } catch (err) {
            alert('Ошибка соединения: ' + err.message);
        }
    });
}

async function loadPendingCount() {
    try {
        const resp = await fetchWithAuth('/api/rides/my/pending-requests-count');
        if (resp.ok) {
            const count = await resp.json();
            pendingCount = count;
            updateBadge(pendingCount);
        }
    } catch(e) {}
}

(async function init() {
    await loadOffices();
    await loadRides();
    loadPendingCount();
})();

let stompClient = null;

function connectWebSocket() {
    const token = localStorage.getItem('jwt_token');
    const socket = new SockJS('/ws?access_token=' + token);
    stompClient = StompJs.Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('WS connected: ' + frame);

        stompClient.subscribe('/user/queue/requests', function(message) {
            const body = JSON.parse(message.body);
            pendingCount++;
            updateBadge(pendingCount);
            alert(`Новая заявка от ${body.passengerName} на поездку #${body.rideId}`);
            loadRides();
        });

        stompClient.subscribe('/user/queue/request-status', function(message) {
            const body = JSON.parse(message.body);
            alert(body.message || 'Обновление статуса заявки');
            loadRides();
        });
    }, function(error) {
        console.error('WS error:', error);
    });
}

connectWebSocket();
