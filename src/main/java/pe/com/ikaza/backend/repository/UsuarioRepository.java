package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Usuario;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Buscar usuario por email
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
    * Buscar usuario por username
    */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verificar si existe usuario por email
     */
    boolean existsByEmail(String email);

    /**
     * Verificar si existe usuario por email
     */
    boolean existsByUsername(String username);

    /**
     * Buscar usuarios activos
     */
    List<Usuario> findByActivoTrue();
}