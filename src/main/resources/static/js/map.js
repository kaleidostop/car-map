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
        loadRides(filterSelect.value);
    });
}


let selectedRideId = null;
let selectedLayers = []; 

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

        const popupContent = `
            <b>Поездка #${ride.id}</b><br>
            Водитель: ${ride.driverName}<br>
            Офис: ${ride.officeName}<br>
            Место отправления:  ${ride.departureAddress}<br>
            Время: ${new Date(ride.departureTime).toLocaleString()}<br>
            В пути: ${duration}<br>
            Мест: ${ride.seatsAvailable} из ${ride.seatsTotal}<br>
            <button onclick="joinRide(${ride.id})" class="badge bg-success">Присоединиться</button>
        `;

        const startMarker = L.marker([ride.departureLat, ride.departureLon], {
            icon: L.icon({
                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                iconSize: [25, 41],
                iconAnchor: [12, 41],
                popupAnchor: [1, -34],
                shadowSize: [41, 41]
            })
        }).bindPopup(popupContent);

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
            if (selectedLayers.length) {
                selectedLayers.forEach(layer => {
                    if (layer instanceof L.Marker) {
                    } else {
                        layer.setStyle({ color: '#999', weight: 5 });
                    }
                });
            }
            selectedRideId = ride.id;
            selectedLayers = [startMarker, polyline];
            if (polyline.setStyle) {
                polyline.setStyle({ color: 'green', weight: 6 });
                polyline.bringToFront();
            }
            if (startMarker.bringToFront) {
                startMarker.bringToFront(); 
            }
            document.querySelectorAll('#ride-list .list-group-item').forEach(el => el.classList.remove('active'));
            const rideItem = document.getElementById('ride-item-' + ride.id);
            if (rideItem) rideItem.classList.add('active');
        };

        startMarker.on('click', selectThisRide);
        if (polyline.on) {
            polyline.on('click',  function(e) {
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
        li.addEventListener('click', selectThisRide);
        rideList.appendChild(li);
    });

    const allMarkers = [...officeMarkers, ...rideMarkers];
    if (allMarkers.length > 0) {
        const group = L.featureGroup(allMarkers);
        map.fitBounds(group.getBounds().pad(0.1));
    }
}

function joinRide(rideId) {
    const lat = prompt('Введите широту вашего местоположения:');
    const lon = prompt('Введите долготу вашего местоположения:');
    if (!lat || !lon) return;

    fetchWithAuth('/api/rides/' + rideId + '/join', {
        method: 'POST',
        body: JSON.stringify({
            passengerLat: parseFloat(lat),
            passengerLon: parseFloat(lon)
        })
    })
    .then(async res => {
        const text = await res.text();
        let data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = { error: text || 'Неизвестная ошибка' };
        }
        
        if (!res.ok) {
            alert(data.error || 'Ошибка сервера: ' + res.status);
            return;
        }
        alert(data.message || 'Вы присоединились к поездке');
        loadRides();
    })
    .catch(e => alert('Ошибка соединения: ' + e.message));

}

(async function init() {
    await loadOffices();
    await loadRides();
})();
