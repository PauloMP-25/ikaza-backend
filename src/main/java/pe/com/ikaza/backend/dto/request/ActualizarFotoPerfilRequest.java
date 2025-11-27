package pe.com.ikaza.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar foto de perfil o icono
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarFotoPerfilRequest {

    private String photoURL; // URL de imagen o clase de icono Bootstrap
    private String customIcon;
}
