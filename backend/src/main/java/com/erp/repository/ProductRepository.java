package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends BaseRepository<Product> {
    @Query("select count(p) from Product p where p.active = true and p.quantityInStock <= p.reorderLevel")
    long countLowStock();

    @Query("select p from Product p where p.active = true and p.quantityInStock <= p.reorderLevel order by p.quantityInStock asc")
    List<Product> findLowStock(Pageable pageable);
}
