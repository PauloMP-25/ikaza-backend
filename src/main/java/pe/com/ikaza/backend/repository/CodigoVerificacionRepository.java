// CodigoVerificacionRepository.java
package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.CodigoVerificacion;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

        
        Optional<CodigoVerificacion> findTopByEmailAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        String email,
                        CodigoVerificacion.TipoVerificacion tipo);

        Optional<CodigoVerificacion> findTopByTelefonoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        String telefono,
                        CodigoVerificacion.TipoVerificacion tipo);

        @Modifying
        @Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha")
        void eliminarCodigosExpirados(LocalDateTime fecha);
}