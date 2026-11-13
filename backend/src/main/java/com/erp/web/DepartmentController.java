package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.domain.Department;
import com.erp.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Département")
public class DepartmentController extends BaseCrudController<Department> {

    public DepartmentController(DepartmentRepository repo) {
        super(repo, "Département");
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "description");
    }
}
