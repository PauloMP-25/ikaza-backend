package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para actualizar los datos personales de un cliente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarClienteRequest {

    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    private String nombres;

    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;

    private String tipoDocumento; // DNI, CE, RUC, PASAPORTE

    @Pattern(regexp = "^[0-9]{8,20}$", message = "Número de documento inválido")
    private String numeroDocumento;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^\\+\\d{1,3}$", message = "Prefijo telefónico inválido")
    private String prefijoTelefono;

    @Pattern(regexp = "^[0-9]{9}$", message = "Teléfono debe tener 9 dígitos")
    private String telefono;

    private Boolean telefonoVerificado;

    @Pattern(regexp = "^(HOMBRE|MUJER|OTRO)$", message = "Género inválido. Debe ser HOMBRE, MUJER u OTRO.")
    private String genero;
}