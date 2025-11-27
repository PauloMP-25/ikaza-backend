package pe.com.ikaza.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO para crear un nuevo producto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @NotNull(message = "La categoría es obligatoria")
    private Long idCategoria;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombreProducto;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcionProducto;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @Min(value = 1, message = "El stock mínimo debe ser al menos 1")
    private Integer stockMinimo = 5;

    // Detalles extendidos (opcionales, se guardan en MongoDB)
    private String codigo;
    private String marca;
    private String modelo;
    private String garantia;
    private List<String> imagenesUrls;
    private Map<String, String> atributos;
}