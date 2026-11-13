import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const LINKS: { to: string; label: string; perm: string }[] = [
  { to: '/', label: 'Tableau de bord', perm: 'DASHBOARD_READ' },
  { to: '/products', label: 'Produits', perm: 'PRODUCT_READ' },
  { to: '/categories', label: 'Catégories', perm: 'CATEGORY_READ' },
  { to: '/stock', label: 'Stock', perm: 'STOCK_READ' },
  { to: '/suppliers', label: 'Fournisseurs', perm: 'SUPPLIER_READ' },
  { to: '/customers', label: 'Clients', perm: 'CUSTOMER_READ' },
  { to: '/orders', label: 'Commandes', perm: 'ORDER_READ' },
  { to: '/invoices', label: 'Factures', perm: 'INVOICE_READ' },
  { to: '/payments', label: 'Paiements', perm: 'PAYMENT_READ' },
  { to: '/employees', label: 'Employés', perm: 'EMPLOYEE_READ' },
  { to: '/departments', label: 'Départements', perm: 'DEPARTMENT_READ' },
  { to: '/users', label: 'Utilisateurs', perm: 'USER_READ' },
  { to: '/roles', label: 'Rôles', perm: 'USER_READ' },
];

export default function Layout() {
  const { user, logout, can } = useAuth();
  return (
    <div className="layout">
      <nav className="sidebar">
        <h1>ERP 2026</h1>
        {LINKS.filter((l) => can(l.perm)).map((l) => (
          <NavLink key={l.to} to={l.to} end={l.to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
            {l.label}
          </NavLink>
        ))}
        <div className="who">
          {user?.username} ({user?.roles.join(', ')})
          <br />
          <a href="#" onClick={(e) => { e.preventDefault(); logout(); }}>Se déconnecter</a>
        </div>
      </nav>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
