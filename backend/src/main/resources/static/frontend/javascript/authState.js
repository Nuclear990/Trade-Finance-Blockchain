// authState.js
export const authState = {
    accessToken: null
};

export async function authFetch(url, options = {}) {
    const headers = {
        ...(options.headers || {}),
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
    };

    const response = await fetch(url, {
        ...options,
        headers,
        credentials: 'include'
    });

    // access token expired
    if (response.status === 401 && response.headers.get("Token-Expired") === 'true') {
        const refreshed = await refreshAccessToken();
        if (!refreshed) {
            window.location.replace('/frontend/html/index.html');
            return;
        }

        // retry original request
        return authFetch(url, options);
    }

    return response;
}

async function refreshAccessToken() {
    const response = await fetch('/public/refresh', {
        method: 'POST',
        credentials: 'include'
    });

    if (!response.ok) return false;

    const data = await response.json();
    accessToken = data.accessToken;
    return true;
}
