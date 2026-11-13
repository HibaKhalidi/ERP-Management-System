package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.domain.MovementType;
import com.erp.domain.Product;
import com.erp.domain.StockMovement;
import com.erp.repository.ProductRepository;
import com.erp.repository.StockMovementRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /** Applique un mouvement : met à jour la quantité du produit et enregistre l'historique. */
    @Transactional
    public StockMovement apply(Product product, MovementType type, int quantity, String reason) {
        if (type != MovementType.ADJUSTMENT && quantity <= 0) {
            throw new BusinessException("La quantité doit être strictement positive");
        }
        if (type == MovementType.ADJUSTMENT && quantity == 0) {
            throw new BusinessException("L'ajustement ne peut pas être nul");
        }
        int delta = type == MovementType.OUT ? -quantity : quantity;
        int newQty = product.getQuantityInStock() + delta;
        if (newQty < 0) {
            throw new BusinessException("Stock insuffisant pour « " + product.getName() + " » (disponible : "
                    + product.getQuantityInStock() + ")");
        }
        product.setQuantityInStock(newQty);
        products.save(product);

        StockMovement m = new StockMovement();
        m.setProduct(product);
        m.setType(type);
        m.setQuantity(quantity);
        m.setReason(reason);
        m.setCreatedBy(currentUser());
        return movements.save(m);
    }

    private String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }
}
