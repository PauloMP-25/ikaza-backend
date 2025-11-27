package pe.com.ikaza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.com.ikaza.backend.dto.request.PedidoRequest;
import pe.com.ikaza.backend.enums.EstadoPago;
import pe.com.ikaza.backend.enums.MetodoPago;
import pe.com.ikaza.backend.enums.ResultadoPago;

/**
 * Servicio que maneja la estrategia de procesamiento de pagos
 * Separa la lógica de pagos del PedidoService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcesadorPagoService {

    private final MercadoPagoService mercadoPagoService;

    /**
     * Procesa el pago según el método seleccionado
     */
    public ResultadoPago procesarPago(PedidoRequest request, Integer idUsuario, Long pedidoId) {
        log.info("Procesando pago con método: {}", request.getMetodoPago());

        // Según método de pago
        switch (request.getMetodoPago()) {            
            case "MERCADO_PAGO":
                return procesarPagoMercadoPago(request, pedidoId);
            
            case "TRANSFERENCIA_BANCARIA":
            case "EFECTIVO_CONTRAENTREGA":
                return procesarPagoDiferido(request.getMetodoPago());
            
            default:
                throw new RuntimeException("Método de pago no soportado: " + request.getMetodoPago());
        }
    }


    /**
     * Procesa pago con Mercado Pago (asíncrono)
     * Devuelve URL de redirección, el pedido ya debe estar creado
     */
    /**
 * Procesa pago con Mercado Pago (asíncrono)
 * Devuelve URL de redirección, el pedido ya debe estar creado
 */
private ResultadoPago procesarPagoMercadoPago(PedidoRequest request, Long pedidoId) {
    log.info("Creando preferencia de Mercado Pago para pedido: {}", pedidoId);

    JsonNode respuestaMP = mercadoPagoService.crearPreferencia(
            request.getCartItems(),
            pedidoId);

    // ✅ CORRECCIÓN: Extraer los campos correctos de la respuesta de MP
    String preferenceId = respuestaMP.get("id").asText();
    String initPoint = respuestaMP.get("init_point").asText();

    log.info("✅ MP Response - Preference ID: {}, Init Point: {}", preferenceId, initPoint);

    return ResultadoPago.builder()
            .exitoso(true)
            .transaccionId(preferenceId)        // Esto es el preference_id de MP
            .urlRedireccion(initPoint)          // Esto es el init_point (URL de checkout)
            .requiereRedireccion(true)
            .estadoPago(EstadoPago.PENDIENTE)
            .metodoPago(MetodoPago.MERCADO_PAGO)
            .datosJson(respuestaMP.toString())
            .build();
}

    /**
     * Procesa pagos diferidos (transferencia, efectivo)
     */
    private ResultadoPago procesarPagoDiferido(String metodoPago) {
        log.info("Procesando pago diferido: {}", metodoPago);

        return ResultadoPago.builder()
                .exitoso(true)
                .requiereRedireccion(false)
                .estadoPago(EstadoPago.PENDIENTE)
                .metodoPago(MetodoPago.valueOf(metodoPago))
                .build();
    }
}