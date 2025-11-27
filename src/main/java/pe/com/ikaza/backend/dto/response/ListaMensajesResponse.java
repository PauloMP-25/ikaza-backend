package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta de correos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaMensajesResponse {
    private boolean success;
    private List<MensajeBuzonDTO> mensajes;
    private Integer total;
}