loginForm = document.querySelector('form');

loginForm.addEventListener("submit", loginFormHandler);

async function loginFormHandler() {
    const username = document.querySelector('#username').value;
    const password = document.querySelector('#password').value;

    const response = await fetch('/login', {
        method: 'POST',
        headers: {
            'Content-type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    });

    if (response.ok) {
        if (response.userType === 'bank') {
            window.location.replace('/bank-dashboard.html');
        } else if (response.userType === 'company') {
            window.location.replace('/company-dashboard.html');
        } else {
            window.location.replace('/shipper-dashboard.html');
        }
    } else {
        username.classList.add('login-fail');
        password.classList.add('login-fail');
        document.querySelector('.login-fail-message').style.display = 'block';
        username.addEventListener('input', () => {
            username.classList.remove('login-fail');
            password.classList.remove('login-fail');
            document.querySelector('.login-fail-message').style.display = 'none';
        });
        password.addEventListener('input', () => {
            username.classList.remove('login-fail');
            password.classList.remove('login-fail');
            document.querySelector('.login-fail-message').style.display = 'none';
        });
    }
}


// Backend /login api should have: response.ok = true/false and response.userType = 'bank'/'company'/'shipper'