package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Inventario;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    /**
     * Busca el inventario de un producto específico
     */
    Optional<Inventario> findByProductoIdProducto(Long idProducto);

    /**
     * Busca inventarios con stock bajo (stockDisponible <= 5)
     */
    @Query("SELECT i FROM Inventario i WHERE i.stockDisponible <= 5")
    List<Inventario> findInventariosConStockBajo();

    /**
     * Busca inventarios sin stock disponible
     */
    @Query("SELECT i FROM Inventario i WHERE i.stockDisponible = 0")
    List<Inventario> findInventariosSinStock();
}