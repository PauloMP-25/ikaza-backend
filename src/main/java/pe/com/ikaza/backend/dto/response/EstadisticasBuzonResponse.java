package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta de las estadtisticas administrativas de los correos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasBuzonResponse {
    private boolean success;
    private Integer totalMensajes;
    private Integer totalReclamos;
    private Integer totalRecomendaciones;
    private Integer reclamosPendientes;
    private Integer reclamosUrgentes;
    private Integer mensajesNoLeidos;
}