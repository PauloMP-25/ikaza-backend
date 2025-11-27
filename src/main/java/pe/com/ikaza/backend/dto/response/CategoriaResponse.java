package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para categorías
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {

    private Long idCategoria;
    private String nombreCategoria;
    private String descripcionCategoria;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private Integer cantidadProductos;
}
