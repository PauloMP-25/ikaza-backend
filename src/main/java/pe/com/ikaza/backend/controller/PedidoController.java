package pe.com.ikaza.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;

import pe.com.ikaza.backend.dto.request.PedidoRequest;
import pe.com.ikaza.backend.dto.response.PedidoDetalleResponse;
import pe.com.ikaza.backend.dto.request.ConfirmarPagoMercadoPagoRequest;
import pe.com.ikaza.backend.service.MercadoPagoService;
import pe.com.ikaza.backend.repository.PedidoRepository;
import pe.com.ikaza.backend.dto.response.ConfirmarPagoResponse;
import pe.com.ikaza.backend.dto.response.PedidoResponse;
import pe.com.ikaza.backend.entity.Pedido;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.enums.EstadoPago;
import pe.com.ikaza.backend.repository.UsuarioRepository;
import pe.com.ikaza.backend.service.PedidoService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador refactorizado de Pedidos
 * Separa los flujos síncronos y asíncronos
 */
@RestController
@RequestMapping("/api/usuarios/pedidos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.frontend.url:http://localhost:4200}")
public class PedidoController {

    private final PedidoService pedidoService;
    private final MercadoPagoService mercadoPagoService;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Endpoint para crear pedidos SÍNCRONOS
     * Soporta: Transferencia Bancaria, Efectivo Contraentrega
     * NOTA: MercadoPago se maneja en WebhookController con
     * /api/webhooks/mercadopago/create-preference
     */
    @PostMapping("/crear")
    public ResponseEntity<PedidoResponse> crearPedido(
            @Valid @RequestBody PedidoRequest request,
            Authentication authentication) {

        try {
            Usuario usuario = extraerUsuario(authentication);
            Integer idUsuario = usuario.getIdUsuario();

            log.info("Procesando checkout para usuario: {} con {} items",
                    idUsuario, request.getCartItems().size());

            // Validar que el usuario del request coincida (seguridad adicional)
            if (request.getIdUsuario() != null && !request.getIdUsuario().equals(idUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(PedidoResponse.error("Usuario no autorizado"));
            }

            log.info("Nuevo pedido - Usuario: {}, Método: {}", idUsuario, request.getMetodoPago());

            // Validar que no sea MercadoPago (ese flujo va por WebhookController)
            if ("MERCADO_PAGO".equals(request.getMetodoPago())) {
                return ResponseEntity.badRequest()
                        .body(PedidoResponse.error(
                                "MercadoPago debe procesarse desde /api/webhooks/mercadopago/create-preference"));
            }

            return ResponseEntity.ok(null);

        } catch (Exception e) {
            log.error("Error al crear pedido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PedidoResponse.error("Error al procesar el pedido: " + e.getMessage()));
        }
    }

    /**
     * Confirma un pago de Mercado Pago después del retorno del usuario
     * POST /api/pedidos/{pedidoId}/confirmar-pago
     */
    @PostMapping("/{pedidoId}/confirmar-pago")
    public ResponseEntity<ConfirmarPagoResponse> confirmarPagoMercadoPago(
            @PathVariable Long pedidoId,
            @RequestBody ConfirmarPagoMercadoPagoRequest request,
            Authentication authentication) {
        try {
            log.info("🔄 Confirmando pago MP para pedido: {}, payment_id: {}, status: {}",
                    pedidoId, request.getPaymentId(), request.getStatus());

            // Validar usuario autenticado
            Usuario usuario = extraerUsuario(authentication);

            // Buscar el pedido
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

            // Verificar que el pedido pertenezca al usuario
            if (!pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
                throw new RuntimeException("No tienes permisos para este pedido");
            }

            // Consultar el pago en Mercado Pago para validar
            JsonNode pagoInfo = mercadoPagoService.consultarPago(request.getPaymentId());
            String mpStatus = pagoInfo.get("status").asText();

            log.info("📊 Estado del pago en MP: {}", mpStatus);

            // Validar que el estado coincida
            if (!"approved".equals(mpStatus)) {
                return ResponseEntity.ok(ConfirmarPagoResponse.builder()
                        .success(false)
                        .mensaje("El pago no está aprobado en Mercado Pago. Estado: " + mpStatus)
                        .build());
            }

            // Actualizar el pedido
            pedido.setEstadoPago(EstadoPago.APROBADO);
            pedido.setTransaccionId(request.getPaymentId());
            pedido.setFechaPago(LocalDateTime.now());

            pedidoRepository.save(pedido);

            log.info("✅ Pedido {} confirmado como PAGADO", pedido.getNumeroPedido());

            // TODO: Enviar email de confirmación
            // TODO: Liberar stock definitivamente (ya está reservado)

            return ResponseEntity.ok(ConfirmarPagoResponse.builder()
                    .success(true)
                    .numeroPedido(pedido.getNumeroPedido())
                    .mensaje("Pago confirmado exitosamente")
                    .build());

        } catch (Exception e) {
            log.error("❌ Error confirmando pago", e);
            return ResponseEntity.ok(ConfirmarPagoResponse.builder()
                    .success(false)
                    .mensaje("Error al confirmar el pago: " + e.getMessage())
                    .build());
        }
    }

    // ========== ENDPOINTS PARA CLIENTES ==========

    /**
     * Obtener un pedido por ID (Detalle Completo).
     * GET /api/pedidos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPedido(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Usuario usuario = extraerUsuario(authentication);
            Integer idUsuario = usuario.getIdUsuario();
            PedidoDetalleResponse response = pedidoService.getPedidoDetalleByIdAndUser(id, idUsuario);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al obtener pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(PedidoResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al obtener pedido", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PedidoResponse.error("Error interno: " + e.getMessage()));
        }
    }

    /**
     * Obtener todos los pedidos del usuario autenticado.
     * GET /api/pedidos/mis-pedidos
     * * REFACTORIZACIÓN: Se delega la lógica de mapeo y obtención al PedidoService.
     */
    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponse>> obtenerMisPedidos(Authentication authentication) {
        try {
            // 1. Extraer información del usuario del JWT
            Usuario usuario = extraerUsuario(authentication);
            Integer idUsuario = usuario.getIdUsuario();

            // 2. Delegar la obtención y mapeo al servicio
            List<PedidoResponse> responseList = pedidoService.getMisPedidosResponse(idUsuario);

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error al obtener pedidos", e);
            // Usamos el error de PedidoResponse para mantener la consistencia en el cuerpo
            // de la respuesta de error.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(PedidoResponse.error("Error al obtener los pedidos: " + e.getMessage())));
        }
    }

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
}
