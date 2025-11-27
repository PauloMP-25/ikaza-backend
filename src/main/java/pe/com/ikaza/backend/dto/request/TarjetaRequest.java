package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una tarjeta de un usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaRequest {

    @NotBlank(message = "El tipo de método es obligatorio")
    private String tipo;

    @NotBlank(message = "El token de pago es obligatorio")
    private String tokenPago; // TOKEN ENCRIPTADO

    @Size(min = 4, max = 4, message = "Los últimos 4 dígitos deben ser 4 caracteres")
    private String ultimos4Digitos;

    private String alias;
    private String nombreTitular;
    private String bancoEmisor;
    private String tipoTarjeta;
    private Boolean esPrincipal = false;

    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{2}$", message = "Formato de expiración inválido (MM/YY)")
    private String fechaExpiracion;
}