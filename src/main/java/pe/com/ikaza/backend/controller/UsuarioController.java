package pe.com.ikaza.backend.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.ActualizarFotoPerfilRequest;
import pe.com.ikaza.backend.dto.request.ActualizarUsernameRequest;
import pe.com.ikaza.backend.dto.request.ActualizarPasswordRequest;
import pe.com.ikaza.backend.dto.response.MessageResponse;
import pe.com.ikaza.backend.dto.response.UsuarioResponse;
import pe.com.ikaza.backend.service.UsuarioService;
import pe.com.ikaza.backend.utils.SecurityUtils;

/**
 * Controlador REST para gestión de Usuario
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SecurityUtils securityUtils;

    // ===============================================
    // OBTENER PERFIL BÁSICO
    // ===============================================

    /**
     * GET /api/usuarios/perfil/{email}
     * Obtener información básica del usuario (requiere autenticacion)
     */
    @GetMapping("/perfil/{email}")
    public ResponseEntity<?> obtenerPerfil(@PathVariable String email) {
        try {
            logger.info("Obteniendo perfil de usuario: {}", email);
            UsuarioResponse usuario = usuarioService.obtenerPorEmail(email);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            logger.warn("Usuario no encontrado: {}", email);
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * PUT /api/usuarios/perfil/{email}
     * Actualizar username del usuario (requiere autenticacion)
     */
    @PutMapping("/perfil/{email}")
    public ResponseEntity<?> actualizarUsername(
            @PathVariable String email,
            @Valid @RequestBody ActualizarUsernameRequest request) {
        try {
            logger.info("Actualizando username para: {}", email);
            UsuarioResponse usuario = usuarioService.actualizarUsername(email, request);
            logger.info("Username actualizado exitosamente");
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            logger.error("❌ Error al actualizar username: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    // ===============================================
    // ACTUALIZAR FOTO DE PERFIL
    // ===============================================

    /**
     * PUT /api/usuarios/perfil/{email}/imagen
     * Actualizar foto de perfil o icono (requiere autenticacion)
     */
    @PutMapping("/perfil/{email}/imagen")
    public ResponseEntity<?> actualizarFotoPerfil(
            @PathVariable String email,
            @RequestBody ActualizarFotoPerfilRequest request) {
        try {
            logger.info("🖼️ Actualizando foto de perfil para: {}", email);
            UsuarioResponse usuario = usuarioService.actualizarFotoPerfil(email, request);
            logger.info("Foto de perfil actualizada exitosamente");
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            logger.error("Error al actualizar foto: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * DELETE /api/usuarios/perfil/{email}/imagen
     * Eliminar foto de perfil (resetear a icono por defecto) (requiere
     * autenticacion)
     */
    @DeleteMapping("/perfil/{email}/imagen")
    public ResponseEntity<?> eliminarFotoPerfil(@PathVariable String email) {
        try {
            logger.info("Eliminando foto de perfil para: {}", email);

            ActualizarFotoPerfilRequest request = new ActualizarFotoPerfilRequest();
            request.setPhotoURL("bi-person-circle");

            UsuarioResponse usuario = usuarioService.actualizarFotoPerfil(email, request);
            logger.info("Foto eliminada exitosamente");
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            logger.error("Error al eliminar foto: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    // ===============================================
    // CAMBIAR CONTRASEÑA
    // ===============================================

    /**
     * POST /api/usuarios/cambiar-password
     * Cambiar contraseña del usuario (requiere autenticacion)
     */
    @PostMapping("/perfil/{email}/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @Valid @RequestBody ActualizarPasswordRequest request) {
        try {
            String email = securityUtils.getCurrentUserEmail();
            logger.info("Cambiando contraseña para: {}", email);
            usuarioService.cambiarPassword(email, request);
            logger.info("Contraseña cambiada exitosamente");
            return ResponseEntity.ok(new MessageResponse("Contraseña cambiada correctamente", true));
        } catch (RuntimeException e) {
            logger.error("Error al cambiar contraseña: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }
}
