package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta enviado al frontend después del checkout.
 * Incluye información del pedido creado y URLs de redirección si aplica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponse {
    
    // Estado de la operación
    private boolean success;
    private String mensaje;
    
    // Datos del pedido
    private Long pedidoId;
    private String numeroPedido;
    private BigDecimal total;
    private BigDecimal subtotal;
    private LocalDateTime fechaPedido;
    
    // Estado
    private String estado;
    private String estadoPago;
    private String metodoPago;
    
    // Datos de pago (Mercado Pago, Culqi, etc.)
    private String transaccionId;        // preference_id o payment_id
    private String redirectionUrl;       // URL de checkout (init_point)
    private boolean requiresRedirection;
    private int cantidadProductos;
    // Datos adicionales
    private String datosJson;
    
    // Métodos de conveniencia
    public boolean isSuccess() {
        return success;
    }
    
    public boolean getRequiresRedirection() {
        return requiresRedirection;
    }
    
    // Constructor para respuesta exitosa con redirección (Mercado Pago)
    public static PedidoResponse exitoConRedireccion(
            Long pedidoId, 
            String numeroPedido, 
            String redirectionUrl,
            String mensaje) {
        return PedidoResponse.builder()
                .success(true)
                .pedidoId(pedidoId)
                .numeroPedido(numeroPedido)
                .redirectionUrl(redirectionUrl)
                .estado("PENDIENTE")
                .estadoPago("PENDIENTE")
                .mensaje(mensaje)
                .build();
    }

    
    // Constructor para respuesta de error
    public static PedidoResponse error(String mensaje) {
        return PedidoResponse.builder()
                .success(false)
                .mensaje(mensaje)
                .build();
    }

    // Constructor que coincide con la llamada 'super' de PedidoDetalleResponse
    public PedidoResponse(
            boolean success, String mensaje, Long pedidoId, String numeroPedido, String transaccionId,
            String redirectionUrl, String estado, String estadoPago, String metodoPago,
            BigDecimal total, BigDecimal subtotal, LocalDateTime fechaPedido, Integer cantidadProductos) {
        this.success = success;
        this.mensaje = mensaje;
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.transaccionId = transaccionId;
        this.redirectionUrl = redirectionUrl;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.metodoPago = metodoPago;
        this.total = total;
        this.subtotal = subtotal;
        this.fechaPedido = fechaPedido;
    }
}
