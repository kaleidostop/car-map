const DEFAULT_CENTER = [59.939, 30.316];  // центр Санкт-Петербурга
const DEFAULT_ZOOM = 12;
let officeMarkers = []; 
let rideMarkers = [];

const token = localStorage.getItem('jwt_token');
if (!token) {
    window.location.href = '/login';
}

try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const role = payload.role;
    if (role === 'ROLE_DRIVER' || role === 'ROLE_ADMIN') {
        document.getElementById('create-ride-btn').style.display = 'inline-block';
    }
    document.getElementById('user-info').textContent = 
        `Вы вошли как: ${payload.sub} (роль: ${role})`;
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

async function loadOffices() {
    const response = await fetchWithAuth('/api/offices');
    if (!response.ok) {
        console.error('Failed to load offices');
        return;
    }
    const offices = await response.json();
    const officeList = document.getElementById('office-list');
    officeList.innerHTML = ''; 

    officeMarkers.forEach(m => map.removeLayer(m));
    officeMarkers = [];

    offices.forEach(office => {
        const marker = L.marker([office.latitude, office.longitude])
            .addTo(map)
            .bindPopup(`<b>${office.name}</b><br>${office.address}`);
        officeMarkers.push(marker);

        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = `<strong>${office.name}</strong><br><small>${office.address}</small>`;
        officeList.appendChild(li);
    });

}

async function loadRides() {
    const response = await fetchWithAuth('/api/rides');
    if (!response.ok) {
        console.error('Failed to load rides');
        return;
    }
    const rides = await response.json();

    rideMarkers.forEach(m => map.removeLayer(m));
    rideMarkers = [];

    const rideList = document.getElementById('ride-list');
    if (rideList) rideList.innerHTML = '';

    rides.forEach(ride => {
        const startMarker = L.marker([ride.departureLat, ride.departureLon], {
            icon: L.icon({
                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                iconSize: [25, 41],
                iconAnchor: [12, 41],
                popupAnchor: [1, -34],
                shadowSize: [41, 41]
            })
        }).bindPopup(`
            <b>Поездка #${ride.id}</b><br>
            Водитель: ${ride.driverName}<br>
            Офис: ${ride.officeName}<br>
            Место отправления:  ${ride.departureAddress}<br>
            Время: ${new Date(ride.departureTime).toLocaleString()}<br>
            Мест: ${ride.seatsAvailable} из ${ride.seatsTotal}<br>
            <button onclick="joinRide(${ride.id})" class="badge bg-success">Присоединиться</button>
        `);

        const polyline = L.polyline([
            [ride.departureLat, ride.departureLon],
            [ride.officeLat, ride.officeLon]
        ], { color: 'green', dashArray: '5,5' }).bindPopup(`Маршрут поездки #${ride.id}`);

        startMarker.addTo(map);
        polyline.addTo(map);
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
        rideList.appendChild(li);
    });

    const allMarkers = [...officeMarkers, ...rideMarkers];
    if (allMarkers.length > 0) {
        const group = L.featureGroup(allMarkers);
        map.fitBounds(group.getBounds().pad(0.1));
    }
}

function joinRide(rideId) {
    alert('Функция присоединения к поездке будет реализована позже');
}

(async function init() {
    await loadOffices();
    await loadRides();
})();
