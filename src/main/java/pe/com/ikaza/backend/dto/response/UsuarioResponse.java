package pe.com.ikaza.backend.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado para el perfil de un usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    // Datos de Usuario
    private Integer idUsuario;
    private String email;
    
    //Datos
    private String username;
    private String fotoPerfil;
    private String rol;
    
    // Metadatos
    private Boolean activo;
    private Boolean emailVerificado;
    private String proveedorAuth;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
}
