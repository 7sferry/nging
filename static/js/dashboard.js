(async () => {
    const hasToken = await refreshAccessToken();
    if (!hasToken) {
        window.location.href = '/login.html';
        return;
    }
    document.getElementById('authStatus').textContent = 'Logged in as: ' + (username || 'unknown');
    loadUsers();
})();

async function loadUsers() {
    try {
        const res = await authFetch('/api/users/');
        if (!res) return;

        const data = await res.json();
        const tbody = document.getElementById('usersTable');
        tbody.innerHTML = '';

        data.users.forEach(user => {
            const tr = document.createElement('tr');
            tr.className = 'clickable';
            tr.onclick = () => loadUserDetail(user.id);
            tr.innerHTML = `<td>${user.id}</td><td>${user.name}</td><td>${user.email}</td><td>${user.role}</td><td>$${user.account_balance}</td>`;
            tbody.appendChild(tr);
        });
    } catch (err) {
        alert('Failed to load users: ' + err.message);
    }
}

async function loadUserDetail(id) {
    try {
        // Two parallel API calls — if token expired, both get 401,
        // but refreshPromise dedup ensures only one refresh happens
        const [userRes, contactRes] = await Promise.all([
            authFetch('/api/users/' + id),
            authFetch('/api/contacts/' + id)
        ]);

        if (!userRes || !contactRes) return;

        const userData = await userRes.json();
        const contactData = await contactRes.json();
        const user = userData.user;
        const contact = contactData.contact;

        document.getElementById('detailId').textContent = user.id;
        document.getElementById('detailName').textContent = user.name;
        document.getElementById('detailEmail').textContent = user.email;
        document.getElementById('detailRole').textContent = user.role;
        document.getElementById('detailBalance').textContent = user.account_balance;
        document.getElementById('detailPhone').textContent = contact.phone;
        document.getElementById('detailAddress').textContent = contact.address;
        document.getElementById('detailEmergency').textContent = contact.emergency;

        document.getElementById('userList').style.display = 'none';
        document.getElementById('userDetail').style.display = 'block';
    } catch (err) {
        alert('Failed to load user detail: ' + err.message);
    }
}

function showList() {
    document.getElementById('userList').style.display = 'block';
    document.getElementById('userDetail').style.display = 'none';
}
