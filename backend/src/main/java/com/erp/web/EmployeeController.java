package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.Refs;
import com.erp.common.Specs;
import com.erp.domain.Employee;
import com.erp.repository.AppUserRepository;
import com.erp.repository.DepartmentRepository;
import com.erp.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Filtres : ?departmentId= */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employés")
public class EmployeeController extends BaseCrudController<Employee> {

    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    private final AppUserRepository users;

    public EmployeeController(EmployeeRepository repo, DepartmentRepository departments, AppUserRepository users) {
        super(repo, "Employé");
        this.employees = repo;
        this.departments = departments;
        this.users = users;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("firstName", "lastName", "email", "position", "department.name");
    }

    @Override
    protected Specification<Employee> filters(Map<String, String> p) {
        return Specs.equal("department.id", Specs.toLong(p.get("departmentId")));
    }

    @Override
    protected void prepare(Employee in, Employee existing) {
        in.setDepartment(Refs.load(departments, in.getDepartment(), "Département"));
        in.setManager(Refs.load(employees, in.getManager(), "Manager"));
        in.setUser(Refs.load(users, in.getUser(), "Utilisateur"));
    }
}
