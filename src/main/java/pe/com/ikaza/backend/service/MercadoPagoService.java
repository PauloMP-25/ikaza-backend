package pe.com.ikaza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.com.ikaza.backend.dto.request.ItemPedidoRequest;
import pe.com.ikaza.backend.entity.Producto;
import pe.com.ikaza.backend.repository.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@Slf4j
public class MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String mercadoPagoAccessToken;

    @Value("${mercadopago.success.url}")
    private String successUrl;

    @Value("${mercadopago.failure.url}")
    private String failureUrl;

    @Value("${mercadopago.pending.url}")
    private String pendingUrl;

    private static final String MP_API_URL = "https://api.mercadopago.com/checkout/preferences";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProductoRepository productoRepository;

    public MercadoPagoService(ProductoRepository productoRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.productoRepository = productoRepository;
    }

        /**
         * Crea una preferencia de pago en Mercado Pago.
         * 
         * @param items    Lista de items del pedido
         * @param pedidoId ID del pedido preliminar (para los back_urls)
         * @return JsonNode con la respuesta de Mercado Pago
         */
        // En MercadoPagoService.java - Reemplaza el método crearPreferencia

public JsonNode crearPreferencia(List<ItemPedidoRequest> items, Long pedidoId) {
    log.debug("MP Access Token: {}", mercadoPagoAccessToken.substring(0, 5) + "...");
    log.debug("MP Success URL: {}", successUrl);
    log.debug("MP Failure URL: {}", failureUrl);
    
    try {
        log.info("Creando preferencia de Mercado Pago para pedido: {}", pedidoId);
        
        // Construir items de la preferencia
        List<Map<String, Object>> mpItems = new ArrayList<>();
        for (ItemPedidoRequest item : items) {
            String itemTitle = item.getNombreProducto();
            
            // Si no viene el nombre, intentar recuperar de BD
            if (itemTitle == null || itemTitle.trim().isEmpty()) {
                log.warn("Nombre del producto vacío para ID: {}. Recuperando de DB.", 
                        item.getIdProducto());
                Producto producto = productoRepository.findByIdProducto(item.getIdProducto());
                itemTitle = producto != null ? producto.getNombreProducto()
                        : "Producto ID " + item.getIdProducto();
            }
            
            // Agregar variantes al título si existen
            StringBuilder fullTitle = new StringBuilder(itemTitle);
            if (item.getColor() != null && !item.getColor().isEmpty()) {
                fullTitle.append(" - Color: ").append(item.getColor());
            }
            if (item.getTalla() != null && !item.getTalla().isEmpty()) {
                fullTitle.append(", Talla: ").append(item.getTalla());
            }
            
            Map<String, Object> mpItem = new HashMap<>();
            mpItem.put("title", fullTitle.toString());
            mpItem.put("quantity", item.getCantidad());
            mpItem.put("unit_price", item.getPrecioUnitario().doubleValue());
            mpItem.put("currency_id", "PEN");
            
            // Agregar descripción opcional
            if (item.getSku() != null) {
                mpItem.put("description", "SKU: " + item.getSku());
            }
            
            // Agregar imagen si existe
            if (item.getImagenUrl() != null && !item.getImagenUrl().isEmpty()) {
                mpItem.put("picture_url", item.getImagenUrl());
            }
            
            mpItems.add(mpItem);
            
            log.info("Item agregado: {} - Cant: {} - Precio: {}", 
                    fullTitle.toString(), item.getCantidad(), item.getPrecioUnitario());
        }

Map<String, String> backUrls = new HashMap<>();
backUrls.put("success", successUrl + "?method=mercadopago&pedidoId=" + pedidoId);
backUrls.put("failure", failureUrl + "?method=mercadopago&pedidoId=" + pedidoId);
backUrls.put("pending", pendingUrl + "?method=mercadopago&pedidoId=" + pedidoId);

log.info("📍 Back URLs configuradas:");
log.info("   Success: {}", backUrls.get("success"));
log.info("   Failure: {}", backUrls.get("failure"));
log.info("   Pending: {}", backUrls.get("pending"));

        log.debug("Back URLs configuradas - Success: {}, Failure: {}, Pending: {}", 
                successUrl, failureUrl, pendingUrl);

        // Construir payload completo
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", mpItems);
        payload.put("back_urls", backUrls);
        payload.put("external_reference", pedidoId.toString());
        
        // IMPORTANTE: Solo agregar auto_return si las URLs están correctamente configuradas
        // Para desarrollo, es mejor omitirlo y que el usuario haga clic en "Volver al sitio"
        // payload.put("auto_return", "approved");  // Comentado para desarrollo

        // Metadata adicional
        Map<String, String> metadata = new HashMap<>();
        metadata.put("pedido_id", pedidoId.toString());
        payload.put("metadata", metadata);

        // Log del payload final
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            log.debug("Payload FINAL enviado a MP: {}", payloadJson);
        } catch (Exception ignored) {
            log.warn("No se pudo serializar payload para log");
        }
        
        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mercadoPagoAccessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                MP_API_URL,
                request,
                String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String preferenceId = responseJson.get("id").asText();
            String initPoint = responseJson.get("init_point").asText();

            log.info("✅ Preferencia creada exitosamente. ID: {}, URL: {}",
                    preferenceId, initPoint);

            return responseJson;
        } else {
            log.error("❌ Error en respuesta de Mercado Pago: {}", responseJson);
            throw new RuntimeException("Error al crear preferencia de Mercado Pago");
        }

    } catch (Exception e) {
        log.error("❌ Error al crear preferencia en Mercado Pago", e);
        throw new RuntimeException("Error al procesar el pago: " + e.getMessage());
    }
}

    /**
     * Consulta el estado de un pago en Mercado Pago.
     */
    public JsonNode consultarPago(String paymentId) {
        try {
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(mercadoPagoAccessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class);

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            log.error("Error al consultar pago en Mercado Pago", e);
            throw new RuntimeException("Error al consultar estado del pago");
        }
    }
}