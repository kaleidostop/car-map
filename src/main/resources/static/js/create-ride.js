async function loadOffices() {
    const response = await fetchWithAuth('/api/offices');
    if (!response.ok) return;
    const offices = await response.json();
    const select = document.getElementById('office');
    select.innerHTML = '<option value="">Выберите офис</option>';
    offices.forEach(office => {
        const option = document.createElement('option');
        option.value = office.id;
        option.textContent = `${office.name} (${office.address})`;
        select.appendChild(option);
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        window.location.href = '/login';
        return;
    }

    document.getElementById('logout-btn')?.addEventListener('click', () => {
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
    });

    await loadOffices();

    const savedForm = sessionStorage.getItem('createRideFormData');
    const savedCoords = sessionStorage.getItem('pickedCoordinates');

    if (savedForm) {
        const f = JSON.parse(savedForm);
        if (f.officeId) document.getElementById('office').value = f.officeId;
        if (f.departureTime) document.getElementById('departureTime').value = f.departureTime;
        if (f.seatsTotal) document.getElementById('seatsTotal').value = f.seatsTotal;
        sessionStorage.removeItem('createRideFormData');
    }

    if (savedCoords) {
        const coords = JSON.parse(savedCoords);
        document.getElementById('departureLat').value = coords.lat;
        document.getElementById('departureLon').value = coords.lng;
        const addrField = document.getElementById('departureAddress');
        if (!addrField.value.trim()) {
            addrField.value = coords.address || `${coords.lat}, ${coords.lng}`;
        }
        const coordsDiv = document.getElementById('departureCoords');
        if (coordsDiv) {
            coordsDiv.textContent = `Координаты: ${coords.lat.toFixed(6)}, ${coords.lng.toFixed(6)}`;
        }

        sessionStorage.removeItem('pickedCoordinates');
    }

    
    document.getElementById('pick-departure-btn').addEventListener('click', function(e) {
        e.preventDefault();
        const formData = {
            officeId: document.getElementById('office').value,
            departureTime: document.getElementById('departureTime').value,
            seatsTotal: document.getElementById('seatsTotal').value
        };
        sessionStorage.setItem('createRideFormData', JSON.stringify(formData));
        window.location.href = '/map?mode=pick-departure&returnUrl=%2Fcreate-ride';
    });


    document.getElementById('ride-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = e.target.querySelector('button[type="submit"]');
        submitBtn.disabled = true;

        const data = {
            officeId: parseInt(document.getElementById('office').value),
            departureAddress: document.getElementById('departureAddress').value,
            departureLat: parseFloat(document.getElementById('departureLat').value),
            departureLon: parseFloat(document.getElementById('departureLon').value),
            departureTime: document.getElementById('departureTime').value,
            seatsTotal: parseInt(document.getElementById('seatsTotal').value),
            manualApproval: document.getElementById('manualApproval').checked,
            maxDetourMinutes: parseInt(document.getElementById('maxDetourMinutes').value) || null,
            maxDetourMeters: parseFloat(document.getElementById('maxDetourMeters').value) || null
        };

        const response = await fetchWithAuth('/api/rides', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        submitBtn.disabled = false;

        if (response.ok) {
            showToast('Поездка создана!', 'success');
            window.location.href = '/map';
        } else {
            const err = await response.json();
            showToast('Ошибка: ' + (err.error || 'Не удалось создать поездку'), 'danger');
        }
    });

    document.getElementById('manualApproval').addEventListener('change', function() {
        document.getElementById('auto-limits').style.display = this.checked ? 'none' : 'block';
    });

    connectWebSocket();
});