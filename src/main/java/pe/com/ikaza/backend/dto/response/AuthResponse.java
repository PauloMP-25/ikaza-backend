package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta del login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    // Token JWT
    private String token;
    private String refreshToken;

    // Información del usuario
    private Integer idUsuario;
    private String email;
    private String username;
    // Rol y permisos
    private String rol;
    private Boolean isAdmin;

    // Información adicional
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;

    // Mensaje de respuesta
    private String mensaje;
    private Boolean success;
}