import { bootstrap, authFetch } from "/frontend/javascript/authState.js";

bootstrap();

    document.getElementById('test-button', {method: 'GET'})
        .addEventListener('click', async () => {
            const response = await authFetch('/secure/hello');
            const text = await response.text();
            console.log(text);
        });