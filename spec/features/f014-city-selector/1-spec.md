# F014 — City Validation (Backend)

## Propósito

Validar que la ciudad seleccionada por el comprador está dentro de la cobertura del tenant. Las ciudades disponibles se almacenan en la configuración del tenant (F002).

## Endpoints

No requiere endpoint dedicado. La validación se hace dentro del flujo de checkout (F005).

## Lógica

### En CreateOrderUseCaseImpl (F005)
- Si `deliveryMethod = SHIPPING`:
  1. Obtener `tenant.cities` del TenantRepository.
  2. Verificar que `deliveryInfo.city` está en la lista.
  3. Si no está → `BusinessRuleException("La ciudad seleccionada no está en cobertura")`.

## Criterios de Aceptación

- [ ] Orden con ciudad fuera de cobertura retorna `422`.
- [ ] Orden con ciudad válida se crea correctamente.
- [ ] Orden con `PICKUP` no valida ciudad.

## Notas

- En MVP no hay endpoint dedicado de ciudades. Se obtienen del tenant config.
- A futuro: costo de envío variable por ciudad, disponibilidad de productos por ciudad.
