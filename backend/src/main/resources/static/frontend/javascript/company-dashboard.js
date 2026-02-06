import { authFetch } from "/frontend/javascript/authState.js";

document.addEventListener('DOMContentLoaded', () => {
    // 1. Display Username
    const username = localStorage.getItem('username') || "Company User";
    document.getElementById('username-display').textContent = username;

    // 2. Request LC Handler
    document.getElementById('btnRequestLc').addEventListener('click', async () => {
        const dto = {
            company: document.getElementById('lcCompany').value,
            shipper: document.getElementById('lcShipper').value,
            importerBank: document.getElementById('lcImporterBank').value,
            exporterBank: document.getElementById('lcExporterBank').value,
            amount: parseInt(document.getElementById('lcAmount').value),
            // Corrected to Number for BigInteger backend compatibility
            goods: parseInt(document.getElementById('lcGoods').value)
        };

        if (isNaN(dto.amount) || isNaN(dto.goods)) {
            return alert("Amount and Goods Quantity must be valid numbers");
        }

        try {
            const res = await authFetch('/company/requestLc', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(dto)
            });

            if (res.ok) {
                const trxnId = await res.json();
                alert(`LC Requested successfully! Transaction ID: ${trxnId}`);
                document.getElementById('lcForm').reset();
            } else {
                alert("Failed to request LC: " + await res.text());
            }
        } catch (e) {
            console.error(e);
            alert("Error requesting LC");
        }
    });

    // 3. Request BL Handler
    document.getElementById('btnRequestBl').addEventListener('click', async () => {
        const trxnId = document.getElementById('blTrxnId').value;
        if (!trxnId) return alert("Please enter Transaction ID");

        try {
            const res = await authFetch(`/company/requestBl?trxnId=${trxnId}`, { method: 'POST' });
            if (res.ok) {
                alert("BL Requested successfully!");
                document.getElementById('blTrxnId').value = '';
            } else {
                alert("Failed to request BL: " + await res.text());
            }
        } catch (e) {
            console.error(e);
            alert("Error requesting BL");
        }
    });

    // 4. View Logs Handler - Redirect
    document.getElementById('btnViewLogs').addEventListener('click', () => {
        window.location.href = '/frontend/html/logDashboard.html';
    });
});