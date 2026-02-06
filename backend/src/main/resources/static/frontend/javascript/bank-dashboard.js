import { authFetch } from "/frontend/javascript/authState.js";

document.addEventListener('DOMContentLoaded', () => {
    // 1. Display Username
    const username = localStorage.getItem('username') || "Bank User";
    document.getElementById('username-display').textContent = username;

    // 2. Issue LC Handler
    document.getElementById('btnIssueLc').addEventListener('click', async () => {
        const txnId = document.getElementById('issueLcTxnId').value;
        if (!txnId) return alert("Please enter Transaction ID");

        try {
            const res = await authFetch(`/bank/issueLc?trxnId=${txnId}`, { method: 'POST' });
            if (res.ok) {
                alert("LC Issued successfully!");
                document.getElementById('issueLcTxnId').value = '';
            } else {
                alert("Failed to issue LC: " + await res.text());
            }
        } catch (e) {
            console.error(e);
            alert("Error issuing LC");
        }
    });

    // 3. Settle Handler
    document.getElementById('btnSettle').addEventListener('click', async () => {
        const txnId = document.getElementById('settleTxnId').value;
        if (!txnId) return alert("Please enter Transaction ID");

        try {
            const res = await authFetch(`/bank/settle?trxnId=${txnId}`, { method: 'POST' });
            if (res.ok) {
                alert("Transaction Settled successfully!");
                document.getElementById('settleTxnId').value = '';
            } else {
                alert("Failed to settle: " + await res.text());
            }
        } catch (e) {
            console.error(e);
            alert("Error settling transaction");
        }
    });

    // 4. View Logs Handler - Redirect
    document.getElementById('btnViewLogs').addEventListener('click', () => {
        window.location.href = '/frontend/html/logDashboard.html';
    });
});
