package pe.com.ikaza.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un recurso (entidad) no es encontrado.
 * Mapea automáticamente a un código de estado HTTP 404 (Not Found).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor que acepta un mensaje de detalle.
     * @param message El mensaje a mostrar (ej: "Dirección no encontrada").
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor que acepta un mensaje y la causa original (Throwable).
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}