package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductFilter;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.models.SearchProductsRequest;
import com.projectx.backend.domain.models.SearchResult;
import com.projectx.backend.domain.models.SortOption;
import com.projectx.backend.domain.ports.in.ListProductsUseCase;
import com.projectx.backend.domain.ports.in.SearchProductsUseCase;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementación de búsqueda de productos con filtros en memoria.
 * Para MVP con catálogos < 1000 productos por tenant.
 */
public class SearchProductsUseCaseImpl implements SearchProductsUseCase {

    private final ListProductsUseCase listProducts;

    @Inject
    public SearchProductsUseCaseImpl(ListProductsUseCase listProducts) {
        this.listProducts = listProducts;
    }

    @Override
    public SearchResult<Product> execute(SearchProductsRequest request) {
        // Validar query
        if (request.query() == null || request.query().trim().length() < 2) {
            throw new BadRequestException("La búsqueda debe tener al menos 2 caracteres");
        }

        // Limitar tamaño
        int size = Math.min(request.size(), 50);
        if (size <= 0)
            size = 20;

        // Obtener todos los productos activos del tenant
        ProductFilter filter = new ProductFilter(null, null, ProductStatus.ACTIVE, 0, 1000);
        Page<Product> allProducts = listProducts.execute(request.tenantId(), filter);

        String queryLower = request.query().trim().toLowerCase();

        // Filtrar en memoria
        Stream<Product> stream = allProducts.items().stream()
                .filter(p -> matchesQuery(p, queryLower));

        // Filtrar por categoría
        if (request.categoryId() != null && !request.categoryId().isBlank()) {
            stream = stream.filter(p -> request.categoryId().equals(p.categoryId()));
        }

        // Filtrar por precio mínimo
        if (request.minPrice() != null) {
            BigDecimal min = BigDecimal.valueOf(request.minPrice());
            stream = stream.filter(p -> p.price().compareTo(min) >= 0);
        }

        // Filtrar por precio máximo
        if (request.maxPrice() != null) {
            BigDecimal max = BigDecimal.valueOf(request.maxPrice());
            stream = stream.filter(p -> p.price().compareTo(max) <= 0);
        }

        // Ordenar
        List<Product> filtered = stream.sorted(getComparator(request.sort())).toList();

        // Paginar (1-based)
        int page = Math.max(request.page(), 1);
        long total = filtered.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());

        List<Product> pageItems = fromIndex < filtered.size()
                ? filtered.subList(fromIndex, toIndex)
                : List.of();

        return new SearchResult<>(pageItems, total, page, totalPages, size);
    }

    /**
     * Verifica si un producto coincide con la búsqueda (nombre o descripción,
     * case-insensitive).
     */
    private boolean matchesQuery(Product product, String queryLower) {
        boolean nameMatch = product.name() != null
                && product.name().toLowerCase().contains(queryLower);
        boolean descMatch = product.description() != null
                && product.description().toLowerCase().contains(queryLower);
        return nameMatch || descMatch;
    }

    /**
     * Retorna el comparador según la opción de ordenamiento.
     */
    private Comparator<Product> getComparator(SortOption sort) {
        if (sort == null)
            sort = SortOption.NAME;
        return switch (sort) {
            case PRICE_ASC -> Comparator.comparing(Product::price);
            case PRICE_DESC -> Comparator.comparing(Product::price).reversed();
            case NEWEST -> Comparator.comparing(Product::createdAt).reversed();
            case NAME -> Comparator.comparing(p -> p.name().toLowerCase());
        };
    }
}
