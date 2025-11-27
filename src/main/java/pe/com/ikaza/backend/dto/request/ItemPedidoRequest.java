package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO que representa un item en el carrito de compras.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoRequest{

    @NotNull(message = "El ID del producto es obligatorio")
    private Long idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @Min(value = 0, message = "El precio debe ser mayor a 0")
    private BigDecimal precioUnitario;

    // Variantes opcionales
    private String color;
    private String talla;
    private String sku;

    // Información adicional
    private String nombreProducto;
    private String imagenUrl;
    private Long idVariante;
    
    // Métodos auxiliares para Mercado Pago
    public String getTitle() {
        return nombreProducto;
    }
    
    public Integer getQuantity() {
        return cantidad;
    }
    
    public BigDecimal getUnit_price() {
        return precioUnitario;
    }
}
