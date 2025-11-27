package pe.com.ikaza.backend.dto.request;

import lombok.Data;

@Data
public class ConfirmarPagoMercadoPagoRequest {
    private String paymentId;
    private String status;
}
