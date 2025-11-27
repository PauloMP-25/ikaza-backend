package pe.com.ikaza.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO de respuesta de un item de carrito
 */
@Data
@Builder
public class ItemDetalleResponse {
    private Long idProducto;
    private String nombreProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String colorSeleccionado;
    private String tallaSeleccionada;
}