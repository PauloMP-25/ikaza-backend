package pe.com.ikaza.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.DireccionRequest;
import pe.com.ikaza.backend.dto.response.DireccionResponse;
import pe.com.ikaza.backend.service.DireccionService;
import pe.com.ikaza.backend.service.UsuarioService;
import pe.com.ikaza.backend.utils.SecurityUtils;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para la gestión de las direcciones del Cliente
 */
@RestController
@RequestMapping("/api/usuarios/direcciones")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SecurityUtils securityUtils;

    // ========== METODO AUXILIAR ==========

    /**
     * Obtiene el ID del usuario autenticado a partir del token JWT.
     */
    private Integer getCurrentUserId() {
        String email = securityUtils.getCurrentUserEmail();

        if (email == null) {
            throw new RuntimeException("Usuario no autenticado o token no contiene email.");
        }
        return usuarioService.obtenerPorEmail(email).getIdUsuario();
    }

    /**
     * GET /api/usuarios/direcciones
     * Carga todas las direcciones del usuario autenticado.
     */
    @GetMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DireccionResponse>> cargarDirecciones() {
        Integer idUsuario = getCurrentUserId();
        List<DireccionResponse> direcciones = direccionService.cargarDirecciones(idUsuario);
        return ResponseEntity.ok(direcciones);
    }

    /**
     * POST /api/usuarios/direcciones
     * Guarda una nueva dirección.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> guardarDireccion(@Valid @RequestBody DireccionRequest request) {
        try {
            Integer idUsuario = getCurrentUserId();
            DireccionResponse nuevaDireccion = direccionService.guardarDireccion(idUsuario, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaDireccion);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * PUT /api/usuarios/direcciones/{idDireccion}
     * Actualiza una dirección existente.
     */
    @PutMapping("/{idDireccion}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<DireccionResponse> actualizarDireccion(@PathVariable Integer idDireccion,
            @Valid @RequestBody DireccionRequest request) {
        Integer idUsuario = getCurrentUserId();
        DireccionResponse actualizada = direccionService.actualizarDireccion(idDireccion, idUsuario, request);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * PUT /api/usuarios/direcciones/{idDireccion}/principal
     * Actualiza una dirección existente marcándola como principal
     */
    @PutMapping("/{idDireccion}/principal")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<DireccionResponse> actualizarDireccionPrincipal(@PathVariable Integer idDireccion,
            @Valid @RequestBody DireccionRequest request) {
        Integer idUsuario = getCurrentUserId();
        DireccionResponse actualizada = direccionService.actualizarDireccionPrincipal(idDireccion, idUsuario, request);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * DELETE /api/usuarios/direcciones/{idDireccion}
     * Elimina una dirección.
     */
    @DeleteMapping("/{idDireccion}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> eliminarDireccion(@PathVariable Integer idDireccion) {
        Integer idUsuario = getCurrentUserId();
        direccionService.eliminarDireccion(idDireccion, idUsuario);
        return ResponseEntity.noContent().build();
    }
}
