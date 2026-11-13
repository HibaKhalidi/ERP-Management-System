package com.erp.common;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD générique : liste paginée + recherche (?q=) + filtres, lecture, création, modification, suppression.
 * Les sous-classes surchargent les "hooks" (prepare, doCreate, doUpdate, doDelete, filters).
 */
public abstract class BaseCrudController<T extends BaseEntity> {

    protected final BaseRepository<T> repo;
    private final String label;

    protected BaseCrudController(BaseRepository<T> repo, String label) {
        this.repo = repo;
        this.label = label;
    }

    /** Champs utilisés par la recherche ?q= (chemins pointés autorisés). */
    protected List<String> searchFields() {
        return List.of();
    }

    /** Filtres supplémentaires à partir des paramètres de requête. */
    protected Specification<T> filters(Map<String, String> params) {
        return Specs.none();
    }

    /** Résout les relations / règles métier avant sauvegarde. existing == null pour une création. */
    protected void prepare(T incoming, T existing) {
    }

    protected T doCreate(T entity) {
        entity.setId(null);
        prepare(entity, null);
        return repo.save(entity);
    }

    protected T doUpdate(Long id, T incoming) {
        T existing = find(id);
        incoming.setId(id);
        prepare(incoming, existing);
        return repo.save(incoming);
    }

    protected void doDelete(Long id) {
        repo.delete(find(id));
    }

    protected T find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException(label + " introuvable : " + id));
    }

    @GetMapping
    public Page<T> list(@RequestParam(required = false) String q,
                        @Parameter(hidden = true) @RequestParam Map<String, String> params,
                        @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Specification<T> spec = Specs.<T>search(q, searchFields()).and(filters(params));
        return repo.findAll(spec, pageable);
    }

    @GetMapping("/{id}")
    public T get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@Valid @RequestBody T body) {
        return doCreate(body);
    }

    @PutMapping("/{id}")
    public T update(@PathVariable Long id, @Valid @RequestBody T body) {
        return doUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        doDelete(id);
    }
}
