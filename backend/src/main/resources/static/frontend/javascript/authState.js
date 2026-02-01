export const authState = {
    accessToken: null
};

let refreshPromise = null;

/**
 * Centralized authenticated fetch
 */
export async function authFetch(url, options = {}) {
    const {
        headers: customHeaders = {},
        _retry = false,
        ...rest
    } = options;

    // Build headers fresh every time
    const headers = { ...customHeaders };

    if (authState.accessToken) {
        headers.Authorization = `Bearer ${authState.accessToken}`;
    }

    const response = await fetch(url, {
        ...rest,
        headers,
        credentials: 'include'
    });

    /* ---------------- HARD FAIL ---------------- */
    if (response.status === 403) {
        // 403 == forbidden, NOT refreshable
        forceLogout();
        return;
    }

    /* ---------------- SUCCESS ---------------- */
    if (response.status !== 401) {
        return response;
    }

    /* ---------------- ALREADY RETRIED ---------------- */
    if (_retry) {
        forceLogout();
        return;
    }

    /* ---------------- REFRESH LOCK ---------------- */
    if (!refreshPromise) {
        refreshPromise = refreshAccessToken()
            .finally(() => {
                refreshPromise = null;
            });
    }

    const refreshed = await refreshPromise;

    if (!refreshed) {
        forceLogout();
        return;
    }

    /* ---------------- RETRY ONCE ---------------- */
    return authFetch(url, {
        ...options,
        _retry: true
    });
}

/**
 * Refresh access token using HttpOnly cookie
 */
export async function refreshAccessToken() {
    try {
        const response = await fetch('/public/refresh', {
            method: 'POST',
            credentials: 'include'
        });

        if (!response.ok) return false;

        const data = await response.json();

        if (!data?.accessToken) return false;

        authState.accessToken = data.accessToken;
        return true;
    } catch {
        return false;
    }
}

/**
 * Central logout handler
 */
function forceLogout() {
    authState.accessToken = null;
    window.location.replace('/frontend/html/index.html');
}

export async function bootstrap() {
           const ok = await refreshAccessToken();

           if (!ok) {
               window.location.replace("/frontend/html/index.html");
               return;
           }
       }

