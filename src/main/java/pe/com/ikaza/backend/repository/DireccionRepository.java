package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Direccion;

import java.util.List;
import java.util.Optional;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Integer> {
    
    /**
     * Busca todas las direcciones asociadas a un id_usuario específico.
     */
    List<Direccion> findByIdUsuario(Integer idUsuario);

    /**
     * Busca una dirección por su ID y verifica que pertenezca al usuario.
     */
    Optional<Direccion> findByIdDireccionAndIdUsuario(Integer idDireccion, Integer idUsuario);
    
    /**
     * DESMARCAR TODAS LAS DIRECCIONES PRINCIPALES
     */
    @Modifying
    @Query("UPDATE Direccion d SET d.esPrincipal = false WHERE d.idUsuario = :idUsuario")
    void desmarcarTodasPrincipales(@Param("idUsuario") Integer idUsuario);
    
    /**
     * CONTAR DIRECCIONES PRINCIPALES
     */
    @Query("SELECT COUNT(d) FROM Direccion d WHERE d.idUsuario = :idUsuario AND d.esPrincipal = true")
    int countDireccionesPrincipales(@Param("idUsuario") Integer idUsuario);
    
    /**
     * Buscar la dirección principal actual
     */
    Optional<Direccion> findByIdUsuarioAndEsPrincipalTrue(Integer idUsuario);
}