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
    alert('Доступ запрещён');
    window.location.href = '/map';
}

let officeModal, deleteModal;
document.addEventListener('DOMContentLoaded', () => {
    officeModal = new bootstrap.Modal(document.getElementById('officeModal'));
    deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    loadOffices();
});

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('jwt_token');
    window.location.href = '/login';
});

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
        btn.addEventListener('click', () => openEditModal(btn.dataset));
    });
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', () => openDeleteModal(btn.dataset.id, btn.dataset.name));
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.getElementById('add-office-btn').addEventListener('click', () => {
    document.getElementById('office-form').reset();
    document.getElementById('office-id').value = '';
    document.getElementById('modalTitle').textContent = 'Добавить офис';
    officeModal.show();
});

function openEditModal(data) {
    document.getElementById('office-id').value = data.id;
    document.getElementById('office-name').value = data.name;
    document.getElementById('office-address').value = data.address;
    document.getElementById('office-lat').value = data.lat;
    document.getElementById('office-lon').value = data.lon;
    document.getElementById('modalTitle').textContent = 'Редактировать офис';
    officeModal.show();
}

document.getElementById('save-office-btn').addEventListener('click', async () => {
    const id = document.getElementById('office-id').value;
    const payload = {
        name: document.getElementById('office-name').value,
        address: document.getElementById('office-address').value,
        latitude: parseFloat(document.getElementById('office-lat').value),
        longitude: parseFloat(document.getElementById('office-lon').value)
    };
    const url = id ? `/api/offices/${id}` : '/api/offices';
    const method = id ? 'PUT' : 'POST';
    const response = await fetchWithAuth(url, {
        method: method,
        body: JSON.stringify(payload)
    });
    if (response.ok) {
        officeModal.hide();
        loadOffices();
        showAlert('Офис сохранён', 'success');
    } else {
        const err = await response.json();
        alert('Ошибка: ' + (err.error || 'Не удалось сохранить'));
    }
});

let deleteId = null;
function openDeleteModal(id, name) {
    deleteId = id;
    document.getElementById('delete-office-name').textContent = name;
    deleteModal.show();
}
document.getElementById('confirm-delete-btn').addEventListener('click', async () => {
    const response = await fetchWithAuth(`/api/offices/${deleteId}`, { method: 'DELETE' });
    if (response.ok || response.status === 204) {
        deleteModal.hide();
        loadOffices();
        showAlert('Офис удалён', 'info');
    } else {
        const err = await response.json();
        alert('Ошибка удаления: ' + (err.error || ''));
    }
});

function showAlert(message, type) {
    const container = document.getElementById('alert-container');
    container.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>`;
}