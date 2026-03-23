package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.AddToCartRequest;
import com.projectx.backend.application.dto.UpdateCartItemRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.ports.in.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private static final String SESSION_COOKIE = "sessionId";
    private static final int SESSION_MAX_AGE = 86400;

    private final GetCartUseCase getCart;
    private final AddToCartUseCase addToCart;
    private final UpdateCartItemUseCase updateCartItem;
    private final RemoveCartItemUseCase removeCartItem;
    private final ClearCartUseCase clearCart;

    @Inject
    public CartController(GetCartUseCase getCart, AddToCartUseCase addToCart, UpdateCartItemUseCase updateCartItem,
            RemoveCartItemUseCase removeCartItem, ClearCartUseCase clearCart) {
        this.getCart = getCart;
        this.addToCart = addToCart;
        this.updateCartItem = updateCartItem;
        this.removeCartItem = removeCartItem;
        this.clearCart = clearCart;
    }

    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/cart";

        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String sessionId = getOrCreateSessionId(ctx);
            Cart cart = getCart.execute(tenantId, sessionId);
            ctx.json(Map.of("data", cartResponse(cart)));
        });

        app.post(base + "/items", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String sessionId = getOrCreateSessionId(ctx);
            AddToCartRequest request = ctx.bodyAsClass(AddToCartRequest.class);
            Cart cart = addToCart.execute(tenantId, sessionId, request);
            ctx.status(201).json(Map.of("data", cartResponse(cart)));
        });

        app.put(base + "/items/{productId}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String productId = ctx.pathParam("productId");
            String sessionId = getOrCreateSessionId(ctx);
            UpdateCartItemRequest req = ctx.bodyAsClass(UpdateCartItemRequest.class);
            Cart cart = updateCartItem.execute(tenantId, sessionId, productId, req.quantity());
            ctx.json(Map.of("data", cartResponse(cart)));
        });

        app.delete(base + "/items/{productId}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String productId = ctx.pathParam("productId");
            String sessionId = getOrCreateSessionId(ctx);
            Cart cart = removeCartItem.execute(tenantId, sessionId, productId);
            ctx.json(Map.of("data", cartResponse(cart)));
        });

        app.delete(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String sessionId = getOrCreateSessionId(ctx);
            clearCart.execute(tenantId, sessionId);
            ctx.status(204);
        });

        log.info("Cart endpoints registrados en {}/tenants/:tenantId/cart", ApiConstants.API_PREFIX);
    }

    private String getOrCreateSessionId(Context ctx) {
        // Prioridad: cookie > header X-Session-Id > nueva sesión
        String sessionId = ctx.cookie(SESSION_COOKIE);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = ctx.header("X-Session-Id");
        }
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
            log.debug("Nueva sesión creada: {}", sessionId);
        }
        // Siempre re-enviar la cookie con atributos completos para garantizar
        // persistencia cross-origin
        ctx.header("Set-Cookie",
                SESSION_COOKIE + "=" + sessionId
                        + "; Max-Age=" + SESSION_MAX_AGE
                        + "; Path=/"
                        + "; SameSite=None"
                        + "; Secure"
                        + "; HttpOnly");
        return sessionId;
    }

    private Map<String, Object> cartResponse(Cart cart) {
        return Map.of(
                "cartId", cart.cartId(),
                "tenantId", cart.tenantId(),
                "items", cart.items(),
                "itemCount", cart.itemCount(),
                "subtotal", cart.subtotal(),
                "createdAt", cart.createdAt().toString(),
                "updatedAt", cart.updatedAt().toString());
    }
}
