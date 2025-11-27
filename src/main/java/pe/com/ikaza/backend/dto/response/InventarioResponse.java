package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioResponse {

    private Long idInventario;
    private Long idProducto;
    private String nombreProducto;
    private Integer stockActual;
    private Integer stockReservado;
    private Integer stockDisponible;
    private Boolean necesitaReposicion;
}
