package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateOrderRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.CreateOrderUseCase;
import com.projectx.backend.domain.ports.out.CartRepository;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementación del caso de uso para crear una orden a partir del carrito.
 * Valida stock, re-calcula precios desde BD, genera orderCode legible,
 * y vacía el carrito tras éxito.
 */
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderUseCaseImpl.class);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    @Inject
    public CreateOrderUseCaseImpl(CartRepository cartRepository, OrderRepository orderRepository,
            ProductRepository productRepository, TenantRepository tenantRepository,
            EmailService emailService) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    @Override
    public Order execute(String tenantId, String sessionId, CreateOrderRequest req) {
        // 1. Validar campos del comprador
        validateRequest(req);

        // 2. Obtener carrito
        Cart cart = cartRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> new BadRequestException("El carrito está vacío"));

        if (cart.items().isEmpty()) {
            throw new BadRequestException("El carrito está vacío");
        }

        // 3. Parsear enums
        DeliveryMethod deliveryMethod;
        try {
            deliveryMethod = DeliveryMethod.valueOf(req.deliveryMethod());
        } catch (Exception e) {
            throw new BadRequestException("Método de entrega inválido");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(req.paymentMethod());
        } catch (Exception e) {
            throw new BadRequestException("Método de pago inválido");
        }

        // 4. Validar dirección si es envío
        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            if (req.address() == null || req.address().isBlank()) {
                throw new BadRequestException("Dirección requerida para envío");
            }
            if (req.city() == null || req.city().isBlank()) {
                throw new BadRequestException("Ciudad requerida para envío");
            }

            // F014: Validar que la ciudad está dentro de la cobertura del tenant
            // Por ahora solo acepta Bogotá y sus variantes
            String normalizedCity = req.city().trim()
                    .replaceAll("\\s+D\\.C\\.?$", "")
                    .replaceAll("á", "a")
                    .replaceAll("Á", "A")
                    .toLowerCase()
                    .trim();
            if (!normalizedCity.equals("bogota")) {
                throw new BusinessRuleException("Por ahora solo aceptamos envíos a Bogotá");
            }
        }

        // 5. Re-validar stock y re-calcular precios desde BD
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.items()) {
            Product product = productRepository.findById(tenantId, cartItem.productId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + cartItem.productName()));

            if (product.status() != ProductStatus.ACTIVE) {
                throw new BusinessRuleException("Producto no disponible: " + product.name());
            }

            if (cartItem.quantity() > product.stock()) {
                throw new BusinessRuleException(
                        "Stock insuficiente para '" + product.name() + "'. Disponible: " + product.stock());
            }

            // Usar precio actual de BD, no del carrito
            orderItems.add(new OrderItem(
                    product.productId(),
                    product.name(),
                    product.price(),
                    cartItem.quantity(),
                    cartItem.imageUrl(),
                    cartItem.variantId(),
                    cartItem.variantName()));
        }

        // 6. Calcular totales (F031: shipping + descuento manual)
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Obtener tenant para configuración de envío y descuento
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado"));

        // Calcular costo de envío
        boolean freeShippingApplied = false;
        BigDecimal shippingCost;
        if (deliveryMethod == DeliveryMethod.PICKUP) {
            shippingCost = BigDecimal.ZERO;
        } else if (tenant.shippingConfig() == null) {
            shippingCost = BigDecimal.ZERO;
        } else {
            ShippingConfig sc = tenant.shippingConfig();
            if (sc.freeShippingThreshold() != null && subtotal.compareTo(sc.freeShippingThreshold()) >= 0) {
                shippingCost = BigDecimal.ZERO;
                freeShippingApplied = true;
            } else {
                shippingCost = sc.defaultShippingCost() != null ? sc.defaultShippingCost() : BigDecimal.ZERO;
            }
        }

        // Calcular descuento por pago manual
        BigDecimal manualDiscount = BigDecimal.ZERO;
        BigDecimal manualDiscountRate = BigDecimal.ZERO;
        ManualPaymentDiscountConfig mpd = tenant.manualPaymentDiscount();
        if (mpd != null && mpd.enabled() && mpd.applicableMethods() != null
                && mpd.applicableMethods().contains(paymentMethod.name())) {
            manualDiscountRate = mpd.discountRate();
            manualDiscount = subtotal.multiply(manualDiscountRate).setScale(0, java.math.RoundingMode.HALF_UP);
        }

        // Validar que no apliquen descuento con Wompi
        if (paymentMethod == PaymentMethod.WOMPI && manualDiscount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Descuento no aplica para pagos con Wompi");
        }

        BigDecimal total = subtotal.subtract(manualDiscount).add(shippingCost);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El total de la orden debe ser mayor a 0");
        }

        // 7. Generar orderCode legible
        String orderCode = generateOrderCode();

        // 8. Construir delivery info
        DeliveryInfo deliveryInfo = null;
        if (deliveryMethod == DeliveryMethod.SHIPPING) {
            deliveryInfo = new DeliveryInfo(
                    req.address(),
                    req.city(),
                    req.neighborhood(),
                    req.additionalInfo());
        }

        // 9. Crear orden
        Instant now = Instant.now();
        List<StatusHistory> history = new ArrayList<>();
        history.add(new StatusHistory(OrderStatus.PENDING, now, "Orden creada"));

        Order order = new Order(
                UUID.randomUUID().toString(),
                orderCode,
                tenantId,
                new Customer(req.fullName(), req.email(), req.phone()),
                orderItems,
                deliveryMethod,
                deliveryInfo,
                paymentMethod,
                PaymentStatus.PENDING,
                OrderStatus.PENDING,
                subtotal,
                shippingCost,
                total,
                manualDiscount,
                manualDiscountRate,
                freeShippingApplied,
                history,
                req.notes(),
                now,
                now);

        // 10. Si es efectivo contraentrega, descontar stock inmediatamente
        if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            for (OrderItem item : orderItems) {
                productRepository.findById(tenantId, item.productId()).ifPresent(product -> {
                    int newStock = product.stock() - item.quantity();
                    Product updated = new Product(
                            product.productId(), product.tenantId(), product.name(),
                            product.description(), product.price(), product.compareAtPrice(),
                            product.images(), product.categoryId(), product.categoryName(),
                            Math.max(0, newStock),
                            product.variants(),
                            newStock <= 0 ? ProductStatus.OUT_OF_STOCK : product.status(),
                            product.sortOrder(), product.createdAt(), Instant.now());
                    productRepository.save(updated);
                });
            }
        }

        // 11. Guardar orden
        orderRepository.save(order);

        // 12. Vaciar carrito
        cartRepository.delete(tenantId, sessionId);

        // 13. Enviar emails de forma asíncrona (F009)
        final Order savedOrder = order;
        Thread.startVirtualThread(() -> {
            try {
                tenantRepository.findById(tenantId).ifPresent(t -> {
                    emailService.sendOrderConfirmation(t, savedOrder);
                    if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
                        emailService.sendPaymentInstructions(t, savedOrder);
                    }
                });
            } catch (Exception e) {
                log.warn("Error enviando emails para orden {}: {}", savedOrder.orderCode(), e.getMessage());
            }
        });

        return order;
    }

    /**
     * Valida los campos obligatorios del request.
     */
    private void validateRequest(CreateOrderRequest req) {
        if (req.fullName() == null || req.fullName().trim().length() < 3) {
            throw new BadRequestException("Nombre completo es requerido (mín. 3 caracteres)");
        }
        if (req.email() == null || !req.email().matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new BadRequestException("Email inválido");
        }
        if (req.phone() == null || !req.phone().matches("^\\+57\\d{10}$")) {
            throw new BadRequestException("Teléfono inválido. Formato: +57XXXXXXXXXX");
        }
        if (req.deliveryMethod() == null || req.deliveryMethod().isBlank()) {
            throw new BadRequestException("Método de entrega es requerido");
        }
        if (req.paymentMethod() == null || req.paymentMethod().isBlank()) {
            throw new BadRequestException("Método de pago es requerido");
        }
    }

    /**
     * Genera un código de orden legible: ORD-XXXX-XXXX (alfanumérico).
     */
    private String generateOrderCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("ORD-");
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++)
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        sb.append('-');
        for (int i = 0; i < 4; i++)
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }
}
