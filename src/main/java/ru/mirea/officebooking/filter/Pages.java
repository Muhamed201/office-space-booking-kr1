package ru.mirea.officebooking.filter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.mirea.officebooking.error.ValidationException;

import java.util.Set;

public final class Pages {
    private Pages() {}

    public static PageRequest sorted(Pageable pageable, Set<String> fields, Sort fallback) {
        Sort sort = pageable.getSortOr(fallback);
        if (sort.stream().anyMatch(order -> !fields.contains(order.getProperty()))) {
            throw new ValidationException("Недопустимое поле сортировки");
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }
}
