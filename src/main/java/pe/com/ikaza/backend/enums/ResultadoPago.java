package pe.com.ikaza.backend.enums;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResultadoPago {
    private boolean exitoso;
    private String transaccionId;
    private String urlRedireccion;
    private boolean requiereRedireccion;
    private EstadoPago estadoPago;
    private MetodoPago metodoPago;
    private String datosJson;
    private Long pedidoIdPreliminar;
}