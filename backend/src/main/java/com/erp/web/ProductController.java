package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.Refs;
import com.erp.common.Specs;
import com.erp.domain.MovementType;
import com.erp.domain.Product;
import com.erp.repository.CategoryRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.SupplierRepository;
import com.erp.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Filtres : ?categoryId= &supplierId= &active=true|false &lowStock=true */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Produits")
public class ProductController extends BaseCrudController<Product> {

    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final StockService stock;

    public ProductController(ProductRepository repo, CategoryRepository categories, SupplierRepository suppliers, StockService stock) {
        super(repo, "Produit");
        this.categories = categories;
        this.suppliers = suppliers;
        this.stock = stock;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("sku", "name", "description", "category.name", "supplier.name");
    }

    @Override
    protected Specification<Product> filters(Map<String, String> p) {
        Specification<Product> spec = Specs.<Product>equal("category.id", Specs.toLong(p.get("categoryId")))
                .and(Specs.equal("supplier.id", Specs.toLong(p.get("supplierId"))));
        if (p.get("active") != null && !p.get("active").isBlank()) {
            spec = spec.and(Specs.equal("active", Boolean.parseBoolean(p.get("active"))));
        }
        if ("true".equalsIgnoreCase(p.get("lowStock"))) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.<Integer>get("quantityInStock"), root.<Integer>get("reorderLevel")));
        }
        return spec;
    }

    @Override
    protected void prepare(Product in, Product existing) {
        in.setCategory(Refs.load(categories, in.getCategory(), "Catégorie"));
        in.setSupplier(Refs.load(suppliers, in.getSupplier(), "Fournisseur"));
        if (existing != null) {
            // le stock ne se modifie que par mouvements
            in.setQuantityInStock(existing.getQuantityInStock());
        }
    }

    @Override
    protected Product doCreate(Product entity) {
        int initial = Math.max(entity.getQuantityInStock(), 0);
        entity.setId(null);
        entity.setQuantityInStock(0);
        prepare(entity, null);
        Product saved = repo.save(entity);
        if (initial > 0) {
            stock.apply(saved, MovementType.IN, initial, "Stock initial");
        }
        return find(saved.getId());
    }
}
