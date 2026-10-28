package com.lavinuela.shop.shared.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados.
 *
 * @param items elementos de esta página
 * @param page número de página, desde 0
 * @param size tamaño pedido
 * @param totalItems total de elementos en todas las páginas
 * @param totalPages total de páginas
 * @param <T> tipo de los elementos
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PageResult {
        items = List.copyOf(items);
    }

    /** Construye la página calculando el total de páginas. */
    public static <T> PageResult<T> of(List<T> items, PageQuery query, long totalItems) {
        int pages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / query.size());
        return new PageResult<>(items, query.page(), query.size(), totalItems, pages);
    }

    /** Transforma los elementos conservando los datos de paginación (por ejemplo, de entidad a DTO). */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = items.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalItems, totalPages);
    }
}
