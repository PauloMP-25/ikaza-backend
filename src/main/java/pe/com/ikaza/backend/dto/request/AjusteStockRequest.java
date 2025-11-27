package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.ikaza.backend.entity.MovimientoInventario;

/**
 * DTO para solicitud de ajuste de stock (admin)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjusteStockRequest {

    @NotNull(message = "El tipo de movimiento es requerido")
    private MovimientoInventario.TipoMovimiento tipo;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotBlank(message = "El motivo es requerido")
    private String motivo;
}
