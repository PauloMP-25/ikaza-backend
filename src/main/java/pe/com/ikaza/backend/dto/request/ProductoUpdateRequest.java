package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para actualizar un producto existente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUpdateRequest {

    private Long idCategoria;

    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombreProducto;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcionProducto;

    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    private Integer stockMinimo;
}
