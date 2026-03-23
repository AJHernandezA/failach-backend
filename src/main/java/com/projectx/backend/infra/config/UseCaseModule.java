package com.projectx.backend.infra.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.projectx.backend.application.usecases.*;
import com.projectx.backend.domain.ports.in.*;

/**
 * Módulo Guice para binding de use cases.
 * Aquí se registran las interfaces de use cases con sus implementaciones.
 * Se irán agregando bindings a medida que se implementen las features.
 */
public class UseCaseModule extends AbstractModule {

    @Override
    protected void configure() {
        // F002: Tenant Configuration
        bind(GetTenantConfigUseCase.class).to(GetTenantConfigUseCaseImpl.class).in(Singleton.class);
        bind(UpdateTenantConfigUseCase.class).to(UpdateTenantConfigUseCaseImpl.class).in(Singleton.class);

        // F003: Product Catalog
        bind(ListProductsUseCase.class).to(ListProductsUseCaseImpl.class).in(Singleton.class);
        bind(GetProductUseCase.class).to(GetProductUseCaseImpl.class).in(Singleton.class);
        bind(CreateProductUseCase.class).to(CreateProductUseCaseImpl.class).in(Singleton.class);
        bind(UpdateProductUseCase.class).to(UpdateProductUseCaseImpl.class).in(Singleton.class);
        bind(DeleteProductUseCase.class).to(DeleteProductUseCaseImpl.class).in(Singleton.class);
        bind(ListCategoriesUseCase.class).to(ListCategoriesUseCaseImpl.class).in(Singleton.class);
        bind(CreateCategoryUseCase.class).to(CreateCategoryUseCaseImpl.class).in(Singleton.class);

        // F005: Checkout / Orders
        bind(CreateOrderUseCase.class).to(CreateOrderUseCaseImpl.class).in(Singleton.class);
        bind(GetOrderByCodeUseCase.class).to(GetOrderByCodeUseCaseImpl.class).in(Singleton.class);

        // F004: Shopping Cart
        bind(GetCartUseCase.class).to(GetCartUseCaseImpl.class).in(Singleton.class);
        bind(AddToCartUseCase.class).to(AddToCartUseCaseImpl.class).in(Singleton.class);
        bind(UpdateCartItemUseCase.class).to(UpdateCartItemUseCaseImpl.class).in(Singleton.class);
        bind(RemoveCartItemUseCase.class).to(RemoveCartItemUseCaseImpl.class).in(Singleton.class);
        bind(ClearCartUseCase.class).to(ClearCartUseCaseImpl.class).in(Singleton.class);

        // F006: Wompi Payment
        bind(InitiateWompiPaymentUseCase.class).to(InitiateWompiPaymentUseCaseImpl.class).in(Singleton.class);
        bind(ProcessWompiWebhookUseCase.class).to(ProcessWompiWebhookUseCaseImpl.class).in(Singleton.class);
        bind(GetPaymentStatusUseCase.class).to(GetPaymentStatusUseCaseImpl.class).in(Singleton.class);

        // F007: Manual Payment
        bind(ConfirmManualPaymentUseCase.class).to(ConfirmManualPaymentUseCaseImpl.class).in(Singleton.class);
        bind(CancelOrderUseCase.class).to(CancelOrderUseCaseImpl.class).in(Singleton.class);

        // F008: Order Tracking
        bind(UpdateOrderStatusUseCase.class).to(UpdateOrderStatusUseCaseImpl.class).in(Singleton.class);
        bind(ListOrdersUseCase.class).to(ListOrdersUseCaseImpl.class).in(Singleton.class);

        // F012: Storefront (Homepage)
        bind(GetStorefrontDataUseCase.class).to(GetStorefrontDataUseCaseImpl.class).in(Singleton.class);

        // F016: SEO / Sitemap
        bind(GetSitemapDataUseCase.class).to(GetSitemapDataUseCaseImpl.class).in(Singleton.class);

        // F018: Legal Content
        bind(GetLegalContentUseCase.class).to(GetLegalContentUseCaseImpl.class).in(Singleton.class);
        bind(UpdateLegalContentUseCase.class).to(UpdateLegalContentUseCaseImpl.class).in(Singleton.class);

        // F022: Product Search
        bind(SearchProductsUseCase.class).to(SearchProductsUseCaseImpl.class).in(Singleton.class);

        // F035: Admin Dashboard Metrics
        bind(GetDashboardMetricsUseCase.class).to(GetDashboardMetricsUseCaseImpl.class).in(Singleton.class);

        // F032: Platform Landing Page
        bind(GetPlatformInfoUseCase.class).to(GetPlatformInfoUseCaseImpl.class).in(Singleton.class);
        bind(ListPublicStoresUseCase.class).to(ListPublicStoresUseCaseImpl.class).in(Singleton.class);
        bind(SendContactMessageUseCase.class).to(SendContactMessageUseCaseImpl.class).in(Singleton.class);
    }
}
