const DEFAULT_CENTER = [59.939, 30.316];  // центр Санкт-Петербурга
const DEFAULT_ZOOM = 12;

const TEST_OFFICES = [
    { name: 'Главный корпус', address: 'Санкт-Петербург, Кронверкский пр., 49', lat: 59.956363, lon: 30.310011 },
    { name: 'Корпус на Ломоносова', address: 'Санкт-Петербург, ул. Ломоносова, 9', lat: 59.927288, lon: 30.338353 },
    { name: 'Спортивный комплекс', address: 'Санкт-Петербург, Вяземский пер., 5-7', lat: 59.972631, lon: 30.302501 }
];


const token = localStorage.getItem('jwt_token');
if (!token) {
    window.location.href = '/login';
}

try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    document.getElementById('user-info').textContent = 
        `Вы вошли как: ${payload.sub} (роль: ${payload.role})`;
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

const officeList = document.getElementById('office-list');

const markers = []; 
TEST_OFFICES.forEach(office => {
    const marker = L.marker([office.lat, office.lon])
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
