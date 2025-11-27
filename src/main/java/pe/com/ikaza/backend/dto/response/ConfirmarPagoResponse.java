// dto/response/ConfirmarPagoResponse.java
package pe.com.ikaza.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmarPagoResponse {
    private boolean success;
    private String numeroPedido;
    private String mensaje;
}
