package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta detallada de un producto
 * Incluye información extendida de producto_detalle
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDetalleResponse {

    // Información de PostgreSQL (tabla productos)
    private Long idProducto;
    private String nombreProducto;
    private String descripcionProducto;
    private BigDecimal precio;
    private Integer stock;
    private Integer stockDisponible;
    private Integer stockReservado;
    private BigDecimal calificacionPromedio;
    private String nombreCategoria;
    private Long idCategoria;

    // Información extendida (tabla producto_detalle)
    private String codigo;
    private String marca;
    private String modelo;
    private String garantia;
    private List<ImagenDto> imagenes;
    private Map<String, String> atributos;
    private List<EspecificacionDto> especificaciones;
    private List<VarianteDto> variantes;
    private SeoDto seo;

    // DTOs internos
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagenDto {
        private String url;
        private String alt;
        private Boolean esPrincipal;
        private Integer orden;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EspecificacionDto {
        private String nombre;
        private String valor;
        private String unidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteDto {
        private String sku;
        private String color;
        private String talla;
        private String material;
        private Integer stockAdicional;
        private String imagenUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoDto {
        private String slug;
        private String metaTitle;
        private String metaDescription;
        private List<String> keywords;
    }
}