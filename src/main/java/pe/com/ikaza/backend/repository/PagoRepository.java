package pe.com.ikaza.backend.repository;

import pe.com.ikaza.backend.entity.Pago;
import pe.com.ikaza.backend.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    Optional<Pago> findByPedido_IdPedido(Long idPedido);
    
    Optional<Pago> findByTransaccionExternaId(String transaccionExternaId);
    
    @Query("SELECT p FROM Pago p WHERE p.estado = :estado")
    List<Pago> findByEstado(@Param("estado") EstadoPago estado);
}
