let officeModal, deleteModal;

async function loadOffices() {
    const response = await fetchWithAuth('/api/offices');
    const container = document.getElementById('offices-table-container');
    if (!response.ok) {
        container.innerHTML = '<p class="text-danger">Ошибка загрузки офисов</p>';
        return;
    }
    const offices = await response.json();
    renderTable(offices);
}

function renderTable(offices) {
    const container = document.getElementById('offices-table-container');
    if (offices.length === 0) {
        container.innerHTML = '<p>Нет офисов</p>';
        return;
    }
    let html = `
        <table class="table table-striped">
            <thead>
                <tr><th>ID</th><th>Название</th><th>Адрес</th><th>Широта</th><th>Долгота</th><th>Действия</th></tr>
            </thead>
            <tbody>
    `;
    offices.forEach(o => {
        html += `
            <tr>
                <td>${o.id}</td>
                <td>${escapeHtml(o.name)}</td>
                <td>${escapeHtml(o.address)}</td>
                <td>${o.latitude}</td>
                <td>${o.longitude}</td>
                <td>
                    <button class="btn btn-sm btn-warning edit-btn" data-id="${o.id}" data-name="${escapeHtml(o.name)}" data-address="${escapeHtml(o.address)}" data-lat="${o.latitude}" data-lon="${o.longitude}">Редактировать</button>
                    <button class="btn btn-sm btn-danger delete-btn" data-id="${o.id}" data-name="${escapeHtml(o.name)}">Удалить</button>
                </td>
            </tr>
        `;
    });
    html += '</tbody></table>';
    container.innerHTML = html;

    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const dataset = e.target.dataset;
            window.openEditModal({
                id: dataset.id,
                name: dataset.name,
                address: dataset.address,
                lat: dataset.lat,
                lon: dataset.lon
            });
        });
    });
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const dataset = e.target.dataset;
            window.openDeleteModal(dataset.id, dataset.name);
        });
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        window.location.href = '/login';
    }

    let currentUserRole = null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        currentUserRole = payload.role;
    } catch(e) {
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
    }

    if (currentUserRole !== 'ROLE_ADMIN') {
        showToast('Доступ запрещён', 'danger');
        window.location.href = '/map';
    }

    officeModal = new bootstrap.Modal(document.getElementById('officeModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));

    const savedCoords = sessionStorage.getItem('pickedCoordinates');
    const savedOfficeForm = sessionStorage.getItem('officeFormData');

    if (savedOfficeForm) {
        const data = JSON.parse(savedOfficeForm);
        if (data.id) document.getElementById('office-id').value = data.id;
        document.getElementById('office-name').value = data.name || '';
        document.getElementById('office-address').value = data.address || '';
        sessionStorage.removeItem('officeFormData');
    }

    if (savedCoords) {
        const coords = JSON.parse(savedCoords);
        document.getElementById('office-lat').value = coords.lat;
        document.getElementById('office-lon').value = coords.lng;
        const addressField = document.getElementById('office-address');
        if (!addressField.value.trim()) {
            addressField.value = coords.address || `${coords.lat}, ${coords.lng}`;
        }

        const coordsDiv = document.getElementById('officeCoords');
        if (coordsDiv) {
            coordsDiv.textContent = `Координаты: ${coords.lat.toFixed(6)}, ${coords.lng.toFixed(6)}`;
        }
        sessionStorage.removeItem('pickedCoordinates');
    }

    if (savedCoords || savedOfficeForm) {
        const modalTitle = document.getElementById('office-id').value ? 'Редактировать офис' : 'Добавить офис';
        document.getElementById('modalTitle').textContent = modalTitle;
        officeModal.show();
    }

    loadOffices();

    window.openEditModal = function(data) {
        document.getElementById('office-id').value = data.id;
        document.getElementById('office-name').value = data.name;
        document.getElementById('office-lat').value = data.lat;
        document.getElementById('office-lon').value = data.lon;
        document.getElementById('office-address').value = data.address;
        document.getElementById('modalTitle').textContent = 'Редактировать офис';
        officeModal.show();
    };

    window.openDeleteModal = function(id, name) {
        deleteId = id;
        document.getElementById('delete-office-name').textContent = name;
        deleteModal.show();
    };

    window.escapeHtml = function(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    };

    window.showAlert = function(message, type) {
        const container = document.getElementById('alert-container');
        container.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>`;
    };
        
    document.getElementById('logout-btn').addEventListener('click', () => {
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
    });
    
    document.getElementById('add-office-btn').addEventListener('click', () => {
        document.getElementById('office-form').reset();
        document.getElementById('office-id').value = '';
        document.getElementById('modalTitle').textContent = 'Добавить офис';
        officeModal.show();
    });

    document.getElementById('pick-office-btn').addEventListener('click', function(e) {
        e.preventDefault();
        const formData = {
            id: document.getElementById('office-id').value,
            name: document.getElementById('office-name').value,
            address: document.getElementById('office-address').value
        };
        sessionStorage.setItem('officeFormData', JSON.stringify(formData));

        let url = '/map?mode=pick-office&returnUrl=%2Fadmin%2Foffices';
        const lat = document.getElementById('office-lat').value;
        const lng = document.getElementById('office-lon').value;
        if (lat && lng) {
            url += `&lat=${lat}&lng=${lng}`;
        }
        window.location.href = url;
    });

    document.getElementById('save-office-btn').addEventListener('click', async () => {
        const id = document.getElementById('office-id').value;
        const payload = {
            name: document.getElementById('office-name').value,
            address: document.getElementById('office-address').value,  
            latitude: parseFloat(document.getElementById('office-lat').value),
            longitude: parseFloat(document.getElementById('office-lon').value)
        };
        console.log('Отправляемые данные офиса:', payload);

        const url = id ? `/api/offices/${id}` : '/api/offices';
        const method = id ? 'PUT' : 'POST';
        const response = await fetchWithAuth(url, {
            method: method,
            body: JSON.stringify(payload)
        });
        const text = await response.text();
        console.log('Ответ сервера:', response.status, text);
        let data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = { error: text || 'Неизвестная ошибка' };
        }
        if (response.ok) {
            officeModal.hide();
            loadOffices();  
            showAlert('Офис сохранён', 'success');
        } else {
            showToast('Ошибка: ' + (data.error || 'Не удалось сохранить'), 'danger');
        }
    });

    let deleteId = null;
    document.getElementById('confirm-delete-btn').addEventListener('click', async () => {
        const response = await fetchWithAuth(`/api/offices/${deleteId}`, { method: 'DELETE' });
        if (response.ok || response.status === 204) {
            deleteModal.hide();
            loadOffices();
            showAlert('Офис удалён', 'info');
        } else {
            const err = await response.json();
            showToast('Ошибка удаления: ' + (err.error || ''), 'danger');
        }
    });

    connectWebSocket();
});