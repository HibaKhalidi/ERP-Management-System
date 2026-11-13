import { useState, type FormEvent } from 'react';
import { api, errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import CrudPage from '../components/CrudPage';
import Modal from '../components/Modal';
import { Badge, dateFr, money } from '../components/format';
import type { Invoice } from '../types';

const STATUSES = ['UNPAID', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'].map((s) => ({ value: s, label: s }));

function PayModal({ invoice, onClose, onSaved }: { invoice: Invoice; onClose: () => void; onSaved: () => void }) {
  const [amount, setAmount] = useState(String(invoice.balance));
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [reference, setReference] = useState('');
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/payments', { invoice: { id: invoice.id }, amount: Number(amount), method, reference: reference || null });
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <Modal title={`Paiement — ${invoice.number}`} onClose={onClose}>
      <p>Reste à payer : <b>{money(invoice.balance)}</b></p>
      {error && <div className="error">{error}</div>}
      <form onSubmit={submit}>
        <div className="field"><label>Montant</label><input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required /></div>
        <div className="field"><label>Mode</label>
          <select value={method} onChange={(e) => setMethod(e.target.value)}>
            <option value="CASH">Espèces</option><option value="CARD">Carte</option>
            <option value="BANK_TRANSFER">Virement</option><option value="CHECK">Chèque</option>
          </select></div>
        <div className="field"><label>Référence</label><input value={reference} onChange={(e) => setReference(e.target.value)} /></div>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="secondary" onClick={onClose}>Annuler</button>
          <button type="submit">Enregistrer le paiement</button>
        </div>
      </form>
    </Modal>
  );
}

export default function InvoicesPage() {
  const { can } = useAuth();
  const [payFor, setPayFor] = useState<Invoice | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [msg, setMsg] = useState('');
  const today = new Date().toISOString().slice(0, 10);

  return (
    <>
      {msg && <div className="error">{msg}</div>}
      <CrudPage title="Factures" path="/invoices" perm="INVOICE" canCreate={false} canEdit={false} fields={[]} reloadKey={reloadKey}
        filters={[
          { param: 'status', label: 'Statut', options: STATUSES },
          { param: 'overdue', label: 'Échéance', options: [{ value: 'true', label: 'En retard' }] },
        ]}
        columns={[
          { header: 'N°', render: (r) => r.number, sort: 'number' },
          { header: 'Commande', render: (r) => r.order.number },
          { header: 'Client', render: (r) => r.order.customer.name },
          { header: 'Émission', render: (r) => dateFr(r.issueDate) },
          { header: 'Échéance', sort: 'dueDate', render: (r) => (
            <span style={{ color: r.dueDate < today && ['UNPAID', 'PARTIALLY_PAID'].includes(r.status) ? 'var(--danger)' : undefined }}>{dateFr(r.dueDate)}</span>) },
          { header: 'Total', render: (r) => money(r.totalAmount), sort: 'totalAmount' },
          { header: 'Payé', render: (r) => money(r.paidAmount) },
          { header: 'Reste', render: (r) => money(r.balance) },
          { header: 'Statut', render: (r) => <Badge value={r.status} /> },
        ]}
        extraActions={(r, reload) => {
          if (!can('INVOICE_WRITE')) return null;
          return (
            <>
              {can('PAYMENT_WRITE') && r.status !== 'PAID' && r.status !== 'CANCELLED' &&
                <button className="small" onClick={() => setPayFor(r)}>Payer</button>}
              {r.status === 'UNPAID' &&
                <button className="small secondary" onClick={async () => {
                  try { await api.post(`/invoices/${r.id}/cancel`); setMsg(''); reload(); } catch (e) { setMsg(errorMessage(e)); }
                }}>Annuler</button>}
            </>
          );
        }} />
      {payFor && <PayModal invoice={payFor} onClose={() => setPayFor(null)} onSaved={() => { setPayFor(null); setReloadKey((k) => k + 1); }} />}
    </>
  );
}
