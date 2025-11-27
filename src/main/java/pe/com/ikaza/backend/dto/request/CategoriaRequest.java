package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar una categoría
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombreCategoria;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcionCategoria;

    private Boolean activo = true;
}