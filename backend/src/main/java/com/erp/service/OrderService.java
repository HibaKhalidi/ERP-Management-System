package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.common.NotFoundException;
import com.erp.common.Refs;
import com.erp.domain.*;
import com.erp.repository.CustomerRepository;
import com.erp.repository.EmployeeRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final SalesOrderRepository orders;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final ProductRepository products;
    private final StockService stock;

    public OrderService(SalesOrderRepository orders, CustomerRepository customers, EmployeeRepository employees,
                        ProductRepository products, StockService stock) {
        this.orders = orders;
        this.customers = customers;
        this.employees = employees;
        this.products = products;
        this.stock = stock;
    }

    @Transactional
    public SalesOrder create(SalesOrder in) {
        SalesOrder order = new SalesOrder();
        order.setNumber("TMP-" + UUID.randomUUID());
        order.setStatus(OrderStatus.DRAFT);
        fill(order, in);
        SalesOrder saved = orders.save(order);
        saved.setNumber(String.format("SO-%06d", saved.getId()));
        return orders.save(saved);
    }

    @Transactional
    public SalesOrder update(Long id, SalesOrder in) {
        SalesOrder order = get(id);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessException("Seule une commande en brouillon (DRAFT) peut être modifiée");
        }
        fill(order, in);
        return orders.save(order);
    }

    @Transactional
    public void delete(Long id) {
        SalesOrder order = get(id);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("Seule une commande DRAFT ou CANCELLED peut être supprimée");
        }
        orders.delete(order);
    }

    /** Transitions : DRAFT→CONFIRMED→SHIPPED→DELIVERED ; annulation possible avant livraison. */
    @Transactional
    public SalesOrder changeStatus(Long id, OrderStatus target) {
        SalesOrder order = get(id);
        OrderStatus current = order.getStatus();
        if (current == target) {
            return order;
        }
        boolean ok = switch (target) {
            case CONFIRMED -> current == OrderStatus.DRAFT;
            case SHIPPED -> current == OrderStatus.CONFIRMED;
            case DELIVERED -> current == OrderStatus.SHIPPED;
            case CANCELLED -> current == OrderStatus.DRAFT || current == OrderStatus.CONFIRMED || current == OrderStatus.SHIPPED;
            case DRAFT -> false;
        };
        if (!ok) {
            throw new BusinessException("Transition interdite : " + current + " → " + target);
        }
        if (target == OrderStatus.CONFIRMED) {
            for (OrderLine l : order.getLines()) {
                stock.apply(l.getProduct(), MovementType.OUT, l.getQuantity(), "Commande " + order.getNumber());
            }
        } else if (target == OrderStatus.CANCELLED && current != OrderStatus.DRAFT) {
            for (OrderLine l : order.getLines()) {
                stock.apply(l.getProduct(), MovementType.IN, l.getQuantity(), "Annulation " + order.getNumber());
            }
        }
        order.setStatus(target);
        return orders.save(order);
    }

    public SalesOrder get(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Commande introuvable : " + id));
    }

    private void fill(SalesOrder order, SalesOrder in) {
        order.setCustomer(Refs.require(customers, in.getCustomer(), "Client"));
        order.setEmployee(Refs.load(employees, in.getEmployee(), "Employé"));
        order.setNotes(in.getNotes());
        if (in.getOrderDate() != null) {
            order.setOrderDate(in.getOrderDate());
        }
        if (in.getLines() == null || in.getLines().isEmpty()) {
            throw new BusinessException("Une commande doit contenir au moins une ligne");
        }
        List<OrderLine> newLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine l : in.getLines()) {
            if (l.getQuantity() < 1) {
                throw new BusinessException("Quantité invalide");
            }
            Product p = Refs.require(products, l.getProduct(), "Produit");
            OrderLine line = new OrderLine();
            line.setOrder(order);
            line.setProduct(p);
            line.setQuantity(l.getQuantity());
            BigDecimal price = l.getUnitPrice() != null ? l.getUnitPrice() : p.getPrice();
            line.setUnitPrice(price);
            line.setLineTotal(price.multiply(BigDecimal.valueOf(l.getQuantity())));
            total = total.add(line.getLineTotal());
            newLines.add(line);
        }
        order.getLines().clear();
        order.getLines().addAll(newLines);
        order.setTotalAmount(total);
    }
}
