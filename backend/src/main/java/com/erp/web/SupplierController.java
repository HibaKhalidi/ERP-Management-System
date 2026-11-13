package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.domain.Supplier;
import com.erp.repository.SupplierRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Fournisseur")
public class SupplierController extends BaseCrudController<Supplier> {

    public SupplierController(SupplierRepository repo) {
        super(repo, "Fournisseur");
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "contactName", "email", "city");
    }
}
