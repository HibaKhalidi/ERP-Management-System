package com.erp.config;

import com.erp.domain.*;
import com.erp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Données initiales : permissions, rôles, comptes de démo et jeu de données d'exemple. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissions;
    private final RoleRepository roles;
    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final PasswordEncoder encoder;

    @Value("${app.seed:true}")
    private boolean seed;

    public DataInitializer(PermissionRepository permissions, RoleRepository roles, AppUserRepository users,
                           DepartmentRepository departments, EmployeeRepository employees, CategoryRepository categories,
                           SupplierRepository suppliers, CustomerRepository customers, ProductRepository products,
                           StockMovementRepository movements, PasswordEncoder encoder) {
        this.permissions = permissions;
        this.roles = roles;
        this.users = users;
        this.departments = departments;
        this.employees = employees;
        this.categories = categories;
        this.suppliers = suppliers;
        this.customers = customers;
        this.products = products;
        this.movements = movements;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seed || permissions.count() > 0) {
            return;
        }
        log.info("Initialisation des permissions, rôles et données de démo…");

        Map<String, Permission> perms = new LinkedHashMap<>();
        for (String prefix : new LinkedHashSet<>(SecurityConfig.RESOURCES.values())) {
            for (String action : List.of("READ", "WRITE")) {
                Permission p = new Permission();
                p.setName(prefix + "_" + action);
                perms.put(p.getName(), permissions.save(p));
            }
        }

        Role admin = role("ADMIN", "Accès total", new HashSet<>(perms.values()));
        Set<Permission> manager = new HashSet<>(perms.values());
        manager.removeIf(p -> p.getName().startsWith("USER_"));
        Role managerRole = role("MANAGER", "Gestion complète sauf administration des utilisateurs", manager);
        Role sales = role("SALES", "Ventes : clients, commandes, factures, paiements",
                pick(perms, "PRODUCT_READ", "CATEGORY_READ", "EMPLOYEE_READ", "CUSTOMER_READ", "CUSTOMER_WRITE",
                        "ORDER_READ", "ORDER_WRITE", "INVOICE_READ", "INVOICE_WRITE", "PAYMENT_READ", "PAYMENT_WRITE", "DASHBOARD_READ"));
        Role warehouse = role("WAREHOUSE", "Magasin : produits, fournisseurs, stock",
                pick(perms, "PRODUCT_READ", "PRODUCT_WRITE", "CATEGORY_READ", "CATEGORY_WRITE", "SUPPLIER_READ", "SUPPLIER_WRITE",
                        "STOCK_READ", "STOCK_WRITE", "ORDER_READ", "DASHBOARD_READ"));
        Set<Permission> viewer = new HashSet<>();
        perms.values().stream().filter(p -> p.getName().endsWith("_READ") && !p.getName().startsWith("USER_")).forEach(viewer::add);
        Role viewerRole = role("VIEWER", "Lecture seule", viewer);

        user("admin", "admin@erp.local", "admin123", admin);
        user("manager", "manager@erp.local", "manager123", managerRole);
        user("sales", "sales@erp.local", "sales123", sales);
        user("warehouse", "warehouse@erp.local", "warehouse123", warehouse);
        user("viewer", "viewer@erp.local", "viewer123", viewerRole);

        // Départements et employés
        Department dSales = dept("Ventes");
        Department dLog = dept("Logistique");
        Department dFin = dept("Finance");
        Employee boss = employee("Amina", "Benali", "amina.benali@erp.local", "Directrice générale", "25000", dFin, null);
        employee("Youssef", "El Idrissi", "youssef.elidrissi@erp.local", "Responsable ventes", "14000", dSales, boss);
        employee("Salma", "Rachidi", "salma.rachidi@erp.local", "Magasinière", "8000", dLog, boss);

        // Catalogue
        Category electronics = cat("Électronique");
        Category office = cat("Fournitures de bureau");
        Category furniture = cat("Mobilier");
        Supplier s1 = supplier("TechDistrib", "contact@techdistrib.example", "Casablanca");
        Supplier s2 = supplier("OfficePlus", "contact@officeplus.example", "Rabat");
        product("ELEC-001", "Ordinateur portable 15\"", "6500", "5200", 12, 3, electronics, s1);
        product("ELEC-002", "Écran 27\" 4K", "2800", "2100", 20, 5, electronics, s1);
        product("OFF-001", "Ramette papier A4 (500 feuilles)", "45", "30", 200, 50, office, s2);
        product("OFF-002", "Stylos bille (boîte de 50)", "60", "38", 4, 10, office, s2);
        product("FUR-001", "Bureau ajustable", "3200", "2400", 6, 2, furniture, s2);

        // Clients
        customer("Atlas Industries", "achats@atlas.example", "Casablanca");
        customer("Café du Port", "contact@cafeduport.example", "Tanger");
        customer("Clinique Al Amal", "compta@alamal.example", "Marrakech");
    }

    private Role role(String name, String desc, Set<Permission> ps) {
        Role r = new Role();
        r.setName(name);
        r.setDescription(desc);
        r.setPermissions(ps);
        return roles.save(r);
    }

    private Set<Permission> pick(Map<String, Permission> all, String... names) {
        Set<Permission> s = new HashSet<>();
        for (String n : names) s.add(all.get(n));
        return s;
    }

    private AppUser user(String username, String email, String pwd, Role role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(encoder.encode(pwd));
        u.setRoles(new HashSet<>(Set.of(role)));
        return users.save(u);
    }

    private Department dept(String name) {
        Department d = new Department();
        d.setName(name);
        return departments.save(d);
    }

    private Employee employee(String fn, String ln, String email, String position, String salary, Department d, Employee manager) {
        Employee e = new Employee();
        e.setFirstName(fn);
        e.setLastName(ln);
        e.setEmail(email);
        e.setPosition(position);
        e.setSalary(new BigDecimal(salary));
        e.setHireDate(LocalDate.now().minusYears(2));
        e.setDepartment(d);
        e.setManager(manager);
        return employees.save(e);
    }

    private Category cat(String name) {
        Category c = new Category();
        c.setName(name);
        return categories.save(c);
    }

    private Supplier supplier(String name, String email, String city) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setEmail(email);
        s.setCity(city);
        return suppliers.save(s);
    }

    private Customer customer(String name, String email, String city) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setCity(city);
        return customers.save(c);
    }

    private void product(String sku, String name, String price, String cost, int qty, int reorder, Category c, Supplier s) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCostPrice(new BigDecimal(cost));
        p.setQuantityInStock(qty);
        p.setReorderLevel(reorder);
        p.setCategory(c);
        p.setSupplier(s);
        p = products.save(p);
        StockMovement m = new StockMovement();
        m.setProduct(p);
        m.setType(MovementType.IN);
        m.setQuantity(qty);
        m.setReason("Stock initial");
        m.setCreatedBy("system");
        movements.save(m);
    }
}
