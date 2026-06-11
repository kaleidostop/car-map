const DEFAULT_CENTER = [59.939, 30.316];  // центр Санкт-Петербурга
const DEFAULT_ZOOM = 12;

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
    const markers = []; 
    offices.forEach(office => {
        const marker = L.marker([office.latitude, office.longitude])
            .addTo(map)
            .bindPopup(`<b>${office.name}</b><br>${office.address}`);
        markers.push(marker);

        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = `<strong>${office.name}</strong><br><small>${office.address}</small>`;
        officeList.appendChild(li);
    });

    if (markers.length > 0) {
        const group = L.featureGroup(markers);
        map.fitBounds(group.getBounds().pad(0.1));
    }
}

loadOffices();

