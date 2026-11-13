package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.OrderStatus;
import com.erp.domain.SalesOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SalesOrderRepository extends BaseRepository<SalesOrder> {
    @Query("select coalesce(sum(o.totalAmount), 0) from SalesOrder o where o.status <> :excluded and o.status <> :draft")
    BigDecimal sumSales(@Param("excluded") OrderStatus excluded, @Param("draft") OrderStatus draft);

    long countByStatus(OrderStatus status);

    List<SalesOrder> findTop5ByOrderByIdDesc();

    @Query("select o from SalesOrder o where o.orderDate >= :from and o.status <> :excluded and o.status <> :draft")
    List<SalesOrder> findSince(@Param("from") LocalDate from, @Param("excluded") OrderStatus excluded, @Param("draft") OrderStatus draft);
}
