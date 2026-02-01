import { showError, clearErrors } from "/frontend/javascript/ui.js";
import { authFetch } from "/frontend/javascript/authState.js";

const form = document.querySelector('form');
const username = document.querySelector('#username');
const password = document.querySelector('#password');
const confirm = document.querySelector('#confirmPassword');
const role = document.querySelector('#userType');
const invalidUsername = document.querySelector('#username-invalid');
const invalidPassword = document.querySelector('#password-invalid');
const invalidConfirm = document.querySelector('#confirmPassword-invalid');

document.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', (event) => {
        clearErrors(event.target.parentElement);
    });
});

form.addEventListener('submit', async e => {
    e.preventDefault();
    let invalid = false;

    if (username.value.length < 3 || username.value.length > 50) {
        showError(invalidUsername, 'Username must be 3 to 50 characters long');
        invalid = true;
    }

    if (password.value.length < 8) {
        showError(invalidPassword, 'Password should be at least 8 characters long');
        invalid = true;
    }

    if (password.value !== confirm.value) {
        showError(invalidConfirm, 'Must be same as Password');
        invalid = true;
    }


    if (invalid) return;

    const res = await fetch('/public/createUser', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: username.value,
            password: password.value,
            userType: role.value
        })
    });

    if (res.status === 201) {
        alert('Account created successfully!\nYou will now be redirected to login page');
        window.location.replace('index.html');
    } else if (res.status === 409) {
        showError(invalidUsername, 'Username already exists');
    } else {
        alert('Account creation failed');
    }
});
