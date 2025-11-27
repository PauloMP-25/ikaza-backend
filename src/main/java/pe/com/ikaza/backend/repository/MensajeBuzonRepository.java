// MensajeBuzonRepository.java
package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.MensajeBuzon;
import pe.com.ikaza.backend.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeBuzonRepository extends JpaRepository<MensajeBuzon, Integer> {

        /**
         * Encuentra todos los mensajes de un usuario específico
         */
        List<MensajeBuzon> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

        /**
         * Encuentra mensajes por usuario y tipo
         */
        List<MensajeBuzon> findByUsuarioAndTipoMensajeOrderByFechaCreacionDesc(
                        Usuario usuario,
                        MensajeBuzon.TipoMensaje tipoMensaje);

        /**
         * Encuentra mensajes por estado
         */
        List<MensajeBuzon> findByEstadoOrderByFechaCreacionDesc(MensajeBuzon.EstadoMensaje estado);

        /**
         * Encuentra mensajes no leídos de un usuario
         */
        List<MensajeBuzon> findByUsuarioAndLeidoFalseOrderByFechaCreacionDesc(Usuario usuario);

        /**
         * Cuenta mensajes no leídos de un usuario
         */
        Long countByUsuarioAndLeidoFalse(Usuario usuario);

        /**
         * Encuentra reclamos con urgencia alta pendientes
         */
        @Query("SELECT m FROM MensajeBuzon m WHERE m.tipoMensaje = 'RECLAMO' " +
                        "AND m.urgenciaReclamo = 'ALTA' " +
                        "AND m.estado IN ('PENDIENTE', 'EN_REVISION') " +
                        "ORDER BY m.fechaCreacion ASC")
        List<MensajeBuzon> findReclamosUrgentes();

        /**
         * Encuentra mensajes por rango de fechas
         */
        @Query("SELECT m FROM MensajeBuzon m WHERE m.usuario = :usuario " +
                        "AND m.fechaCreacion BETWEEN :fechaInicio AND :fechaFin " +
                        "ORDER BY m.fechaCreacion DESC")
        List<MensajeBuzon> findByUsuarioAndFechaCreacionBetween(
                        @Param("usuario") Usuario usuario,
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin);

        /**
         * Cuenta mensajes por tipo y estado
         */
        @Query("SELECT COUNT(m) FROM MensajeBuzon m WHERE m.tipoMensaje = :tipo " +
                        "AND m.estado = :estado")
        Long countByTipoAndEstado(
                        @Param("tipo") MensajeBuzon.TipoMensaje tipo,
                        @Param("estado") MensajeBuzon.EstadoMensaje estado);

        /**
         * Encuentra mensajes por tipo
         */
        List<MensajeBuzon> findByTipoMensajeOrderByFechaCreacionDesc(MensajeBuzon.TipoMensaje tipoMensaje);

        /**
         * Cuenta mensajes no leídos
         */
        Long countByLeidoFalse();

        /**
         * Cuenta mensajes por tipo (sin filtro de estado)
         */
        @Query("SELECT COUNT(m) FROM MensajeBuzon m WHERE m.tipoMensaje = :tipo")
        Long countByTipo(@Param("tipo") MensajeBuzon.TipoMensaje tipo);
}