package pe.com.ikaza.backend.repository;

import pe.com.ikaza.backend.entity.Pedido;
import pe.com.ikaza.backend.enums.EstadoPedido;
import pe.com.ikaza.backend.enums.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

        List<Pedido> findByIdUsuario(Integer idUsuario);

        Optional<Pedido> findByNumeroPedido(String numeroPedido);

        List<Pedido> findByIdUsuarioOrderByFechaPedidoDesc(Integer idUsuario);

        @Query("SELECT p FROM Pedido p WHERE p.idUsuario = :idUsuario " +
                        "AND p.estado = :estado ORDER BY p.fechaPedido DESC")
        List<Pedido> findByIdUsuarioAndEstado(
                        @Param("idUsuario") Long idUsuario,
                        @Param("estado") EstadoPedido estado);

        boolean existsByNumeroPedido(String numeroPedido);

        /*
         * Encuentra pedidos preliminares antiguos para limpieza
         * Busca pedidos PENDIENTES de MercadoPago con más de X tiempo
         */
        List<Pedido> findByEstadoAndMetodoPagoAndFechaPedidoBefore(
                        EstadoPedido estado,
                        MetodoPago metodoPago,
                        LocalDateTime fecha);

        /**
         * Cuenta pedidos pendientes por usuario
         */
        @Query("SELECT COUNT(p) FROM Pedido p WHERE p.idUsuario = :idUsuario " +
                        "AND p.estado = :estado")
        long contarPedidosPendientesPorUsuario(
                        @Param("idUsuario") Integer idUsuario,
                        @Param("estado") EstadoPedido estado);
}