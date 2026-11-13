package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.domain.Customer;
import com.erp.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Client")
public class CustomerController extends BaseCrudController<Customer> {

    public CustomerController(CustomerRepository repo) {
        super(repo, "Client");
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "email", "phone", "city");
    }
}
