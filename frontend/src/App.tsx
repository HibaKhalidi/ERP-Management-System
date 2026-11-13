import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import InvoicesPage from './pages/Invoices';
import Login from './pages/Login';
import OrdersPage from './pages/Orders';
import {
  CategoriesPage, CustomersPage, DepartmentsPage, EmployeesPage, PaymentsPage, ProductsPage,
  RolesPage, StockPage, SuppliersPage, UsersPage,
} from './pages/resources';

function Protected() {
  const { user, loading } = useAuth();
  if (loading) return <p style={{ padding: 24 }}>Chargement…</p>;
  if (!user) return <Navigate to="/login" replace />;
  return <Layout />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Protected />}>
        <Route index element={<Dashboard />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="stock" element={<StockPage />} />
        <Route path="suppliers" element={<SuppliersPage />} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="invoices" element={<InvoicesPage />} />
        <Route path="payments" element={<PaymentsPage />} />
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="departments" element={<DepartmentsPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="roles" element={<RolesPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
