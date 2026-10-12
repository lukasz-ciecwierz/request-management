package io.github.lukaszciecwierz.requestmanagement.infra;

import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

class RequestSpecifications {

    private static final char ESCAPE_CHAR = '!';

    static Specification<RequestEntity> matching(@Nullable String name, @Nullable RequestState state) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (name != null && !name.isBlank()) {
                String escape = String.valueOf(ESCAPE_CHAR);
                String escapedName = name.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace(escape, escape + escape)
                        .replace("%", escape + "%")
                        .replace("_", escape + "_");

                predicates.add(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(RequestEntity_.name)), "%" + escapedName + "%", ESCAPE_CHAR)
                );
            }

            if (state != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get(RequestEntity_.state), state)
                );
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
