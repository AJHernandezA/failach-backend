package com.projectx.backend.infra.seed;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Seed de datos iniciales de categorías y productos para desarrollo.
 * Crea categorías y productos de ejemplo para cada tenant.
 */
public class ProductSeeder {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Inject
    public ProductSeeder(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Ejecuta el seed de categorías y productos para los 3 tenants.
     */
    public void seed() {
        seedTenant("idoneo", List.of(
                new CatSeed("cat-idoneo-1", "Alimentos", "Productos alimenticios naturales", 1),
                new CatSeed("cat-idoneo-2", "Bebidas", "Bebidas naturales y jugos", 2),
                new CatSeed("cat-idoneo-3", "Cuidado Personal", "Productos de cuidado personal", 3)
        ), List.of(
                new ProdSeed("prod-idoneo-1", "Granola Artesanal", "Granola hecha con avena, miel y frutos secos", 18500, 22000, "cat-idoneo-1", "Alimentos", 50, 1),
                new ProdSeed("prod-idoneo-2", "Miel de Abejas Orgánica", "Miel 100% orgánica de apiarios locales", 25000, 0, "cat-idoneo-1", "Alimentos", 30, 2),
                new ProdSeed("prod-idoneo-3", "Jugo Verde Detox", "Jugo prensado en frío con espinaca, manzana y jengibre", 12000, 15000, "cat-idoneo-2", "Bebidas", 100, 1),
                new ProdSeed("prod-idoneo-4", "Kombucha Artesanal", "Té fermentado con probióticos naturales", 14000, 0, "cat-idoneo-2", "Bebidas", 80, 2),
                new ProdSeed("prod-idoneo-5", "Jabón de Avena", "Jabón artesanal con avena y lavanda", 8500, 10000, "cat-idoneo-3", "Cuidado Personal", 120, 1),
                new ProdSeed("prod-idoneo-6", "Aceite de Coco Virgen", "Aceite de coco 100% virgen prensado en frío", 22000, 0, "cat-idoneo-3", "Cuidado Personal", 45, 2)
        ));

        seedTenant("chicha", List.of(
                new CatSeed("cat-chicha-1", "Bebidas Ancestrales", "Bebidas tradicionales colombianas", 1),
                new CatSeed("cat-chicha-2", "Snacks", "Acompañamientos y snacks", 2)
        ), List.of(
                new ProdSeed("prod-chicha-1", "Chicha de Maíz", "Chicha tradicional fermentada de maíz", 8000, 0, "cat-chicha-1", "Bebidas Ancestrales", 200, 1),
                new ProdSeed("prod-chicha-2", "Chicha de Arroz", "Chicha refrescante de arroz con canela", 7500, 0, "cat-chicha-1", "Bebidas Ancestrales", 150, 2),
                new ProdSeed("prod-chicha-3", "Masato", "Bebida tradicional de arroz fermentado", 7000, 0, "cat-chicha-1", "Bebidas Ancestrales", 100, 3),
                new ProdSeed("prod-chicha-4", "Empanadas de Pipián", "Empanadas rellenas de pipián, paquete x6", 15000, 18000, "cat-chicha-2", "Snacks", 60, 1)
        ));

        seedTenant("tech", List.of(
                new CatSeed("cat-tech-1", "Accesorios", "Accesorios tecnológicos", 1),
                new CatSeed("cat-tech-2", "Audio", "Audífonos y parlantes", 2)
        ), List.of(
                new ProdSeed("prod-tech-1", "Cable USB-C Premium", "Cable USB-C a USB-C de 2 metros, carga rápida", 25000, 35000, "cat-tech-1", "Accesorios", 500, 1),
                new ProdSeed("prod-tech-2", "Soporte Laptop Ajustable", "Soporte ergonómico de aluminio para laptop", 85000, 120000, "cat-tech-1", "Accesorios", 30, 2),
                new ProdSeed("prod-tech-3", "Audífonos Bluetooth TWS", "Audífonos inalámbricos con cancelación de ruido", 95000, 130000, "cat-tech-2", "Audio", 75, 1),
                new ProdSeed("prod-tech-4", "Parlante Portátil", "Parlante Bluetooth resistente al agua IPX7", 120000, 0, "cat-tech-2", "Audio", 40, 2)
        ));

        log.info("Seed de productos y categorías completado");
    }

    private void seedTenant(String tenantId, List<CatSeed> categories, List<ProdSeed> products) {
        // Verificar si ya hay categorías para este tenant
        if (!categoryRepository.findByTenant(tenantId).isEmpty()) {
            log.info("Tenant {} ya tiene categorías, omitiendo seed", tenantId);
            return;
        }

        Instant now = Instant.now();

        // Crear categorías
        for (CatSeed cs : categories) {
            categoryRepository.save(new Category(cs.id, tenantId, cs.name, cs.description, "", cs.sortOrder));
        }
        log.info("Tenant {}: {} categorías creadas", tenantId, categories.size());

        // Crear productos
        for (ProdSeed ps : products) {
            ProductStatus status = ps.stock > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK;
            BigDecimal compareAt = ps.compareAtPrice > 0 ? BigDecimal.valueOf(ps.compareAtPrice) : null;
            productRepository.save(new Product(
                    ps.id, tenantId, ps.name, ps.description,
                    BigDecimal.valueOf(ps.price), compareAt,
                    List.of("https://placehold.co/800x800/png?text=" + ps.name.replace(" ", "+")),
                    ps.categoryId, ps.categoryName, ps.stock,
                    List.of(), status, ps.sortOrder, now, now
            ));
        }
        log.info("Tenant {}: {} productos creados", tenantId, products.size());
    }

    private record CatSeed(String id, String name, String description, int sortOrder) {}
    private record ProdSeed(String id, String name, String description, int price, int compareAtPrice, String categoryId, String categoryName, int stock, int sortOrder) {}
}
