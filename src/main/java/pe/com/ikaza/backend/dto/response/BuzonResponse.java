package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta del envio de un correo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuzonResponse {
    private boolean success;
    private String mensaje;
    private Integer idMensaje;
}