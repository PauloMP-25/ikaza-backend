package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.ProductoDetalle;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoDetalleRepository extends JpaRepository<ProductoDetalle, Long> {

    Optional<ProductoDetalle> findByProductoIdProducto(Long idProducto);

    Optional<ProductoDetalle> findByCodigo(String codigo);

    List<ProductoDetalle> findByMarca(String marca);

    boolean existsByProductoIdProducto(Long idProducto);

    void deleteByProductoIdProducto(Long idProducto);
}