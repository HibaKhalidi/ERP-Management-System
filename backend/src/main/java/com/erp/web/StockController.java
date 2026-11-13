package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.BusinessException;
import com.erp.common.Refs;
import com.erp.common.Specs;
import com.erp.domain.MovementType;
import com.erp.domain.StockMovement;
import com.erp.repository.ProductRepository;
import com.erp.repository.StockMovementRepository;
import com.erp.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Historique des mouvements de stock (immuable). Filtres : ?productId= &type=IN|OUT|ADJUSTMENT */
@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock")
public class StockController extends BaseCrudController<StockMovement> {

    private final StockService stock;
    private final ProductRepository products;

    public StockController(StockMovementRepository repo, StockService stock, ProductRepository products) {
        super(repo, "Mouvement");
        this.stock = stock;
        this.products = products;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("product.name", "product.sku", "reason", "createdBy");
    }

    @Override
    protected Specification<StockMovement> filters(Map<String, String> p) {
        return Specs.<StockMovement>equal("product.id", Specs.toLong(p.get("productId")))
                .and(Specs.equal("type", Specs.toEnum(MovementType.class, p.get("type"))));
    }

    @Override
    protected StockMovement doCreate(StockMovement in) {
        var product = Refs.require(products, in.getProduct(), "Produit");
        return stock.apply(product, in.getType(), in.getQuantity(), in.getReason());
    }

    @Override
    protected StockMovement doUpdate(Long id, StockMovement in) {
        throw new BusinessException("L'historique de stock est immuable ; créez un mouvement d'ajustement");
    }

    @Override
    protected void doDelete(Long id) {
        throw new BusinessException("L'historique de stock est immuable ; créez un mouvement d'ajustement");
    }
}
