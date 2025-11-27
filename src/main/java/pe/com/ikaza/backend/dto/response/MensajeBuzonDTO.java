package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta de la creacion de un mensaje al correo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeBuzonDTO {
    private Integer idMensaje;
    private String tipoMensaje;
    private String asunto;
    private String descripcion;
    private String categoriaReclamo;
    private String reclamoOtro;
    private String urgenciaReclamo;
    private String estado;
    private Boolean leido;
    private String respuesta;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;
    private String archivoAdjunto;
    private String archivoEvidencia;
}