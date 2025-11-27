package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de respuesta de una tarjeta del cliente
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaResponse {

    private Integer idMetodo;
    private String tipo;
    private String alias;
    private String ultimos4Digitos;
    private String nombreTitular;
    private String bancoEmisor;
    private String tipoTarjeta;
    private String fechaExpiracion;
    private Boolean esPrincipal;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}