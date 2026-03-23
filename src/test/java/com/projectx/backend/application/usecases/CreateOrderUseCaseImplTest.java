package com.projectx.backend.application.usecases;

import com.projectx.backend.application.dto.CreateOrderRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.CartRepository;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

        @Mock
        private CartRepository cartRepository;
        @Mock
        private OrderRepository orderRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private TenantRepository tenantRepository;
        @Mock
        private EmailService emailService;

        private CreateOrderUseCaseImpl useCase;

        private static final String TENANT = "idoneo";
        private static final String SESSION = "session-1";

        private CreateOrderRequest validRequest(String paymentMethod) {
                return new CreateOrderRequest(
                                "Juan Pérez", "juan@email.com", "+573001234567",
                                "SHIPPING", "Calle 123", "Bogotá", "Chapinero", "Apto 301",
                                paymentMethod, "Sin cebolla");
        }

        private Product activeProduct(String id, String name, BigDecimal price, int stock) {
                return new Product(id, TENANT, name, "desc", price, null, List.of("img.jpg"),
                                "cat-1", "Alimentos", stock, List.of(), ProductStatus.ACTIVE, 0,
                                Instant.now(), Instant.now());
        }

        private Cart cartWithItems() {
                CartItem item = new CartItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
                return Cart.empty(SESSION, TENANT).withItems(List.of(item));
        }

        private Tenant tenantWithCities(List<String> cities) {
                return new Tenant(TENANT, "Test Tienda", "desc", "logo.png", null, "banner.png",
                                new TenantColors("#000", "#666", "#ff6600", "#fff", "#1a1a1a"),
                                "Inter", List.of(), cities, "573001234567", "test@test.com",
                                "3001234567", "Calle 1", "Lun-Vie 9-6", null, true,
                                null, null, null, null, Instant.now(), Instant.now());
        }

        @BeforeEach
        void setUp() {
                useCase = new CreateOrderUseCaseImpl(cartRepository, orderRepository, productRepository,
                                tenantRepository, emailService);
        }

        @Test
        void debeCrearOrdenExitosamente() {
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(tenantRepository.findById(TENANT))
                                .thenReturn(Optional.of(tenantWithCities(List.of("Bogotá", "Medellín"))));
                when(productRepository.findById(TENANT, "prod-1"))
                                .thenReturn(Optional
                                                .of(activeProduct("prod-1", "Granola", BigDecimal.valueOf(18500), 10)));

                Order order = useCase.execute(TENANT, SESSION, validRequest("WOMPI"));

                assertNotNull(order.orderId());
                assertTrue(order.orderCode().startsWith("ORD-"));
                assertEquals(TENANT, order.tenantId());
                assertEquals(OrderStatus.PENDING, order.orderStatus());
                assertEquals(PaymentStatus.PENDING, order.paymentStatus());
                assertEquals(PaymentMethod.WOMPI, order.paymentMethod());
                assertEquals(DeliveryMethod.SHIPPING, order.deliveryMethod());
                assertNotNull(order.deliveryInfo());
                assertEquals("Bogotá", order.deliveryInfo().city());
                assertEquals(1, order.items().size());
                assertEquals(BigDecimal.valueOf(37000), order.total());
                assertEquals(1, order.statusHistory().size());
                verify(orderRepository).save(any(Order.class));
                verify(cartRepository).delete(TENANT, SESSION);
        }

        @Test
        void debeDescontarStockConEfectivoContraentrega() {
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenantWithCities(List.of("Bogotá"))));
                Product p = activeProduct("prod-1", "Granola", BigDecimal.valueOf(18500), 10);
                when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

                useCase.execute(TENANT, SESSION, validRequest("CASH_ON_DELIVERY"));

                // save del producto actualizado + save de orden
                verify(productRepository).save(any(Product.class));
                verify(orderRepository).save(any(Order.class));
        }

        @Test
        void debeFallarConCarritoVacio() {
                Cart empty = Cart.empty(SESSION, TENANT);
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(empty));

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, validRequest("WOMPI")));
        }

        @Test
        void debeFallarConCarritoInexistente() {
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, validRequest("WOMPI")));
        }

        @Test
        void debeFallarConStockInsuficiente() {
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenantWithCities(List.of("Bogotá"))));
                when(productRepository.findById(TENANT, "prod-1"))
                                .thenReturn(Optional
                                                .of(activeProduct("prod-1", "Granola", BigDecimal.valueOf(18500), 1)));

                assertThrows(BusinessRuleException.class,
                                () -> useCase.execute(TENANT, SESSION, validRequest("WOMPI")));
        }

        @Test
        void debeFallarConNombreCorto() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Jo", "j@e.co", "+573001234567", "PICKUP", null, null, null, null, "WOMPI", null);

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, req));
        }

        @Test
        void debeFallarConEmailInvalido() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Juan Pérez", "no-email", "+573001234567", "PICKUP", null, null, null, null, "WOMPI",
                                null);

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, req));
        }

        @Test
        void debeFallarConTelefonoInvalido() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Juan Pérez", "j@e.co", "123456", "PICKUP", null, null, null, null, "WOMPI", null);

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, req));
        }

        @Test
        void debeFallarConMetodoEntregaInvalido() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Juan Pérez", "j@e.co", "+573001234567", "DRONE", null, null, null, null, "WOMPI",
                                null);

                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, req));
        }

        @Test
        void debeFallarConEnvioSinDireccion() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Juan Pérez", "j@e.co", "+573001234567", "SHIPPING", null, "Bogotá", null, null,
                                "WOMPI", null);

                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));

                assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, SESSION, req));
        }

        @Test
        void debeCrearOrdenPickupSinDireccion() {
                CreateOrderRequest req = new CreateOrderRequest(
                                "Juan Pérez", "j@e.co", "+573001234567", "PICKUP", null, null, null, null, "WOMPI",
                                null);

                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(productRepository.findById(TENANT, "prod-1"))
                                .thenReturn(Optional
                                                .of(activeProduct("prod-1", "Granola", BigDecimal.valueOf(18500), 10)));
                when(tenantRepository.findById(TENANT))
                                .thenReturn(Optional.of(tenantWithCities(List.of("Bogotá"))));

                Order order = useCase.execute(TENANT, SESSION, req);

                assertEquals(DeliveryMethod.PICKUP, order.deliveryMethod());
                assertNull(order.deliveryInfo());
        }

        @Test
        void debeFallarConCiudadFueraDeCobertura() {
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(tenantRepository.findById(TENANT))
                                .thenReturn(Optional.of(tenantWithCities(List.of("Medellín", "Cali"))));

                assertThrows(BusinessRuleException.class,
                                () -> useCase.execute(TENANT, SESSION, validRequest("WOMPI")));
        }

        @Test
        void debeRevalidarPreciosDesdeBD() {
                // Carrito tiene precio 18500, pero BD tiene 20000
                when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItems()));
                when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(tenantWithCities(List.of("Bogotá"))));
                when(productRepository.findById(TENANT, "prod-1"))
                                .thenReturn(Optional
                                                .of(activeProduct("prod-1", "Granola", BigDecimal.valueOf(20000), 10)));

                Order order = useCase.execute(TENANT, SESSION, validRequest("WOMPI"));

                // Debe usar precio de BD (20000 x 2 = 40000)
                assertEquals(BigDecimal.valueOf(40000), order.total());
        }
}
