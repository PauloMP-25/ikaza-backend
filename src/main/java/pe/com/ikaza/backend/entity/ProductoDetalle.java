package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entidad única para todos los detalles extendidos del producto
 */
@Entity
@Table(name = "producto_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto_detalle")
    private Long idProductoDetalle;

    @OneToOne
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "codigo", length = 100, unique = true)
    private String codigo;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "garantia", length = 200)
    private String garantia;

    @Column(name = "instrucciones_uso", columnDefinition = "TEXT")
    private String instruccionesUso;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "imagenes", columnDefinition = "jsonb")
    private List<ImagenDto> imagenes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos", columnDefinition = "jsonb")
    private Map<String, String> atributos = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "especificaciones", columnDefinition = "jsonb")
    private List<EspecificacionDto> especificaciones = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variantes", columnDefinition = "jsonb")
    private List<VarianteDto> variantes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seo", columnDefinition = "jsonb")
    private SeoDto seo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // ============================================
    // CLASES DTO PARA JSON
    // ============================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagenDto {
        private String url;
        private String alt;
        private Boolean esPrincipal = false;
        private Integer orden = 0;
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
        private Integer stockAdicional = 0;
        private String imagenUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoDto {
        private String slug;
        private String metaTitle;
        private String metaDescription;
        private List<String> keywords = new ArrayList<>();
    }

    // ============================================
    // MÉTODOS DE UTILIDAD
    // ============================================

    public void agregarImagen(String url, String alt, Boolean esPrincipal) {
        if (imagenes == null) {
            imagenes = new ArrayList<>();
        }

        if (Boolean.TRUE.equals(esPrincipal)) {
            imagenes.forEach(img -> img.setEsPrincipal(false));
        }

        ImagenDto imagen = new ImagenDto(url, alt, esPrincipal, imagenes.size());
        imagenes.add(imagen);
    }

    public ImagenDto getImagenPrincipal() {
        if (imagenes == null || imagenes.isEmpty()) {
            return null;
        }
        return imagenes.stream()
                .filter(img -> Boolean.TRUE.equals(img.getEsPrincipal()))
                .findFirst()
                .orElse(imagenes.get(0));
    }

    public void agregarAtributo(String clave, String valor) {
        if (atributos == null) {
            atributos = new HashMap<>();
        }
        atributos.put(clave, valor);
    }

    public void agregarVariante(VarianteDto variante) {
        if (variantes == null) {
            variantes = new ArrayList<>();
        }
        variantes.add(variante);
    }

    public VarianteDto buscarVariantePorSku(String sku) {
        if (variantes == null) {
            return null;
        }
        return variantes.stream()
                .filter(v -> sku.equals(v.getSku()))
                .findFirst()
                .orElse(null);
    }

    public boolean tieneVariantes() {
        return variantes != null && !variantes.isEmpty();
    }

    public List<String> getUrlsImagenes() {
        if (imagenes == null) {
            return new ArrayList<>();
        }
        return imagenes.stream()
                .map(ImagenDto::getUrl)
                .toList();
    }
}