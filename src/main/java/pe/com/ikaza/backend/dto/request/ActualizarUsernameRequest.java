package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar username
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarUsernameRequest {

    @NotBlank(message = "El username es requerido")
    @Size(min = 2, max = 100, message = "El username debe tener entre 2 y 100 caracteres")
    private String username;
}