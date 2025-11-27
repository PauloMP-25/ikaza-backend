package pe.com.ikaza.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PreferenciaMercadoPagoRequest {
    private List<ItemPedidoRequest> items;
}
