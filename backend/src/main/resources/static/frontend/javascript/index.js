import { accessToken } from "./authState";

loginForm = document.querySelector('form');

loginForm.addEventListener("submit", loginFormHandler);

const usernameE = document.querySelector('#username');
const passwordE = document.querySelector('#password');

usernameE.addEventListener('input', retry);
passwordE.addEventListener('input', retry);

function retry() {
    usernameE.classList.remove('login-fail');
    passwordE.classList.remove('login-fail');
    document.querySelector('.login-fail-message').style.display = 'none';
}
async function loginFormHandler(event) {
    event.preventDefault();
    const username = usernameE.value;
    const password = passwordE.value;

    const response = await fetch('/public/login', {
        method: 'POST',
        headers: {
            'Content-type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ username, password })
    });

    if (response.ok) {
        const data = await response.json();
       authState.accessToken = data.accessToken;
        if (data.userType === 'bank') {
            window.location.replace('/frontend/html/bank-dashboard.html');
        } else if (data.userType === 'company') {
            window.location.replace('/frontend/html/company-dashboard.html');
        } else {
            window.location.replace('/frontend/html/shipper-dashboard.html');
            //window.location.replace(url) redirects to a new page without adding the current page to the browser history.
            //window.location.href = url does the same thing but adds the current page to the browser history. ser can press Back → goes back to login
        }
    } else {
        usernameE.classList.add('login-fail');
        passwordE.classList.add('login-fail');
        document.querySelector('.login-fail-message').style.display = 'block';
    }
}


// Backend /login api should have: response.ok = true/false and response.userType = 'bank'/'company'/'shipper'
//change where to redirect according to backend