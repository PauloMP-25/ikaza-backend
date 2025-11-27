package pe.com.ikaza.backend.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta de una direccion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionResponse {

    private Integer idDireccion;
    private String alias;

    private String pais;
    private String region;
    private String provincia;
    private String distrito;

    private String direccion;
    private String referencia;


    // Control
    private Boolean esPrincipal;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}