// authState.js
export const authState = {
    accessToken: null
};

let refreshPromise = null;

export async function authFetch(url, options = {}) {
    const headers = { ...(options.headers || {}) };

    if (authState.accessToken) {
    console.log(authState.accessToken);

        headers.Authorization = `Bearer ${authState.accessToken}`;
    }

    const response = await fetch(url, {
        ...options,
        headers,
        credentials: 'include'
    });

    // 🔒 Forbidden → hard failure, no refresh
    if (response.status === 403) {
        authState.accessToken = null;
        window.location.replace('/frontend/html/index.html');
        return;
    }

    if (response.status !== 401) {
        return response;
    }

    // Single refresh lock
    if (refreshPromise === null) {
        refreshPromise = refreshAccessToken().finally(() => {
            refreshPromise = null;
        });
    }

    const refreshed = await refreshPromise;
    if (!refreshed) {
        window.location.replace('/frontend/html/index.html');
        return;
    }

    // 🔄 Retry original request exactly once
    return authFetch(url, options);
}

async function refreshAccessToken() {
console.log("Refreshing..\n")
    const response = await fetch('/public/refresh', {
        method: 'POST',
        credentials: 'include'
    });

    if (!response.ok) return false;

    const data = await response.json();
    authState.accessToken = data.accessToken;
    return true;
}
