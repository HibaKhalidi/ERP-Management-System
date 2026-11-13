import CrudPage, { type FieldDef, type Option } from '../components/CrudPage';
import { Badge, dateFr, money } from '../components/format';

const name = (o: { name: string }) => o.name;

export function CategoriesPage() {
  return <CrudPage title="Catégories" path="/categories" perm="CATEGORY"
    columns={[{ header: 'Nom', render: (r) => r.name, sort: 'name' }, { header: 'Description', render: (r) => r.description }]}
    fields={[{ name: 'name', label: 'Nom', type: 'text', required: true }, { name: 'description', label: 'Description', type: 'textarea' }]} />;
}

export function DepartmentsPage() {
  return <CrudPage title="Départements" path="/departments" perm="DEPARTMENT"
    columns={[{ header: 'Nom', render: (r) => r.name, sort: 'name' }, { header: 'Description', render: (r) => r.description }]}
    fields={[{ name: 'name', label: 'Nom', type: 'text', required: true }, { name: 'description', label: 'Description', type: 'textarea' }]} />;
}

const contactFields: FieldDef[] = [
  { name: 'name', label: 'Nom', type: 'text', required: true },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Téléphone', type: 'text' },
  { name: 'address', label: 'Adresse', type: 'text' },
  { name: 'city', label: 'Ville', type: 'text' },
];

export function SuppliersPage() {
  return <CrudPage title="Fournisseurs" path="/suppliers" perm="SUPPLIER"
    columns={[{ header: 'Nom', render: (r) => r.name, sort: 'name' }, { header: 'Contact', render: (r) => r.contactName },
      { header: 'Email', render: (r) => r.email }, { header: 'Téléphone', render: (r) => r.phone }, { header: 'Ville', render: (r) => r.city, sort: 'city' }]}
    fields={[contactFields[0], { name: 'contactName', label: 'Personne de contact', type: 'text' }, ...contactFields.slice(1)]} />;
}

export function CustomersPage() {
  return <CrudPage title="Clients" path="/customers" perm="CUSTOMER"
    columns={[{ header: 'Nom', render: (r) => r.name, sort: 'name' }, { header: 'Email', render: (r) => r.email },
      { header: 'Téléphone', render: (r) => r.phone }, { header: 'Ville', render: (r) => r.city, sort: 'city' }]}
    fields={contactFields} />;
}

export function ProductsPage() {
  return <CrudPage title="Produits" path="/products" perm="PRODUCT"
    filters={[
      { param: 'categoryId', label: 'Catégorie', optionsFrom: { path: '/categories', label: name } },
      { param: 'supplierId', label: 'Fournisseur', optionsFrom: { path: '/suppliers', label: name } },
      { param: 'lowStock', label: 'Stock', options: [{ value: 'true', label: 'Stock bas' }] },
    ]}
    columns={[
      { header: 'SKU', render: (r) => r.sku, sort: 'sku' },
      { header: 'Nom', render: (r) => r.name, sort: 'name' },
      { header: 'Catégorie', render: (r) => r.category?.name },
      { header: 'Fournisseur', render: (r) => r.supplier?.name },
      { header: 'Prix', render: (r) => money(r.price), sort: 'price' },
      { header: 'Stock', sort: 'quantityInStock', render: (r) => (
        <span className={`badge ${r.quantityInStock <= r.reorderLevel ? 'bad' : 'ok'}`}>{r.quantityInStock}</span>) },
      { header: 'Actif', render: (r) => (r.active ? 'Oui' : 'Non') },
    ]}
    defaults={{ active: true, reorderLevel: 0 }}
    fields={[
      { name: 'sku', label: 'SKU', type: 'text', required: true },
      { name: 'name', label: 'Nom', type: 'text', required: true },
      { name: 'description', label: 'Description', type: 'textarea' },
      { name: 'price', label: 'Prix de vente', type: 'number', step: '0.01', required: true },
      { name: 'costPrice', label: "Prix d'achat", type: 'number', step: '0.01' },
      { name: 'quantityInStock', label: 'Stock initial', type: 'number', createOnly: true },
      { name: 'reorderLevel', label: 'Seuil de réapprovisionnement', type: 'number' },
      { name: 'category', label: 'Catégorie', type: 'select', optionsFrom: { path: '/categories', label: name } },
      { name: 'supplier', label: 'Fournisseur', type: 'select', optionsFrom: { path: '/suppliers', label: name } },
      { name: 'active', label: 'Actif', type: 'checkbox' },
    ]} />;
}

const MOVEMENT_TYPES: Option[] = [
  { value: 'IN', label: 'Entrée' }, { value: 'OUT', label: 'Sortie' }, { value: 'ADJUSTMENT', label: 'Ajustement (+/-)' },
];

export function StockPage() {
  return <CrudPage title="Mouvements de stock" path="/stock" perm="STOCK" canEdit={false} canDelete={false}
    filters={[
      { param: 'productId', label: 'Produit', optionsFrom: { path: '/products', label: (p) => `${p.sku} — ${p.name}` } },
      { param: 'type', label: 'Type', options: MOVEMENT_TYPES },
    ]}
    columns={[
      { header: 'Date', render: (r) => new Date(r.createdAt).toLocaleString('fr-FR') },
      { header: 'Produit', render: (r) => `${r.product.sku} — ${r.product.name}` },
      { header: 'Type', render: (r) => <Badge value={r.type} /> },
      { header: 'Quantité', render: (r) => r.quantity },
      { header: 'Motif', render: (r) => r.reason },
      { header: 'Par', render: (r) => r.createdBy },
    ]}
    fields={[
      { name: 'product', label: 'Produit', type: 'select', required: true, optionsFrom: { path: '/products', label: (p) => `${p.sku} — ${p.name} (stock ${p.quantityInStock})` } },
      { name: 'type', label: 'Type', type: 'select', required: true, options: MOVEMENT_TYPES },
      { name: 'quantity', label: 'Quantité (négative possible pour un ajustement)', type: 'number', required: true },
      { name: 'reason', label: 'Motif', type: 'text' },
    ]} />;
}

export function EmployeesPage() {
  return <CrudPage title="Employés" path="/employees" perm="EMPLOYEE"
    filters={[{ param: 'departmentId', label: 'Département', optionsFrom: { path: '/departments', label: name } }]}
    columns={[
      { header: 'Nom', render: (r) => `${r.firstName} ${r.lastName}`, sort: 'lastName' },
      { header: 'Poste', render: (r) => r.position },
      { header: 'Département', render: (r) => r.department?.name },
      { header: 'Manager', render: (r) => (r.manager ? `${r.manager.firstName} ${r.manager.lastName}` : '') },
      { header: 'Email', render: (r) => r.email },
      { header: 'Embauche', render: (r) => dateFr(r.hireDate), sort: 'hireDate' },
    ]}
    fields={[
      { name: 'firstName', label: 'Prénom', type: 'text', required: true },
      { name: 'lastName', label: 'Nom', type: 'text', required: true },
      { name: 'email', label: 'Email', type: 'email' },
      { name: 'phone', label: 'Téléphone', type: 'text' },
      { name: 'position', label: 'Poste', type: 'text' },
      { name: 'salary', label: 'Salaire', type: 'number', step: '0.01' },
      { name: 'hireDate', label: "Date d'embauche", type: 'date' },
      { name: 'department', label: 'Département', type: 'select', optionsFrom: { path: '/departments', label: name } },
      { name: 'manager', label: 'Manager', type: 'select', optionsFrom: { path: '/employees', label: (e) => `${e.firstName} ${e.lastName}` } },
      { name: 'user', label: 'Compte utilisateur', type: 'select', optionsFrom: { path: '/users', label: (u) => u.username } },
    ]} />;
}

export function UsersPage() {
  return <CrudPage title="Utilisateurs" path="/users" perm="USER"
    columns={[
      { header: 'Identifiant', render: (r) => r.username, sort: 'username' },
      { header: 'Email', render: (r) => r.email },
      { header: 'Rôles', render: (r) => (r.roles ?? []).map((x: { name: string }) => x.name).join(', ') },
      { header: 'Actif', render: (r) => (r.enabled ? 'Oui' : 'Non') },
    ]}
    defaults={{ enabled: true }}
    fields={[
      { name: 'username', label: 'Identifiant', type: 'text', required: true },
      { name: 'email', label: 'Email', type: 'email' },
      { name: 'password', label: 'Mot de passe', type: 'password', required: true },
      { name: 'enabled', label: 'Actif', type: 'checkbox' },
      { name: 'roles', label: 'Rôles (Ctrl/Cmd + clic pour plusieurs)', type: 'multiselect', optionsFrom: { path: '/roles', label: name } },
    ]} />;
}

export function RolesPage() {
  return <CrudPage title="Rôles et permissions" path="/roles" perm="USER"
    columns={[
      { header: 'Rôle', render: (r) => r.name, sort: 'name' },
      { header: 'Description', render: (r) => r.description },
      { header: 'Permissions', render: (r) => <span className="muted">{(r.permissions ?? []).length} permission(s)</span> },
    ]}
    fields={[
      { name: 'name', label: 'Nom (ex. ACCOUNTANT)', type: 'text', required: true },
      { name: 'description', label: 'Description', type: 'text' },
      { name: 'permissions', label: 'Permissions (Ctrl/Cmd + clic pour plusieurs)', type: 'multiselect', optionsFrom: { path: '/permissions', label: name } },
    ]} />;
}

const PAYMENT_METHODS: Option[] = [
  { value: 'CASH', label: 'Espèces' }, { value: 'CARD', label: 'Carte' },
  { value: 'BANK_TRANSFER', label: 'Virement' }, { value: 'CHECK', label: 'Chèque' },
];

export function PaymentsPage() {
  return <CrudPage title="Paiements" path="/payments" perm="PAYMENT" canEdit={false}
    filters={[{ param: 'method', label: 'Mode', options: PAYMENT_METHODS }]}
    columns={[
      { header: 'Date', render: (r) => new Date(r.paidAt).toLocaleString('fr-FR') },
      { header: 'Facture', render: (r) => r.invoice.number },
      { header: 'Montant', render: (r) => money(r.amount) },
      { header: 'Mode', render: (r) => r.method },
      { header: 'Référence', render: (r) => r.reference },
    ]}
    fields={[
      { name: 'invoice', label: 'Facture', type: 'select', required: true,
        optionsFrom: { path: '/invoices', label: (i) => `${i.number} — reste ${money(i.balance)}` } },
      { name: 'amount', label: 'Montant', type: 'number', step: '0.01', required: true },
      { name: 'method', label: 'Mode de paiement', type: 'select', required: true, options: PAYMENT_METHODS },
      { name: 'reference', label: 'Référence', type: 'text' },
    ]} />;
}
