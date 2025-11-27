package pe.com.ikaza.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

       /*BUSCAR UN PRODUCTO POR SU ID */
       Producto findByIdProducto(Long idProducto);

       /**
        * Busca productos por categoría
        */
       List<Producto> findByCategoriaIdCategoria(Long idCategoria);

       /**
        * Busca productos por categoría con paginación
        * Pageable permite dividir los resultados en páginas
        */
       Page<Producto> findByCategoriaIdCategoria(Long idCategoria, Pageable pageable);

       /**
        * Busca productos por nombre (búsqueda parcial, sin importar mayúsculas)
        */
       List<Producto> findByNombreProductoContainingIgnoreCase(String nombre);

       /**
        * Busca productos con paginación (para el catálogo)
        */
       Page<Producto> findAll(Pageable pageable);

       /**
        * Busca productos con stock disponible
        */
       List<Producto> findByStockGreaterThan(Integer stock);

       @Query("SELECT p FROM Producto p WHERE p.idProducto IN :ids")
       List<Producto> findAllByIdIn(@Param("ids") List<Long> ids);

       // Método para verificar y actualizar stock
       @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad " +
                     "WHERE p.idProducto = :idProducto AND p.stock >= :cantidad")
       int actualizarStock(
                     @Param("idProducto") Long idProducto,
                     @Param("cantidad") Integer cantidad);

       /**
        * Busca productos que necesitan reposición
        */
       @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo")
       List<Producto> findProductosConBajoStock();

       /**
        * Busca productos por rango de precio
        */
       List<Producto> findByPrecioBetween(BigDecimal precioMin, BigDecimal precioMax);

       /**
        * Busca productos por categoría y rango de precio con paginación
        */
       Page<Producto> findByCategoriaIdCategoriaAndPrecioBetween(
                     Long idCategoria,
                     BigDecimal precioMin,
                     BigDecimal precioMax,
                     Pageable pageable);

       /**
        * Búsqueda avanzada: nombre o descripción contiene el texto
        */
       @Query("SELECT p FROM Producto p WHERE " +
                     "LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
                     "LOWER(p.descripcionProducto) LIKE LOWER(CONCAT('%', :texto, '%'))")
       List<Producto> buscarPorTexto(@Param("texto") String texto);

       /**
        * Búsqueda con paginación
        */
       @Query("SELECT p FROM Producto p WHERE " +
                     "LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
                     "LOWER(p.descripcionProducto) LIKE LOWER(CONCAT('%', :texto, '%'))")
       Page<Producto> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

       /**
        * Obtiene el producto más vendido (por calificación)
        */
       @Query("SELECT p FROM Producto p WHERE p.stock > 0 ORDER BY p.calificacionPromedio DESC, p.fechaCreacion DESC LIMIT 1")
       Optional<Producto> findProductoMasVendido();

       /**
        * Obtiene los N productos más baratos con stock disponible
        */
       @Query("SELECT p FROM Producto p WHERE p.stock > 0 ORDER BY p.precio ASC")
       Page<Producto> findProductosMasBaratos(Pageable pageable);

       /**
        * Obtiene los N productos más recientes
        */
       @Query("SELECT p FROM Producto p ORDER BY p.fechaCreacion DESC")
       Page<Producto> findProductosMasRecientes(Pageable pageable);

       /**
        * Obtiene productos con stock entre 5 y 10 (por agotarse)
        */
       @Query("SELECT p FROM Producto p WHERE p.stock BETWEEN 5 AND 10 ORDER BY p.stock ASC")
       Page<Producto> findProductosPorAgotarse(Pageable pageable);

       /**
        * Verifica si existe un producto con ese nombre en una categoría
        */
       boolean existsByNombreProductoAndCategoriaIdCategoria(
                     String nombreProducto,
                     Long idCategoria);

       /**
        * Cuenta productos por categoría
        */
       @Query("SELECT COUNT(p) FROM Producto p WHERE p.categoria.idCategoria = :idCategoria")
       long contarProductosPorCategoria(@Param("idCategoria") Long idCategoria);
}