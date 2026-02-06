import { authFetch } from "/frontend/javascript/authState.js";

document.addEventListener('DOMContentLoaded', () => {
    // 1. Display Username
    const username = localStorage.getItem('username') || "Shipper User";
    document.getElementById('username-display').textContent = username;

    // 2. Issue BL Handler
    document.getElementById('btnIssueBl').addEventListener('click', async () => {
        const txnId = document.getElementById('issueBlTxnId').value;
        if (!txnId) return alert("Please enter Transaction ID");

        try {
            const res = await authFetch(`/shipper/issueBl?trxnId=${txnId}`, { method: 'POST' });
            if (res.ok) {
                alert("BL Issued successfully!");
                document.getElementById('issueBlTxnId').value = '';
            } else {
                alert("Failed to issue BL: " + await res.text());
            }
        } catch (e) {
            console.error(e);
            alert("Error issuing BL");
        }
    });

    // 3. View Logs Handler - Redirect
    document.getElementById('btnViewLogs').addEventListener('click', () => {
        window.location.href = '/frontend/html/logDashboard.html';
    });
});
