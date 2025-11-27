package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

        /**
         * Busca movimientos de un producto específico
         */
        List<MovimientoInventario> findByProductoIdProductoOrderByFechaMovimientoDesc(Long idProducto);

        /**
         * Busca movimientos por tipo
         */
        List<MovimientoInventario> findByTipoMovimientoOrderByFechaMovimientoDesc(
                        MovimientoInventario.TipoMovimiento tipo);

        /**
         * Busca movimientos realizados por un usuario
         */
        List<MovimientoInventario> findByUsuarioIdUsuarioOrderByFechaMovimientoDesc(Long idUsuario);

        /**
         * Busca movimientos en un rango de fechas
         */
        @Query("SELECT m FROM MovimientoInventario m WHERE " +
                        "m.fechaMovimiento BETWEEN :fechaInicio AND :fechaFin " +
                        "ORDER BY m.fechaMovimiento DESC")
        List<MovimientoInventario> findMovimientosPorRangoFecha(
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin);

        /**
         * Últimos movimientos del sistema
         */
        List<MovimientoInventario> findTop50ByOrderByFechaMovimientoDesc();
}