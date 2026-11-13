package com.erp.common;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Petits constructeurs de Specification pour la recherche et les filtres. */
public final class Specs {
    private Specs() {}

    public static <T> Specification<T> none() {
        return (root, query, cb) -> cb.conjunction();
    }

    @SuppressWarnings("unchecked")
    private static <Y> Path<Y> path(From<?, ?> root, String field) {
        String[] parts = field.split("\\.");
        From<?, ?> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.join(parts[i], JoinType.LEFT);
        }
        return (Path<Y>) current.get(parts[parts.length - 1]);
    }

    /** Recherche texte (LIKE, insensible à la casse) sur plusieurs champs (chemins pointés autorisés). */
    public static <T> Specification<T> search(String q, List<String> fields) {
        if (q == null || q.isBlank() || fields.isEmpty()) {
            return none();
        }
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            List<Predicate> ors = new ArrayList<>();
            for (String f : fields) {
                Expression<String> p = Specs.<String>path(root, f).as(String.class);
                ors.add(cb.like(cb.lower(p), like));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> equal(String field, Object value) {
        if (value == null) {
            return none();
        }
        return (root, query, cb) -> cb.equal(Specs.<Object>path(root, field), value);
    }

    public static Long toLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Valeur numérique invalide : " + s);
        }
    }

    public static <E extends Enum<E>> E toEnum(Class<E> type, String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Enum.valueOf(type, s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Valeur invalide : " + s);
        }
    }
}
