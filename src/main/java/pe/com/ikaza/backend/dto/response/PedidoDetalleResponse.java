package pe.com.ikaza.backend.dto.response;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * DTO de respuesta de los detalles de un producto y heredada los atributos de un Pedido
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDetalleResponse extends PedidoResponse {

    // Lista de ítems (productos comprados)
    private List<ItemDetalleResponse> detalles;

    // Información de Pago Snapshot
    private String ultimos4DigitosTarjeta;
    private String tipoTarjeta;
    private String bancoEmisor;
    private LocalDateTime fechaPago;

    // Información de envío 
    private String telefonoContacto;

    @Builder(builderMethodName = "detalleBuilder")
    public PedidoDetalleResponse(
            boolean success, String mensaje, Long pedidoId, String numeroPedido, String transaccionId,
            String redirectionUrl, String estadoPedido, String estadoPago, String metodoPago,
            BigDecimal total, BigDecimal subtotal, LocalDateTime fechaPedido, Integer cantidadProductos,
            List<ItemDetalleResponse> detalles, String ultimos4DigitosTarjeta, String tipoTarjeta,
            String bancoEmisor, LocalDateTime fechaPago, String direccionEnvioCompleta, String telefonoContacto) {

        super(success, mensaje, pedidoId, numeroPedido, transaccionId, redirectionUrl,
                estadoPedido, estadoPago, metodoPago, total, subtotal, fechaPedido, cantidadProductos);

        this.detalles = detalles;
        this.ultimos4DigitosTarjeta = ultimos4DigitosTarjeta;
        this.tipoTarjeta = tipoTarjeta;
        this.bancoEmisor = bancoEmisor;
        this.fechaPago = fechaPago;
        this.telefonoContacto = telefonoContacto;
    }
}
