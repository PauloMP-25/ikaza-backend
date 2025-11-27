package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de movimiento de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponse {

    private Long idMovimiento;
    private Long idProducto;
    private String nombreProducto;
    private String tipoMovimiento;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private String motivo;
    private LocalDateTime fechaMovimiento;
    private String nombreUsuario;
}