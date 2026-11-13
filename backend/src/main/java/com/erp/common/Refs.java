package com.erp.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.Set;

/** Résolution des références {"id": n} reçues dans le JSON vers de vraies entités. */
public final class Refs {
    private Refs() {}

    public static <E extends BaseEntity> E load(JpaRepository<E, Long> repo, E ref, String label) {
        if (ref == null || ref.getId() == null) {
            return null;
        }
        return repo.findById(ref.getId())
                .orElseThrow(() -> new NotFoundException(label + " introuvable : " + ref.getId()));
    }

    public static <E extends BaseEntity> E require(JpaRepository<E, Long> repo, E ref, String label) {
        E e = load(repo, ref, label);
        if (e == null) {
            throw new BusinessException(label + " obligatoire");
        }
        return e;
    }

    public static <E extends BaseEntity> Set<E> loadAll(JpaRepository<E, Long> repo, Set<E> refs, String label) {
        Set<E> result = new HashSet<>();
        if (refs != null) {
            for (E r : refs) {
                result.add(require(repo, r, label));
            }
        }
        return result;
    }
}
