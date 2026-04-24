
let accessToken = null;
let username = null;

let refreshPromise = null;

async function refreshAccessToken() {
    if (refreshPromise) return refreshPromise;

    refreshPromise = (async () => {
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'same-origin'
            });
            if (!res.ok) return false;

            const data = await res.json();
            accessToken = data.access_token;
            username = data.username;
            return true;
        } catch (e) {
            return false;
        } finally {
            refreshPromise = null;
        }
    })();

    return refreshPromise;
}

async function authFetch(url) {
    let res = await fetch(url, {
        headers: { 'Authorization': 'Bearer ' + accessToken },
        credentials: 'same-origin'
    });

    if (res.status === 401) {
        const refreshed = await refreshAccessToken();
        if (!refreshed) {
            window.location.href = '/login.html';
            return null;
        }
        res = await fetch(url, {
            headers: { 'Authorization': 'Bearer ' + accessToken },
            credentials: 'same-origin'
        });
    }

    return res;
}

async function logout() {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    accessToken = null;
    username = null;
    window.location.href = '/login.html';
}
