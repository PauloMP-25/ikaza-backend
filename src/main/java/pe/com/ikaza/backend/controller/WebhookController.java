package pe.com.ikaza.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.UsuarioRepository;
import pe.com.ikaza.backend.dto.request.PreferenciaMercadoPagoRequest;
import pe.com.ikaza.backend.dto.request.ItemPedidoRequest;
import pe.com.ikaza.backend.dto.response.PreferenciaMercadoPagoResponse;
import pe.com.ikaza.backend.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador para manejar webhooks y endpoints de pasarelas de pago.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.frontend.url:http://localhost:4200}")
public class WebhookController {

    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;

    // ==================== MERCADO PAGO ====================

    /**
     * Endpoint para crear preferencia de Mercado Pago desde el frontend.
     * Este endpoint:
     * 1. Valida stock
     * 2. Crea pedido preliminar
     * 3. Reserva stock temporalmente
     * 4. Genera URL de pago
     * POST /api/webhooks/mercadopago/create-preference
     */
    @PostMapping("/mercadopago/create-preference")
    public ResponseEntity<?> crearPreferenciaMercadoPago(
            @RequestBody PreferenciaMercadoPagoRequest request,
            Authentication authentication) {
        try {
            log.info("Creando preferencia de Mercado Pago con {} items", request.getItems().size());
            Usuario usuario = extraerUsuario(authentication);
            Integer idUsuario = usuario.getIdUsuario();
            String emailUsuario = usuario.getEmail();

            // Convertir items de MercadoPago a ItemPedidoRequest
            List<ItemPedidoRequest> items = request.getItems();

            // Calcular totales
            BigDecimal total = items.stream()
                    .map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotal = total.multiply(BigDecimal.valueOf(0.82));

            // Crear pedido preliminar con reserva de stock
            log.info("Creando pedido preliminar y reservando stock...");

            // Simular PedidoRequest para usar el método refactorizado
            pe.com.ikaza.backend.dto.request.PedidoRequest pedidoRequest = new pe.com.ikaza.backend.dto.request.PedidoRequest();
            pedidoRequest.setMetodoPago("MERCADO_PAGO");
            pedidoRequest.setCartItems(items);
            pedidoRequest.setTotal(total);
            pedidoRequest.setSubtotal(subtotal);
            pedidoRequest.setEmail(emailUsuario);

            // Usar el flujo asíncrono de MercadoPago
            pe.com.ikaza.backend.dto.response.PedidoResponse pedidoResponse = pedidoService
                    .procesarPedidoMercadoPago(pedidoRequest, idUsuario, emailUsuario);

            if (!pedidoResponse.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Error al crear pedido: " + pedidoResponse.getMensaje()));
            }

            // CORRECCIÓN: Construir respuesta con los nombres correctos
            PreferenciaMercadoPagoResponse response = PreferenciaMercadoPagoResponse.builder()
                    .preferenceId(pedidoResponse.getTransaccionId()) // Este viene del MP response
                    .preferenceUrl(pedidoResponse.getRedirectionUrl()) // Este también
                    .pedidoId(pedidoResponse.getPedidoId()) // Este es tu ID interno
                    .build();

            log.info("✅ Preferencia creada: {} para pedido: {} (Stock reservado)",
                    response.getPreferenceId(), response.getPedidoId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error creando preferencia de Mercado Pago", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Error al crear preferencia: " + e.getMessage()));
        }
    }

    /**
     * Webhook de Mercado Pago para notificaciones asíncronas.
     * Este webhook actualiza el estado del pedido según la notificación
     * POST /api/webhooks/mercadopago
     */
    @PostMapping("/mercadopago")
    public ResponseEntity<String> webhookMercadoPago(
            @RequestBody String payload,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id) {

        try {
            log.info("Webhook recibido de Mercado Pago. Type: {}, ID: {}", type, id);
            log.debug("Payload completo: {}", payload);

            JsonNode jsonNode = objectMapper.readTree(payload);

            if ("payment".equals(type)) {
                procesarNotificacionPago(id, jsonNode);
            } else if ("merchant_order".equals(type)) {
                procesarNotificacionOrden(id, jsonNode);
            } else {
                log.warn("Tipo de notificación no manejado: {}", type);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error procesando webhook de Mercado Pago", e);
            return ResponseEntity.ok("ERROR");
        }
    }

    /**
     * Procesa notificación de pago desde MercadoPago
     */
    private void procesarNotificacionPago(String paymentId, JsonNode payload) {
        try {
            log.info("Procesando notificación de pago: {}", paymentId);
            String action = payload.has("action") ? payload.get("action").asText() : "payment.updated";

            pedidoService.procesarWebhookMercadoPago(paymentId, action);

            log.info("Notificación de pago procesada exitosamente");
        } catch (Exception e) {
            log.error("Error al procesar notificación de pago", e);
        }
    }

    /**
     * Procesa notificación de orden desde MercadoPago
     */
    private void procesarNotificacionOrden(String orderId, JsonNode payload) {
        log.info("Procesando notificación de orden: {}", orderId);
        // Implementar
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Extrae el ID del usuario del JWT utilizando el email.
     */
    private Usuario extraerUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
    // Clase auxiliar para respuestas de error
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String error;
    }
}
