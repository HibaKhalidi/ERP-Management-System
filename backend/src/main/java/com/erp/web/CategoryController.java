package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.domain.Category;
import com.erp.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Catégorie")
public class CategoryController extends BaseCrudController<Category> {

    public CategoryController(CategoryRepository repo) {
        super(repo, "Catégorie");
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "description");
    }
}
