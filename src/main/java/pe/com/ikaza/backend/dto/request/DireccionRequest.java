package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar una direccion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionRequest {

    @Size(max = 100, message = "El alias no puede exceder 100 caracteres")
    private String alias;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no puede exceder 100 caracteres")
    private String pais;

    @NotBlank(message = "La región es obligatoria")
    @Size(max = 100, message = "La región no puede exceder 100 caracteres")
    private String region;

    @NotBlank(message = "La provincia es obligatoria")
    @Size(max = 100, message = "La provincia no puede exceder 100 caracteres")
    private String provincia;

    @NotBlank(message = "El distrito es obligatorio")
    @Size(max = 100, message = "El distrito no puede exceder 100 caracteres")
    private String distrito;

    @NotBlank(message = "La dirección específica es obligatoria")
    @Size(max = 255, message = "La dirección completa no puede exceder 255 caracteres")
    private String direccion;

    @Size(max = 500, message = "La referencia no puede exceder 500 caracteres")
    private String referencia;

    private Boolean esPrincipal = false;
}