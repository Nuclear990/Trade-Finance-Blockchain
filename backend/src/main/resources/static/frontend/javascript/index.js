import { authFetch } from "/frontend/javascript/authState.js";
import { showError, clearErrors, setupPasswordToggle } from "/frontend/javascript/ui.js";

const form = document.querySelector('form');
const username = document.querySelector('#username');
const password = document.querySelector('#password');

setupPasswordToggle(
    password,
    document.getElementById('togglePassword')
);

document.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', (event) => {
        clearErrors(event.target.parentElement);
    });
});

form.addEventListener('submit', async e => {
    e.preventDefault();

    const res = await fetch('/public/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            "username": username.value,
            "password": password.value
        })
    });
    let invalidElement = document.querySelector("#invalidCredentials");
    if (!res.ok) {
        showError(invalidElement, 'Invalid credentials');
        return;
    }

    const data = await res.json();
    localStorage.setItem('username', data.username);

    const map = {
        BANK: 'bank-dashboard.html',
        COMPANY: 'company-dashboard.html',
        SHIPPER: 'shipper-dashboard.html'
    };

    window.location.replace(`/frontend/html/${map[data.userType]}`);
});
