import { useEffect, useState, type FormEvent } from 'react';
import { api, errorMessage, fetchAll } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import CrudPage from '../components/CrudPage';
import Modal from '../components/Modal';
import { Badge, dateFr, money } from '../components/format';
import type { Named, OrderStatus, Product } from '../types';

const STATUSES = ['DRAFT', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((s) => ({ value: s, label: s }));

interface LineForm { productId: string; quantity: number; unitPrice: string }

function NewOrderModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [customers, setCustomers] = useState<Named[]>([]);
  const [employees, setEmployees] = useState<{ id: number; firstName: string; lastName: string }[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [customerId, setCustomerId] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<LineForm[]>([{ productId: '', quantity: 1, unitPrice: '' }]);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchAll<Named>('/customers').then(setCustomers).catch(() => undefined);
    fetchAll<{ id: number; firstName: string; lastName: string }>('/employees').then(setEmployees).catch(() => undefined);
    fetchAll<Product>('/products').then(setProducts).catch(() => undefined);
  }, []);

  const setLine = (i: number, patch: Partial<LineForm>) => setLines((ls) => ls.map((l, k) => (k === i ? { ...l, ...patch } : l)));

  const total = lines.reduce((sum, l) => {
    const p = products.find((x) => String(x.id) === l.productId);
    const price = l.unitPrice !== '' ? Number(l.unitPrice) : p?.price ?? 0;
    return sum + price * (l.quantity || 0);
  }, 0);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/orders', {
        customer: { id: Number(customerId) },
        employee: employeeId ? { id: Number(employeeId) } : null,
        notes: notes || null,
        lines: lines.map((l) => ({
          product: { id: Number(l.productId) },
          quantity: Number(l.quantity),
          unitPrice: l.unitPrice !== '' ? Number(l.unitPrice) : null,
        })),
      });
      onSaved();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <Modal title="Nouvelle commande" onClose={onClose} wide>
      {error && <div className="error">{error}</div>}
      <form onSubmit={submit}>
        <div className="row">
          <div className="field"><label>Client *</label>
            <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} required>
              <option value="">—</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select></div>
          <div className="field"><label>Vendeur</label>
            <select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              <option value="">—</option>
              {employees.map((c) => <option key={c.id} value={c.id}>{c.firstName} {c.lastName}</option>)}
            </select></div>
        </div>
        <div className="field"><label>Notes</label><input value={notes} onChange={(e) => setNotes(e.target.value)} /></div>

        <h4>Lignes</h4>
        {lines.map((l, i) => (
          <div className="row" key={i} style={{ marginBottom: 8, alignItems: 'center' }}>
            <select style={{ flex: 3 }} value={l.productId} onChange={(e) => setLine(i, { productId: e.target.value })} required>
              <option value="">Produit…</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.sku} — {p.name} (stock {p.quantityInStock})</option>)}
            </select>
            <input type="number" min={1} value={l.quantity} onChange={(e) => setLine(i, { quantity: Number(e.target.value) })} required />
            <input type="number" step="0.01" placeholder="Prix (auto)" value={l.unitPrice} onChange={(e) => setLine(i, { unitPrice: e.target.value })} />
            <button type="button" className="small danger" style={{ flex: 0 }} disabled={lines.length === 1}
                    onClick={() => setLines((ls) => ls.filter((_, k) => k !== i))}>×</button>
          </div>
        ))}
        <button type="button" className="secondary small" onClick={() => setLines((ls) => [...ls, { productId: '', quantity: 1, unitPrice: '' }])}>+ Ligne</button>

        <p style={{ textAlign: 'right' }}><b>Total : {money(total)}</b></p>
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="secondary" onClick={onClose}>Annuler</button>
          <button type="submit">Créer la commande (brouillon)</button>
        </div>
      </form>
    </Modal>
  );
}

export default function OrdersPage() {
  const { can } = useAuth();
  const [showNew, setShowNew] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [msg, setMsg] = useState('');

  const act = async (fn: () => Promise<unknown>, reload: () => void) => {
    try { await fn(); setMsg(''); reload(); } catch (e) { setMsg(errorMessage(e)); }
  };

  const setStatus = (id: number, value: OrderStatus, reload: () => void) =>
    act(() => api.post(`/orders/${id}/status`, null, { params: { value } }), reload);

  return (
    <>
      {msg && <div className="error">{msg}</div>}
      <CrudPage title="Commandes" path="/orders" perm="ORDER" canCreate={false} canEdit={false} fields={[]}
        reloadKey={reloadKey}
        toolbarExtra={can('ORDER_WRITE') ? <button onClick={() => setShowNew(true)}>+ Nouvelle commande</button> : null}
        filters={[
          { param: 'status', label: 'Statut', options: STATUSES },
          { param: 'customerId', label: 'Client', optionsFrom: { path: '/customers', label: (c) => c.name } },
        ]}
        columns={[
          { header: 'N°', render: (r) => r.number, sort: 'number' },
          { header: 'Client', render: (r) => r.customer.name },
          { header: 'Date', render: (r) => dateFr(r.orderDate), sort: 'orderDate' },
          { header: 'Lignes', render: (r) => r.lines.map((l: { product: { name: string }; quantity: number }) => `${l.quantity}× ${l.product.name}`).join(', ') },
          { header: 'Total', render: (r) => money(r.totalAmount), sort: 'totalAmount' },
          { header: 'Statut', render: (r) => <Badge value={r.status} /> },
        ]}
        extraActions={(r, reload) => {
          if (!can('ORDER_WRITE')) return null;
          const s: OrderStatus = r.status;
          return (
            <>
              {s === 'DRAFT' && <button className="small" onClick={() => setStatus(r.id, 'CONFIRMED', reload)}>Confirmer</button>}
              {s === 'CONFIRMED' && <button className="small" onClick={() => setStatus(r.id, 'SHIPPED', reload)}>Expédier</button>}
              {s === 'SHIPPED' && <button className="small" onClick={() => setStatus(r.id, 'DELIVERED', reload)}>Livrer</button>}
              {['CONFIRMED', 'SHIPPED', 'DELIVERED'].includes(s) && can('INVOICE_WRITE') &&
                <button className="small secondary" onClick={() => act(() => api.post(`/orders/${r.id}/invoice`), reload)}>Facturer</button>}
              {['DRAFT', 'CONFIRMED', 'SHIPPED'].includes(s) &&
                <button className="small secondary" onClick={() => setStatus(r.id, 'CANCELLED', reload)}>Annuler</button>}
            </>
          );
        }} />
      {showNew && <NewOrderModal onClose={() => setShowNew(false)} onSaved={() => { setShowNew(false); setReloadKey((k) => k + 1); }} />}
    </>
  );
}
