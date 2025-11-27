package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con datos completos del usuario
 * Combina datos de Usuario (Auth) + Cliente (Perfil)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    // Datos de Usuario (Auth Core)
    private Integer idUsuario;

    // Datos de Cliente (Perfil)
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    
    // Datos extendidos
    private String tipoDocumento;
    private String numeroDocumento;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String prefijoTelefono;
    private String telefono;
    private Boolean telefonoVerificado;
    private String genero;
    private LocalDateTime fechaActualizacion;
    
    // Flag para indicar si tiene datos completos
    private Boolean datosCompletos;
}