package pe.com.ikaza.backend.repository;

import pe.com.ikaza.backend.entity.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedido_IdPedidoOrderByFechaCambioDesc(Long idPedido);

    @Query("SELECT h FROM HistorialEstadoPedido h " +
            "WHERE h.pedido.idPedido = :idPedido " +
            "ORDER BY h.fechaCambio DESC")
    List<HistorialEstadoPedido> findHistorialByPedidoId(@Param("idPedido") Long idPedido);
    
    /**
    * Método para eliminar un pedido
    */
    void deleteByPedido_IdPedido(Long idPedido);
}
