# ERP / Gestion d'entreprise 2026

Application de gestion d'entreprise : **utilisateurs, employés, produits, fournisseurs, stock, commandes, factures, paiements et tableau de bord**.

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Bean Validation, springdoc-openapi (Swagger) |
| Base de données | MySQL 8.4 (H2 pour les tests) |
| Frontend | React 18, TypeScript, Vite, React Router, Axios |
| DevOps | Docker, docker-compose, GitHub Actions (CI) |

## Démarrage rapide (Docker)

```bash
docker compose up --build
```

- Application : http://localhost:3000
- API : http://localhost:8080/api
- Swagger UI : http://localhost:8080/swagger-ui.html

Pour changer les secrets : `cp .env.example .env` puis éditez `DB_PASSWORD` et `JWT_SECRET`.

## Démarrage en local (sans Docker pour l'appli)

```bash
# 1. MySQL (ou utilisez : docker compose up db)
# 2. Backend
cd backend
mvn spring-boot:run          # variables : DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET

# 3. Frontend
cd frontend
npm install
npm run dev                  # http://localhost:5173 (proxy /api → :8080)
```

## Comptes de démonstration

Créés au premier démarrage (`APP_SEED=true`). **À changer / désactiver en production.**

| Utilisateur | Mot de passe | Rôle | Accès |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | tout, y compris utilisateurs et rôles |
| `manager` | `manager123` | MANAGER | tout sauf utilisateurs/rôles |
| `sales` | `sales123` | SALES | clients, commandes, factures, paiements |
| `warehouse` | `warehouse123` | WAREHOUSE | produits, catégories, fournisseurs, stock |
| `viewer` | `viewer123` | VIEWER | lecture seule |

## Sécurité : JWT, rôles, permissions

- `POST /api/auth/login` → `{ token, roles, permissions }` ; envoyer ensuite `Authorization: Bearer <token>`.
- Chaque ressource a deux permissions : `X_READ` (GET) et `X_WRITE` (POST/PUT/DELETE) — ex. `PRODUCT_READ`, `ORDER_WRITE`.
  Les ressources : PRODUCT, CATEGORY, SUPPLIER, CUSTOMER, DEPARTMENT, EMPLOYEE, STOCK, ORDER, INVOICE, PAYMENT, USER (utilisateurs, rôles, permissions), DASHBOARD.
- Les règles sont centralisées dans `SecurityConfig` ; les rôles et leurs permissions sont des données (modifiables via `/api/roles`).
- Les autorisations sont relues en base à chaque requête : retirer un rôle ou désactiver un compte prend effet immédiatement.
- Mots de passe hachés en BCrypt, jamais renvoyés par l'API. Sessions sans état, CORS configurable (`CORS_ORIGINS`).

## API (extrait)

Toutes les listes sont paginées et triables : `?page=0&size=20&sort=name,asc`, avec recherche texte `?q=` et filtres.

| Ressource | Chemin | Filtres |
|---|---|---|
| Produits | `/api/products` | `categoryId`, `supplierId`, `active`, `lowStock=true` |
| Catégories, fournisseurs, clients, départements | `/api/categories` `/suppliers` `/customers` `/departments` | `q` |
| Employés | `/api/employees` | `departmentId` |
| Mouvements de stock | `/api/stock` (création seule) | `productId`, `type` |
| Commandes | `/api/orders` — `POST /{id}/status?value=`, `POST /{id}/invoice` | `status`, `customerId`, `employeeId` |
| Factures | `/api/invoices` — `POST /{id}/cancel` | `status`, `orderId`, `overdue=true` |
| Paiements | `/api/payments` | `invoiceId`, `method` |
| Utilisateurs / rôles / permissions | `/api/users` `/api/roles` `/api/permissions` | `q` |
| Tableau de bord | `/api/dashboard` | — |
| Auth | `/api/auth/login`, `/api/auth/me` | — |

Les relations s'envoient sous forme `{"category": {"id": 3}}`. Erreurs : JSON `{status, error, message, details}` (400 validation/règle métier, 401, 403, 404, 409 doublon).

Exemple de création de commande :

```json
POST /api/orders
{ "customer": {"id": 1}, "employee": {"id": 2},
  "lines": [ {"product": {"id": 1}, "quantity": 2}, {"product": {"id": 3}, "quantity": 10, "unitPrice": 40} ] }
```

## Règles métier

- **Stock** : la quantité d'un produit ne change que par mouvements (`IN`, `OUT`, `ADJUSTMENT`), historisés et immuables ; jamais négative.
- **Commande** : `DRAFT → CONFIRMED → SHIPPED → DELIVERED`. La confirmation décrémente le stock (refusée si insuffisant) ; l'annulation d'une commande confirmée/expédiée le réintègre. Seul un brouillon est modifiable.
- **Facture** : une par commande (confirmée, expédiée ou livrée), échéance par défaut +30 jours.
- **Paiement** : ne peut pas dépasser le reste à payer ; le statut de la facture passe automatiquement à `PARTIALLY_PAID` / `PAID` (et revient en arrière si un paiement est supprimé).

## Modèle de données

```
Permission ⇄ Role ⇄ AppUser ── 1:1 ── Employee ── N:1 ── Department
                                          └─ N:1 ── Employee (manager)
Category ──< Product >── Supplier        Product ──< StockMovement
Customer ──< SalesOrder >── Employee     SalesOrder ──< OrderLine >── Product
SalesOrder ── 1:1 ── Invoice ──< Payment
```

## Tests

```bash
cd backend && mvn test      # sécurité/JWT, validation, flux commande → stock → facture → paiement (H2)
cd frontend && npm test     # utilitaires
```

La CI GitHub (`.github/workflows/ci.yml`) lance ces tests et le build à chaque push.

## Structure

```
backend/src/main/java/com/erp
  common/    CRUD générique (pagination, recherche, filtres), erreurs, Specs
  config/    SecurityConfig, OpenApiConfig, DataInitializer (données de démo)
  security/  JwtService, JwtAuthFilter, UserDetailsService
  domain/    entités JPA        repository/  Spring Data
  service/   règles métier (stock, commandes, factures, paiements, dashboard)
  web/       contrôleurs REST
frontend/src
  api/ auth/ components/ (CrudPage générique) pages/
```

## Pistes d'amélioration

Refresh tokens, audit (créé par / modifié par), migrations Flyway au lieu de `ddl-auto=update`, export PDF des factures, modification des commandes brouillon dans l'interface, tests E2E.
