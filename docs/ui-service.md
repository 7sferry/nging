# Static Frontend

Directory: `static/`

## Overview

The frontend is plain HTML, CSS, and JavaScript — no frameworks, no build tools, no Java. Nginx serves these files directly from the `static/` directory (mounted as a Docker volume). There is no separate service process.

## File Structure

```
static/
├── login.html          # Login page
├── dashboard.html      # User list + detail page
├── css/
│   └── style.css       # Shared styles
└── js/
    ├── auth.js         # Shared auth logic (token variable, refresh, authFetch)
    ├── login.js        # Login form handler
    └── dashboard.js    # Dashboard data loading
```

## Pages

### login.html

The login form.

- On page load: tries `POST /api/auth/refresh`. If a valid refresh token cookie exists, skips login and redirects to `dashboard.html`.
- On submit: sends credentials to `POST /api/auth/login`. On success, the server sets the `refresh_token` cookie and the browser redirects to `dashboard.html`. The access token from the response is **not stored** — the dashboard will get its own via `/refresh`.

### dashboard.html

The main page. Shows a table of users with account balances.

- On page load: calls `refreshAccessToken()` to get an access token from the server via the refresh cookie. If no valid session, redirects to `login.html`.
- Clicking a user row shows a detail card with ID, name, email, role, and account balance.
- Logout button: calls `POST /api/auth/logout`, clears the access token variable, redirects to `login.html`.

## JavaScript Modules

### auth.js (shared)

Loaded on both pages. Contains:

- `let accessToken = null` — the access token lives **only** in this variable. Never in localStorage, sessionStorage, or cookies.
- `refreshAccessToken()` — calls `POST /api/auth/refresh` (cookie sent automatically). On success, stores the access token in the variable.
- `authFetch(url)` — wrapper around `fetch()` that adds the `Authorization: Bearer` header. On 401, automatically calls `refreshAccessToken()` and retries once. If refresh fails, redirects to login.
- `logout()` — calls `POST /api/auth/logout`, clears the variable, redirects to login.

### login.js

Login-specific logic. Handles form submission and the auto-redirect check on page load.

### dashboard.js

Dashboard-specific logic:
- `loadUsers()` — calls `GET /api/users/` via `authFetch()`, renders the table.
- `loadUserDetail(id)` — calls `GET /api/users/{id}` via `authFetch()`, shows the detail card.
- Initialization: calls `refreshAccessToken()`, then `loadUsers()`.

## How Auth Works

The access token is never persisted. The flow on every page load:

```
1. Page loads → accessToken is null
2. auth.js calls POST /api/auth/refresh
3. Browser automatically sends refresh_token cookie
4. Server validates cookie, returns access token (from Redis cache or newly generated)
5. accessToken variable is set
6. All API calls use authFetch() which adds Authorization header
7. On 401 → auto-refresh and retry
8. Page reload → variable lost → repeat from step 1
```

## Development

Edit the files in `static/` and refresh the browser. No build step, no restart needed. Nginx serves the files directly via the volume mount.
