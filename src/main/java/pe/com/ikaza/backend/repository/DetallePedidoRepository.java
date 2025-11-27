package pe.com.ikaza.backend.repository;

import pe.com.ikaza.backend.entity.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedido_IdPedido(Long idPedido);

    @Query("SELECT d FROM DetallePedido d " +
            "JOIN FETCH d.producto " +
            "WHERE d.pedido.idPedido = :idPedido")
    List<DetallePedido> findByPedidoIdWithProducto(@Param("idPedido") Long idPedido);
    
    /**
     * Método para obtener solo el conteo de detalles, sin cargar las entidades.
     */
    @Query("SELECT COUNT(d) FROM DetallePedido d WHERE d.pedido.idPedido = :idPedido")
    Integer countByPedidoId(@Param("idPedido") Integer idPedido);
    
    /**
     * Método para eliminar un detallePedido por pedidoID.
     */
    void deleteByPedido_IdPedido(Long idPedido);
}
