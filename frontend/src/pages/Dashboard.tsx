import { useEffect, useState } from 'react';
import { api, errorMessage } from '../api/client';
import { Badge, money, dateFr } from '../components/format';
import type { Dashboard as DashboardData } from '../types';

function Stat({ label, value }: { label: string; value: string | number }) {
  return <div className="card stat"><div className="v">{value}</div><div className="l">{label}</div></div>;
}

export default function Dashboard() {
  const [d, setD] = useState<DashboardData | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get<DashboardData>('/dashboard').then((r) => setD(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <div className="error">{error}</div>;
  if (!d) return <p>Chargement…</p>;

  const max = Math.max(...d.salesByMonth.map((m) => Number(m.total)), 1);

  return (
    <div>
      <h2>Tableau de bord</h2>
      <div className="cards">
        <Stat label="Chiffre d'affaires (commandes)" value={money(d.totalSales)} />
        <Stat label="Facturé" value={money(d.totalInvoiced)} />
        <Stat label="Encaissé" value={money(d.totalPaid)} />
        <Stat label="Reste à encaisser" value={money(d.totalOutstanding)} />
        <Stat label="Factures en retard" value={d.overdueInvoices} />
        <Stat label="Produits" value={d.products} />
        <Stat label="Produits en stock bas" value={d.lowStockCount} />
        <Stat label="Clients" value={d.customers} />
        <Stat label="Fournisseurs" value={d.suppliers} />
        <Stat label="Employés" value={d.employees} />
      </div>

      <div className="cards" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(320px,1fr))' }}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Ventes — 6 derniers mois</h3>
          <div className="bars">
            {d.salesByMonth.map((m) => (
              <div className="bar" key={m.month} title={money(Number(m.total))}>
                <div style={{ height: `${(Number(m.total) / max) * 100}%` }} />
                {m.month.slice(5)}
              </div>
            ))}
          </div>
        </div>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Commandes par statut</h3>
          {Object.entries(d.ordersByStatus).map(([s, n]) => (
            <p key={s} style={{ display: 'flex', justifyContent: 'space-between', margin: '6px 0' }}><Badge value={s} /> <b>{n}</b></p>
          ))}
        </div>
      </div>

      <div className="cards" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(320px,1fr))' }}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Dernières commandes</h3>
          <table><tbody>
            {d.recentOrders.map((o) => (
              <tr key={o.id}><td>{o.number}</td><td>{o.customer.name}</td><td>{dateFr(o.orderDate)}</td><td>{money(o.totalAmount)}</td><td><Badge value={o.status} /></td></tr>
            ))}
            {d.recentOrders.length === 0 && <tr><td className="muted">Aucune commande</td></tr>}
          </tbody></table>
        </div>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Stock bas</h3>
          <table><tbody>
            {d.lowStockProducts.map((p) => (
              <tr key={p.id}><td>{p.sku}</td><td>{p.name}</td><td><b>{p.quantityInStock}</b> / seuil {p.reorderLevel}</td></tr>
            ))}
            {d.lowStockProducts.length === 0 && <tr><td className="muted">Rien à signaler</td></tr>}
          </tbody></table>
        </div>
      </div>
    </div>
  );
}
