async function loadOffices() {
    const response = await fetchWithAuth('/api/offices');
    if (response.ok) {
        const offices = await response.json();
        const select = document.getElementById('office');
        offices.forEach(office => {
            const option = document.createElement('option');
            option.value = office.id;
            option.textContent = `${office.name} (${office.address})`;
            select.appendChild(option);
        });
    }
}

document.getElementById('ride-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = {
        officeId: parseInt(document.getElementById('office').value),
        departureAddress: document.getElementById('departureAddress').value,
        departureLat: parseFloat(document.getElementById('departureLat').value),
        departureLon: parseFloat(document.getElementById('departureLon').value),
        departureTime: document.getElementById('departureTime').value,
        seatsTotal: parseInt(document.getElementById('seatsTotal').value)
    };

    const response = await fetchWithAuth('/api/rides', {
        method: 'POST',
        body: JSON.stringify(data)
    });
    if (response.ok) {
        alert('Поездка создана!');
        window.location.href = '/map';
    } else {
        const err = await response.json();
        alert('Ошибка: ' + (err.error || 'Не удалось создать поездку'));
    }
});

loadOffices();