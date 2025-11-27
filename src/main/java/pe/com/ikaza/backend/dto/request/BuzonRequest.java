package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para la solicitud de envío de mensaje al Buzón (reclamo o recomendación).
 */
@Data
public class BuzonRequest {

    @NotNull(message = "El tipo de mensaje es obligatorio")
    private TipoMensaje tipo;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
    private String asunto;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
    private String descripcion;

    // Campos específicos para RECLAMO
    private CategoriaReclamo categoriaReclamo;

    @Size(max = 200, message = "La especificación no puede exceder 200 caracteres")
    private String reclamoOtro;

    private UrgenciaReclamo urgenciaReclamo;

    // Archivos
    private MultipartFile archivoAdjunto;
    private MultipartFile archivoEvidencia;

    public enum TipoMensaje {
        RECOMENDACION, RECLAMO
    }

    public enum CategoriaReclamo {
        PRODUCTO, ENTREGA, ATENCION, FACTURACION, OTRO
    }

    public enum UrgenciaReclamo {
        NORMAL, ALTA
    }
}