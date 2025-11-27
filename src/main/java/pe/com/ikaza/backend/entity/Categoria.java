package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @autor Paulo
 * Entidad que representa las categorías de productos
 * Mapea a la tabla "categorias" de PostgreSQL
 */
@Entity
@Table(name = "categorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(name = "nombre_categoria", nullable = false, unique = true, length = 100)
    private String nombreCategoria;

    @Column(name = "descripcion_categoria", columnDefinition = "TEXT")
    private String descripcionCategoria;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Relación One-to-Many con Producto
     * Una categoría puede tener múltiples productos
     */
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Producto> productos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (activo == null) {
            activo = true;
        }
    }

    /**
     * Constructor para crear una categoría básica
     */
    public Categoria(String nombreCategoria, String descripcionCategoria) {
        this.nombreCategoria = nombreCategoria;
        this.descripcionCategoria = descripcionCategoria;
        this.activo = true;
    }

    /**
     * Método de utilidad para agregar un producto a la categoría
     */
    public void agregarProducto(Producto producto) {
        productos.add(producto);
        producto.setCategoria(this);
    }

    /**
     * Método de utilidad para remover un producto de la categoría
     */
    public void removerProducto(Producto producto) {
        productos.remove(producto);
        producto.setCategoria(null);
    }
}
