package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Tarjeta;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Integer> {
    
    /**
     * Busca todos los métodos de pago activos de un id_usuario.
     */
    List<Tarjeta> findByIdUsuarioAndActivoTrue(Integer idUsuario);
    
    /**
     * Busca un método de pago por su ID y verifica que pertenezca al usuario.
     */
    Optional<Tarjeta> findByIdMetodoAndIdUsuario(Integer idMetodo, Integer idUsuario);
    
    /**
    * DESMARCAR TODAS LAS TARJETAS PRINCIPALES
    */
    @Modifying
    @Query("UPDATE Tarjeta m SET m.esPrincipal = false WHERE m.idUsuario = :idUsuario")
    void desmarcarTodasPrincipales(@Param("idUsuario") Integer idUsuario);
    
    /**
    * CONTAR TARJETAS PRINCIPALES
    */
    @Query("SELECT COUNT(d) FROM Tarjeta d WHERE d.idUsuario = :idUsuario AND d.esPrincipal = true")
    int countTarjetasPrincipales(@Param("idUsuario") Integer idUsuario);
}