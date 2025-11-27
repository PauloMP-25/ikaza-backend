package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar contraseña
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String newPassword;
}
