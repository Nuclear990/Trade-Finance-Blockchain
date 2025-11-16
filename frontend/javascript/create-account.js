const createForm = document.querySelector('form');

createForm.addEventListener("submit", createFormHandler);

// attach once (NOT inside submit)
const confirmPassword = document.querySelector('#confirmPassword');
confirmPassword.addEventListener('input', () => {
    document.querySelector('.password-mismatch-message').style.display = 'none';
});

async function createFormHandler(event) {
    event.preventDefault(); // STOP page reload

    const role = document.querySelector('#role').value;
    const name = document.querySelector('#name').value;
    const password = document.querySelector('#password').value;
    const confirmPasswordVal = confirmPassword.value;

    // password mismatch
    if (password !== confirmPasswordVal) {
        document.querySelector('.password-mismatch-message').style.display = 'block';
        confirmPassword.focus();
        return;   // Stop here
    }

    // send data to backend
    const response = await fetch('/api/users/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role, username: name, password })
    });

    if (response.ok) {
        const data = await response.json();
        alert(`Account created successfully as ${role}.\nYour Username is ${data.username}`);
        window.location.replace('index.html')
    } else {
        alert('Failed to create account.');
    }
}

//backend /api/users/create should return response.ok=true/false and response.username on success
//change where to redirect according to backend