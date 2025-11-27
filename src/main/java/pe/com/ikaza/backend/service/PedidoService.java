package pe.com.ikaza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.ItemPedidoRequest;
import pe.com.ikaza.backend.dto.request.PedidoRequest;
import pe.com.ikaza.backend.dto.response.ItemDetalleResponse;
import pe.com.ikaza.backend.dto.response.PedidoDetalleResponse;
import pe.com.ikaza.backend.dto.response.PedidoResponse;
import pe.com.ikaza.backend.entity.Pedido;
import pe.com.ikaza.backend.entity.Producto;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.entity.Pago;
import pe.com.ikaza.backend.entity.Cliente;
import pe.com.ikaza.backend.entity.DetallePedido;
import pe.com.ikaza.backend.entity.HistorialEstadoPedido;
import pe.com.ikaza.backend.enums.EstadoPago;
import pe.com.ikaza.backend.enums.EstadoPedido;
import pe.com.ikaza.backend.enums.MetodoPago;
import pe.com.ikaza.backend.enums.ResultadoPago;
import pe.com.ikaza.backend.repository.ClienteRepository;
import pe.com.ikaza.backend.repository.DetallePedidoRepository;
import pe.com.ikaza.backend.repository.HistorialEstadoPedidoRepository;
import pe.com.ikaza.backend.repository.PagoRepository;
import pe.com.ikaza.backend.repository.PedidoRepository;
import pe.com.ikaza.backend.repository.ProductoRepository;
import pe.com.ikaza.backend.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsabilidades:
 * - Orquestación del proceso de pedidos
 * - Gestión del ciclo de vida de pedidos
 * - Coordinación entre inventario, pagos y notificaciones
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PedidoService {

    // Repositorios
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PagoRepository pagoRepository;
    private final ProductoRepository productoRepository;
    private final HistorialEstadoPedidoRepository historialRepository;

    // Servicios
    private final InventarioService inventarioService;
    private final ProcesadorPagoService procesadorPagoService;
    private final MercadoPagoService mercadoPagoService;
    private final EmailService emailService;

    // ==================== MÉTODOS PRINCIPALES ====================

    /**
     * FLUJO ASÍNCRONO: Para MercadoPago
     * 1. Valida stock
     * 2. Crea pedido preliminar
     * 3. Reserva stock
     * 4. Genera URL de pago
     * 5. Retorna URL para redirección
     */
    @Transactional
    public PedidoResponse procesarPedidoMercadoPago(
            PedidoRequest request,
            Integer idUsuario,
            String emailUsuario) {

        try {
            log.info("Iniciando pedido MERCADO_PAGO para usuario: {}", idUsuario);

            // 1. Validar stock
            validarStock(request.getCartItems());

            // 2. Crear pedido preliminar (estado PENDIENTE)
            Pedido pedido = crearPedidoPreliminar(request, idUsuario);

            // 3. Crear detalles del pedido
            crearDetallesPedido(pedido, request.getCartItems());

            // 4. Reservar stock temporalmente
            reservarStock(pedido.getIdPedido(), request.getCartItems());

            // 5. Registrar historial
            registrarCambioEstado(pedido, null, EstadoPedido.PENDIENTE);

            // 6. Procesar pago con Mercado Pago
            ResultadoPago resultadoPago = procesadorPagoService.procesarPago(
                    request, idUsuario, pedido.getIdPedido());

            if (!resultadoPago.isExitoso()) {
                throw new RuntimeException("Error al procesar el pago");
            }

            // 7. Actualizar pedido con datos de MP
            pedido.setTransaccionId(resultadoPago.getTransaccionId());
            pedidoRepository.save(pedido);

            log.info("Pedido MercadoPago creado: {}", pedido.getNumeroPedido());

            // ✅ CORRECCIÓN: Construir respuesta con todos los datos necesarios
            return PedidoResponse.builder()
                    .success(true)
                    .mensaje("Pedido creado exitosamente")
                    .pedidoId(pedido.getIdPedido())
                    .numeroPedido(pedido.getNumeroPedido())
                    .total(pedido.getTotal())
                    .subtotal(pedido.getSubtotal())
                    .fechaPedido(pedido.getFechaPedido())
                    .estado(pedido.getEstado().name())
                    .estadoPago(pedido.getEstadoPago().name())
                    .metodoPago(pedido.getMetodoPago().name())
                    // Datos de Mercado Pago
                    .transaccionId(resultadoPago.getTransaccionId()) // preference_id
                    .redirectionUrl(resultadoPago.getUrlRedireccion()) // init_point
                    .requiresRedirection(resultadoPago.isRequiereRedireccion())
                    .datosJson(resultadoPago.getDatosJson())
                    .build();

        } catch (Exception e) {
            log.error("Error al procesar pedido MercadoPago", e);

            return PedidoResponse.builder()
                    .success(false)
                    .mensaje("Error al procesar pedido: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Confirma un pago de MercadoPago después del retorno del usuario
     */
    @Transactional
    public PedidoResponse confirmarPagoMercadoPago(Long pedidoId, String paymentId, String status, Integer idUsuario) {
        try {
            log.info("Confirmando pago MercadoPago. PedidoId: {}, PaymentId: {}", pedidoId, paymentId);

            Pedido pedido = validarPedidoUsuario(pedidoId, idUsuario);

            // Consultar estado real en MercadoPago
            JsonNode paymentInfo = mercadoPagoService.consultarPago(paymentId);
            String estadoMP = paymentInfo.get("status").asText();

            // Actualizar pedido según estado
            actualizarPedidoSegunEstadoMP(pedido, estadoMP, paymentId, paymentInfo);

            // Enviar email si fue aprobado
            if ("approved".equals(estadoMP)) {
                String email = obtenerEmailUsuario(idUsuario);
                emailService.enviarConfirmacionPedido(pedido, email);
            }

            return construirRespuestaConfirmacion(pedido, estadoMP);

        } catch (Exception e) {
            log.error("Error al confirmar pago MercadoPago", e);
            return PedidoResponse.error("Error al confirmar el pago: " + e.getMessage());
        }
    }

    /**
     * Webhook de MercadoPago
     */
    @Transactional
    public void procesarWebhookMercadoPago(String paymentId, String action) {
        try {
            log.info("Procesando webhook MercadoPago. PaymentId: {}", paymentId);

            Pago pago = pagoRepository.findByTransaccionExternaId(paymentId).orElse(null);
            if (pago == null) {
                log.warn("No se encontró pago para paymentId: {}", paymentId);
                return;
            }

            JsonNode paymentInfo = mercadoPagoService.consultarPago(paymentId);
            String estadoMP = paymentInfo.get("status").asText();

            Pedido pedido = pago.getPedido();
            actualizarPedidoSegunEstadoMP(pedido, estadoMP, paymentId, paymentInfo);

            log.info("Webhook procesado exitosamente");

        } catch (Exception e) {
            log.error("Error al procesar webhook", e);
        }
    }

    // ==================== MÉTODOS DE CONSULTA ====================

    public PedidoDetalleResponse getPedidoDetalleByIdAndUser(Long idPedido, Integer idUsuario) {
        Pedido pedido = validarPedidoUsuario(idPedido, idUsuario);

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoIdWithProducto(idPedido);
        Pago pago = pagoRepository.findByPedido_IdPedido(idPedido).orElse(null);

        String telefonoContacto = clienteRepository.findByUsuarioIdUsuario(idUsuario)
                .map(Cliente::getTelefono)
                .orElse("N/A");

        List<ItemDetalleResponse> detallesResponse = detalles.stream()
                .map(this::mapearDetalle)
                .collect(Collectors.toList());

        return PedidoDetalleResponse.detalleBuilder()
                .success(true)
                .pedidoId(pedido.getIdPedido())
                .numeroPedido(pedido.getNumeroPedido())
                .estadoPedido(pedido.getEstado().name())
                .estadoPago(pedido.getEstadoPago().name())
                .metodoPago(pedido.getMetodoPago().name())
                .total(pedido.getTotal())
                .subtotal(pedido.getSubtotal())
                .fechaPedido(pedido.getFechaPedido())
                .transaccionId(pedido.getTransaccionId())
                .detalles(detallesResponse)
                .ultimos4DigitosTarjeta(pago != null ? pago.getUltimos4Digitos() : null)
                .tipoTarjeta(pago != null ? pago.getTipoTarjeta() : null)
                .bancoEmisor(pago != null ? pago.getBancoEmisor() : null)
                .fechaPago(pago != null ? pago.getFechaPago() : null)
                .direccionEnvioCompleta("Dirección")
                .telefonoContacto(telefonoContacto)
                .mensaje("Detalle de pedido obtenido")
                .build();
    }

    public List<Pedido> getPedidosByUserId(Integer idUsuario) {
        return pedidoRepository.findByIdUsuario(idUsuario);
    }

    /**
     * CAMBIO: Método ajustado para aceptar Integer directamente, como sugiere el
     * usuario,
     * asumiendo que el repositorio espera este tipo.
     */
    public int contarDetallesPedido(Integer idPedido) {
        // Se asume que el idPedido ya es un Integer válido y se llama al repositorio.
        // El .intValue() final es por si el repositorio devuelve Long.
        return detallePedidoRepository.countByPedidoId(idPedido).intValue();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Crea un pedido preliminar para MercadoPago
     */
    private Pedido crearPedidoPreliminar(PedidoRequest request, Integer idUsuario) {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(generarNumeroPedido());
        pedido.setIdUsuario(idUsuario);
        pedido.setTotal(request.getTotal());
        pedido.setSubtotal(request.getSubtotal());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        pedido.setMetodoPago(MetodoPago.MERCADO_PAGO);
        pedido.setFechaPedido(LocalDateTime.now());

        return pedidoRepository.save(pedido);
    }

    /**
     * Crea los detalles del pedido
     */
    private void crearDetallesPedido(Pedido pedido, List<ItemPedidoRequest> items) {
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.setColorSeleccionado(item.getColor());
            detalle.setTallaSeleccionada(item.getTalla());
            detalle.calcularSubtotal();

            detallePedidoRepository.save(detalle);
        }
        log.info("Detalles del pedido creados: {} items", items.size());
    }

    /**
     * Registra el historial inicial del pedido
     */
    private void registrarHistorialInicial(Pedido pedido) {
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(pedido.getEstado());
        historial.setFechaCambio(LocalDateTime.now());
        historialRepository.save(historial);
    }

    /**
     * Actualiza el pedido según el estado de MercadoPago
     */
    private void actualizarPedidoSegunEstadoMP(Pedido pedido, String estadoMP, String paymentId, JsonNode paymentInfo) {
        EstadoPedido estadoAnterior = pedido.getEstado();
        EstadoPago estadoPagoAnterior = pedido.getEstadoPago();

        Usuario usuario = usuarioRepository.findById(pedido.getIdUsuario()).orElse(null);
        List<ItemPedidoRequest> items = convertirDetallesAItems(pedido);

        switch (estadoMP) {
            case "approved":
                // Liberar stock reservado y confirmar venta
                inventarioService.liberarStockReservado(items, pedido.getIdPedido(), usuario);
                inventarioService.confirmarVenta(items, pedido.getIdPedido(), usuario);

                pedido.setEstado(EstadoPedido.CONFIRMADO);
                pedido.setEstadoPago(EstadoPago.APROBADO);
                pedido.setFechaPago(LocalDateTime.now());
                break;

            case "pending":
            case "in_process":
                pedido.setEstado(EstadoPedido.PENDIENTE);
                pedido.setEstadoPago(EstadoPago.PROCESANDO);
                break;

            case "rejected":
            case "cancelled":
                // Liberar stock reservado
                inventarioService.liberarStockReservado(items, pedido.getIdPedido(), usuario);

                pedido.setEstado(EstadoPedido.CANCELADO);
                pedido.setEstadoPago(EstadoPago.RECHAZADO);
                break;

            case "refunded":
                // Devolver stock
                inventarioService.devolverStock(items, pedido.getIdPedido(), usuario);

                pedido.setEstado(EstadoPedido.DEVUELTO);
                pedido.setEstadoPago(EstadoPago.REEMBOLSADO);
                break;

            default:
                log.warn("Estado de MercadoPago no manejado: {}", estadoMP);
                return;
        }

        pedido.setTransaccionId(paymentId);
        pedidoRepository.save(pedido);

        // Actualizar pago
        actualizarRegistroPago(pedido, paymentId, paymentInfo);

        // Registrar cambio de estado
        if (estadoAnterior != pedido.getEstado()) {
            registrarCambioEstado(pedido, estadoAnterior, pedido.getEstado());
        }

        log.info("Pedido {} actualizado: {} -> {}, Pago: {} -> {}",
                pedido.getNumeroPedido(), estadoAnterior, pedido.getEstado(),
                estadoPagoAnterior, pedido.getEstadoPago());
    }

    /**
     * Actualiza el registro de pago
     */
    private void actualizarRegistroPago(Pedido pedido, String paymentId, JsonNode paymentInfo) {
        Pago pago = pagoRepository.findByPedido_IdPedido(pedido.getIdPedido()).orElse(null);

        if (pago == null) {
            pago = new Pago();
            pago.setPedido(pedido);
            pago.setMonto(pedido.getTotal());
            pago.setMetodoUsado(pedido.getMetodoPago());
        }

        pago.setEstado(pedido.getEstadoPago());
        pago.setTransaccionExternaId(paymentId);
        pago.setDatosPasarelaJson(paymentInfo.toString());

        if (pedido.getEstadoPago() == EstadoPago.APROBADO) {
            pago.setFechaPago(LocalDateTime.now());
        }

        pagoRepository.save(pago);
    }

    /**
     * Registra un cambio de estado en el historial
     */
    private void registrarCambioEstado(Pedido pedido, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo) {
        HistorialEstadoPedido historial = new HistorialEstadoPedido();
        historial.setPedido(pedido);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setFechaCambio(LocalDateTime.now());
        historialRepository.save(historial);
    }

    /**
     * Convierte detalles del pedido a items de request
     */
    private List<ItemPedidoRequest> convertirDetallesAItems(Pedido pedido) {
        List<DetallePedido> detalles = detallePedidoRepository.findByPedido_IdPedido(pedido.getIdPedido());

        return detalles.stream()
                .map(d -> {
                    ItemPedidoRequest item = new ItemPedidoRequest();
                    item.setIdProducto(d.getProducto().getIdProducto());
                    item.setCantidad(d.getCantidad());
                    item.setPrecioUnitario(d.getPrecioUnitario());
                    item.setColor(d.getColorSeleccionado());
                    item.setTalla(d.getTallaSeleccionada());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * Valida que el pedido existe y pertenece al usuario
     */
    private Pedido validarPedidoUsuario(Long idPedido, Integer idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!pedido.getIdUsuario().equals(idUsuario)) {
            throw new RuntimeException("El pedido no pertenece al usuario");
        }

        return pedido;
    }

    /**
     * Genera un número de pedido único
     */
    private String generarNumeroPedido() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        String numeroPedido = "PED-" + fecha + "-" + random;

        while (pedidoRepository.existsByNumeroPedido(numeroPedido)) {
            random = String.format("%06d", (int) (Math.random() * 1000000));
            numeroPedido = "PED-" + fecha + "-" + random;
        }

        return numeroPedido;
    }

    /**
     * Obtiene el email del usuario
     */
    private String obtenerEmailUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(Usuario::getEmail)
                .orElse("usuario@example.com");
    }

    /**
     * Mapea detalle a response
     */
    private ItemDetalleResponse mapearDetalle(DetallePedido detalle) {
        return ItemDetalleResponse.builder()
                .idProducto(detalle.getProducto().getIdProducto())
                .nombreProducto(detalle.getProducto().getNombreProducto())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .colorSeleccionado(detalle.getColorSeleccionado())
                .tallaSeleccionada(detalle.getTallaSeleccionada())
                .build();
    }

    /**
     * Construye respuesta de confirmación
     */
    private PedidoResponse construirRespuestaConfirmacion(Pedido pedido, String estadoMP) {
        return PedidoResponse.builder()
                .success(true)
                .pedidoId(pedido.getIdPedido())
                .numeroPedido(pedido.getNumeroPedido())
                .estado(pedido.getEstado().name())
                .estadoPago(pedido.getEstadoPago().name())
                .mensaje(obtenerMensajeSegunEstado(estadoMP))
                .build();
    }

    /**
     * Obtiene mensaje según estado de MercadoPago
     */
    private String obtenerMensajeSegunEstado(String estadoMP) {
        switch (estadoMP) {
            case "approved":
                return "¡Pago aprobado! Tu pedido ha sido confirmado.";
            case "pending":
                return "Tu pago está pendiente de confirmación.";
            case "in_process":
                return "Tu pago está siendo procesado.";
            case "rejected":
                return "Tu pago fue rechazado. Por favor, intenta con otro método de pago.";
            case "cancelled":
                return "El pago fue cancelado.";
            default:
                return "Estado del pago: " + estadoMP;
        }
    }

    // ==================== TAREA PROGRAMADA ====================

    /**
     * Limpia pedidos preliminares antiguos (ejecuta cada 30 minutos)
     * Libera stock reservado de pedidos abandonados
     */
    @Scheduled(initialDelay = 1800000, fixedRate = 1800000)
    @Transactional
    public void limpiarPedidosPreliminaresAntiguos() {
        log.info("Iniciando limpieza de pedidos preliminares antiguos...");

        LocalDateTime umbral = LocalDateTime.now().minus(1, ChronoUnit.HOURS);

        List<Pedido> pedidosAntiguos = pedidoRepository.findByEstadoAndMetodoPagoAndFechaPedidoBefore(
                EstadoPedido.PENDIENTE,
                MetodoPago.MERCADO_PAGO,
                umbral);

        int pedidosLimpiados = 0;

        for (Pedido pedido : pedidosAntiguos) {
            try {
                // Liberar stock reservado
                List<ItemPedidoRequest> items = convertirDetallesAItems(pedido);
                Usuario usuario = usuarioRepository.findById(pedido.getIdUsuario()).orElse(null);
                inventarioService.liberarStockReservado(items, pedido.getIdPedido(), usuario);

                // Eliminar registros relacionados
                detallePedidoRepository.deleteByPedido_IdPedido(pedido.getIdPedido());
                historialRepository.deleteByPedido_IdPedido(pedido.getIdPedido());

                // Eliminar pedido
                pedidoRepository.delete(pedido);
                pedidosLimpiados++;

                log.info("Pedido {} eliminado (sin pago por más de 1 hora)", pedido.getNumeroPedido());

            } catch (Exception e) {
                log.error("Error al limpiar pedido {}: {}", pedido.getNumeroPedido(), e.getMessage());
            }
        }

        log.info("Limpieza finalizada. Pedidos eliminados: {}", pedidosLimpiados);
    }

    /**
     * Valida que hay stock suficiente para todos los items
     */
    private void validarStock(List<ItemPedidoRequest> items) {
        inventarioService.validarStockDisponible(items);
    }

    /**
     * Reserva stock temporalmente para el pedido
     */
    private void reservarStock(Long pedidoId, List<ItemPedidoRequest> items) {
        log.info("Reservando stock para pedido: {}", pedidoId);

        try {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

            Usuario usuario = usuarioRepository.findById(pedido.getIdUsuario()).orElse(null);

            inventarioService.reservarStock(items, pedidoId, usuario);

            log.info("Stock reservado exitosamente");

        } catch (Exception e) {
            log.error("Error al reservar stock para pedido {}: {}", pedidoId, e.getMessage());
            throw new RuntimeException("Error al reservar stock: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS DE CONSULTA ====================

    /**
     * Obtener la lista de pedidos de un usuario y mapearlos a PedidoResponse.
     * Utilizado por el controlador para /api/pedidos/mis-pedidos.
     */
    public List<PedidoResponse> getMisPedidosResponse(Integer idUsuario) {
        // 1. Obtener los pedidos del usuario
        List<Pedido> pedidos = getPedidosByUserId(idUsuario);

        // 2. Mapear cada Pedido a PedidoResponse (incluyendo la cantidad de productos)
        return pedidos.stream()
                .map(this::mapearPedidoAResponse)
                .collect(Collectors.toList());
    }

    /**
	 * Mapea un objeto Pedido a PedidoResponse, incluyendo el conteo de productos.
	 */
	private PedidoResponse mapearPedidoAResponse(Pedido pedido) {
		// CORRECCIÓN: Se realiza el casting de Long a Integer antes de llamar 
		// al método contarDetallesPedido, ya que espera Integer.
		int cantidadProductos = contarDetallesPedido(pedido.getIdPedido().intValue());
		return PedidoResponse.builder()
				.success(true)
				.pedidoId(pedido.getIdPedido())
				.numeroPedido(pedido.getNumeroPedido())
				.estado(pedido.getEstado().name()) // Se mapea el estado del pedido
				.estadoPago(pedido.getEstadoPago().name())
				.metodoPago(pedido.getMetodoPago().name())
				.total(pedido.getTotal())
				.subtotal(pedido.getSubtotal())
				.fechaPedido(pedido.getFechaPedido())
				.mensaje("Pedido obtenido")
				.cantidadProductos(cantidadProductos) // Se incluye la cantidad de productos
				.build();
	}
}