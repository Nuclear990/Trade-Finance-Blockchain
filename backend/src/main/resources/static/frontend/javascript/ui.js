export function showError(input, message) {
    input.textContent = message;
    input.classList.add('active');
}

export function clearErrors(form) {
    form.querySelectorAll('.error-text').forEach(e => e.classList.remove('active'));
}

export function setupPasswordToggle(passwordInput, toggle) {
    const eyeOpen = toggle.querySelector('#eyeOpen');
    const eyeClosed = toggle.querySelector('#eyeClosed');


    toggle.addEventListener('click', () => {
        const isHidden = passwordInput.type === 'password';

        passwordInput.type = isHidden ? 'text' : 'password';
        eyeOpen.style.display = isHidden ? 'none' : 'block';
        eyeClosed.style.display = isHidden ? 'block' : 'none';
    });
}

