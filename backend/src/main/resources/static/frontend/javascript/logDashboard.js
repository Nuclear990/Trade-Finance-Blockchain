import { authFetch } from "/frontend/javascript/authState.js";

document.addEventListener('DOMContentLoaded', () => {
    const username = localStorage.getItem('username') || "User";
    document.getElementById('username-display').textContent = username;
    fetchLogs();
});

async function fetchLogs() {
    const tbody = document.getElementById('logsBody');
    try {
        const res = await authFetch('/logs');
        if (res.ok) {
            const logs = await res.json();
            tbody.innerHTML = '';
            if (logs.length === 0) {
                tbody.innerHTML = '<tr><td colspan="11">No logs found</td></tr>';
                return;
            }
            logs.forEach(log => {
                const tr = document.createElement('tr');
                // Status fallback
                const status = log.status || 'ACTIVE';

                tr.innerHTML = `
                    <td>${log.trxnId}</td>
                    <td><span class="status-badge status-${status}">${status}</span></td>
                    <td><span class="status-badge status-${log.lcToken || ''}">${log.lcToken || '-'}</span></td>
                    <td><span class="status-badge status-${log.blToken || ''}">${log.blToken || '-'}</span></td>
                    <td>${log.importer || '-'}</td>
                    <td>${log.exporter || '-'}</td>
                    <td>${log.importerBank || '-'}</td>
                    <td>${log.exporterBank || '-'}</td>
                    <td>${log.shipper || '-'}</td>
                    <td>${log.amount}</td>
                    <td>${log.goods}</td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="11">Failed to load logs</td></tr>';
        }
    } catch (e) {
        console.error(e);
        tbody.innerHTML = '<tr><td colspan="11">Error loading logs</td></tr>';
    }
}
