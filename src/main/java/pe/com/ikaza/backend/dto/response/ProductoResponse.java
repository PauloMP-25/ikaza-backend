package pe.com.ikaza.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para productos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {

    private Long idProducto;
    private String nombreProducto;
    private String descripcionProducto;
    private BigDecimal precio;
    private Integer stock;
    private Integer stockMinimo;
    private BigDecimal calificacionPromedio;
    private String nombreCategoria;
    private Long idCategoria;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Campos de Producto_Detalle (si existen)
    private String imagenPrincipal;
    private String marca;
    private String modelo;
    private Boolean disponible;
}