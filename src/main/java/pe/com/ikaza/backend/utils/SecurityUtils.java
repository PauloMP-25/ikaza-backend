package pe.com.ikaza.backend.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Clase de utilidad para interactuar con el contexto de seguridad.
 * Permite obtener información del usuario que ha sido autenticado mediante JWT.
 */
@Component
public class SecurityUtils {

    /**
     * Obtiene el nombre de usuario (email) del usuario actualmente autenticado.
     * Esto se utiliza para buscar el ID real del usuario en la base de datos.
     * @return El nombre de usuario (email) o null si no hay autenticación.
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                // En algunos casos, el principal podría ser el string del username
                return (String) principal;
            }
        }
        return null;
    }
}