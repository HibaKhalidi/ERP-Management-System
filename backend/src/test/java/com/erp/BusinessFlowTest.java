package com.erp;

import com.erp.common.BusinessException;
import com.erp.domain.*;
import com.erp.repository.*;
import com.erp.service.InvoiceService;
import com.erp.service.OrderService;
import com.erp.service.PaymentService;
import com.erp.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Flux métier complet : commande → stock → facture → paiements. */
@SpringBootTest
@Transactional
class BusinessFlowTest {

    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired OrderService orders;
    @Autowired InvoiceService invoices;
    @Autowired PaymentService payments;
    @Autowired StockService stock;

    private Product product(int qty) {
        Product p = new Product();
        p.setSku("T-" + System.nanoTime());
        p.setName("Produit test");
        p.setPrice(new BigDecimal("100.00"));
        p.setQuantityInStock(qty);
        return products.save(p);
    }

    private SalesOrder draft(Product p, int qty) {
        Customer c = customers.findAll().get(0);
        SalesOrder in = new SalesOrder();
        in.setCustomer(c);
        OrderLine l = new OrderLine();
        l.setProduct(p);
        l.setQuantity(qty);
        in.setLines(List.of(l));
        return orders.create(in);
    }

    @Test
    void confirmingOrderDecrementsStockAndCancelRestoresIt() {
        Product p = product(10);
        SalesOrder o = draft(p, 4);
        assertThat(o.getTotalAmount()).isEqualByComparingTo("400");
        assertThat(o.getNumber()).startsWith("SO-");

        orders.changeStatus(o.getId(), OrderStatus.CONFIRMED);
        assertThat(products.findById(p.getId()).orElseThrow().getQuantityInStock()).isEqualTo(6);

        orders.changeStatus(o.getId(), OrderStatus.CANCELLED);
        assertThat(products.findById(p.getId()).orElseThrow().getQuantityInStock()).isEqualTo(10);
    }

    @Test
    void cannotConfirmWhenStockIsInsufficient() {
        Product p = product(2);
        SalesOrder o = draft(p, 5);
        assertThatThrownBy(() -> orders.changeStatus(o.getId(), OrderStatus.CONFIRMED))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Stock insuffisant");
    }

    @Test
    void invoiceAndPaymentsUpdateStatus() {
        Product p = product(10);
        SalesOrder o = draft(p, 2); // 200
        assertThatThrownBy(() -> invoices.createFromOrder(o.getId(), null)).isInstanceOf(BusinessException.class);
        orders.changeStatus(o.getId(), OrderStatus.CONFIRMED);

        Invoice inv = invoices.createFromOrder(o.getId(), null);
        assertThat(inv.getNumber()).startsWith("INV-");
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThatThrownBy(() -> invoices.createFromOrder(o.getId(), null)).isInstanceOf(BusinessException.class);

        Payment first = pay(inv, "50");
        assertThat(invoices.get(inv.getId()).getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);

        assertThatThrownBy(() -> pay(inv, "500")).isInstanceOf(BusinessException.class);

        pay(inv, "150");
        Invoice paid = invoices.get(inv.getId());
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getBalance()).isEqualByComparingTo("0");

        payments.delete(first.getId());
        assertThat(invoices.get(inv.getId()).getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void stockMovementsValidateQuantities() {
        Product p = product(3);
        assertThatThrownBy(() -> stock.apply(p, MovementType.OUT, 10, "test")).isInstanceOf(BusinessException.class);
        stock.apply(p, MovementType.ADJUSTMENT, -1, "inventaire");
        assertThat(products.findById(p.getId()).orElseThrow().getQuantityInStock()).isEqualTo(2);
    }

    private Payment pay(Invoice inv, String amount) {
        Payment in = new Payment();
        in.setInvoice(inv);
        in.setAmount(new BigDecimal(amount));
        in.setMethod(PaymentMethod.CASH);
        return payments.create(in);
    }
}
