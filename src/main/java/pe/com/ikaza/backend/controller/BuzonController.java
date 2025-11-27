// BuzonController.java
package pe.com.ikaza.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import pe.com.ikaza.backend.dto.request.BuzonRequest;
import pe.com.ikaza.backend.dto.response.BuzonResponse;
import pe.com.ikaza.backend.dto.response.ListaMensajesResponse;
import pe.com.ikaza.backend.dto.response.MensajeBuzonDTO;
import pe.com.ikaza.backend.dto.response.EstadisticasBuzonResponse;
import pe.com.ikaza.backend.exception.ResourceNotFoundException;
import pe.com.ikaza.backend.service.BuzonService;
import pe.com.ikaza.backend.service.UsuarioService;
import pe.com.ikaza.backend.utils.SecurityUtils;

/**
 * Controlador REST para enviar correos
 * Maneja envio, lectura y datos de los correos enviados del usuario a la
 * empresa.
 */
@RestController
@RequestMapping("/api/buzon")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${app.frontend.url:http://localhost:4200}")
public class BuzonController {

    private final BuzonService buzonService;
    
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
     * Endpoint para enviar mensaje al buzón (reclamo o recomendación)
     * POST /api/buzon/enviar (publico)
     */
    @PostMapping(value = "/enviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BuzonResponse> enviarMensaje(
            @Valid @ModelAttribute BuzonRequest request,
            Authentication authentication) {

        try {
            Integer idUsuario = getCurrentUserId();

            log.info("Recibiendo mensaje de buzón - Tipo: {}, Usuario: {}", request.getTipo(), idUsuario);

            BuzonResponse response = buzonService.enviarMensaje(request, idUsuario);
            return ResponseEntity.ok(response);

        } catch (UsernameNotFoundException e) {
            log.error("Error de autenticación - Usuario no encontrado", e);
            return ResponseEntity.ok(BuzonResponse.builder()
                    .success(false)
                    .mensaje("Error de autenticación: El usuario asociado no pudo ser encontrado.")
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Error de validación o conversión de datos", e);
            return ResponseEntity.ok(BuzonResponse.builder()
                    .success(false)
                    .mensaje(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error al procesar mensaje de buzón", e);
            return ResponseEntity.ok(BuzonResponse.builder()
                    .success(false)
                    .mensaje("Hubo un error al enviar tu mensaje. Por favor, intenta nuevamente.")
                    .build());
        }
    }

    /**
     * Endpoint para obtener mensajes del usuario actual
     * GET /api/buzon/mis-mensajes (requiere autenticacion)
     */
    @GetMapping("/mis-mensajes")
    public ResponseEntity<ListaMensajesResponse> obtenerMisMensajes(Authentication authentication) {
        try {
            Integer idUsuario = getCurrentUserId();

            log.info("Obteniendo mensajes del usuario: {}", idUsuario);

            ListaMensajesResponse response = buzonService.obtenerMensajesUsuario(idUsuario);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener mensajes", e);
            return ResponseEntity.ok(ListaMensajesResponse.builder()
                    .success(false)
                    .build());
        }
    }

    /**
     * Endpoint para marcar un mensaje como leído
     * PUT /api/buzon/{idMensaje}/marcar-leido (requiere autenticacion y rol de
     * administrador)
     */
    @PutMapping("/{idMensaje}/marcar-leido")
    public ResponseEntity<?> marcarComoLeido(
            @PathVariable Integer idMensaje,
            Authentication authentication) {
        try {
            Integer idUsuario = getCurrentUserId();

            buzonService.marcarComoLeido(idMensaje, idUsuario);

            return ResponseEntity.ok(new MessageResponse(true, "Mensaje marcado como leído"));

        } catch (SecurityException e) {
            log.error("Intento de acceso no autorizado", e);
            return ResponseEntity.ok(new MessageResponse(false, "No autorizado"));
        } catch (Exception e) {
            log.error("Error al marcar mensaje como leído", e);
            return ResponseEntity.ok(new MessageResponse(false, "Error al procesar solicitud"));
        }
    }

    // ========== ENDPOINTS PARA ADMINISTRADORES ==========

    /**
     * Obtiene todos los mensajes (solo admin)
     * GET /api/buzon/admin/todos
     */
    @GetMapping("/admin/todos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ListaMensajesResponse> obtenerTodosMensajes() {
        try {
            log.info("Obteniendo todos los mensajes (Admin)");
            ListaMensajesResponse response = buzonService.obtenerTodosMensajes();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener todos los mensajes", e);
            return ResponseEntity.ok(ListaMensajesResponse.builder()
                    .success(false)
                    .build());
        }
    }

    /**
     * Obtiene mensajes por tipo (solo admin)
     * GET /api/buzon/admin/tipo/{tipo}
     */
    @GetMapping("/admin/tipo/{tipo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ListaMensajesResponse> obtenerMensajesPorTipo(
            @PathVariable String tipo) {
        try {
            log.info("Obteniendo mensajes tipo: {}", tipo);
            ListaMensajesResponse response = buzonService.obtenerMensajesPorTipo(tipo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener mensajes por tipo", e);
            return ResponseEntity.ok(ListaMensajesResponse.builder()
                    .success(false)
                    .build());
        }
    }

    /**
     * Obtiene detalle completo de un mensaje (solo admin)
     * GET /api/buzon/admin/{idMensaje}
     */
    @GetMapping("/admin/{idMensaje}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> obtenerDetalleMensaje(@PathVariable Integer idMensaje) {
        try {
            log.info("Obteniendo detalle del mensaje: {}", idMensaje);
            MensajeBuzonDTO mensaje = buzonService.obtenerDetalleMensaje(idMensaje);
            return ResponseEntity.ok(new DetalleMensajeResponse(true, mensaje));
        } catch (ResourceNotFoundException e) {
            log.error("Mensaje no encontrado: {}", idMensaje);
            return ResponseEntity.ok(new MessageResponse(false, "Mensaje no encontrado"));
        } catch (Exception e) {
            log.error("Error al obtener detalle del mensaje", e);
            return ResponseEntity.ok(new MessageResponse(false, "Error al obtener el detalle"));
        }
    }

    /**
     * Actualiza el estado de un mensaje (solo admin)
     * PUT /api/buzon/admin/{idMensaje}/estado
     */
    @PutMapping("/admin/{idMensaje}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MessageResponse> actualizarEstado(
            @PathVariable Integer idMensaje,
            @RequestBody EstadoUpdateRequest request,
            Authentication authentication) {
        try {
            log.info("Actualizando estado del mensaje {} a {}", idMensaje, request.estado());

            buzonService.actualizarEstado(idMensaje, request.estado());

            return ResponseEntity.ok(new MessageResponse(true, "Estado actualizado correctamente"));
        } catch (Exception e) {
            log.error("Error al actualizar estado", e);
            return ResponseEntity.ok(new MessageResponse(false, "Error al actualizar el estado"));
        }
    }

    /**
     * Obtiene estadísticas del buzón (solo admin)
     * GET /api/buzon/admin/estadisticas
     */
    @GetMapping("/admin/estadisticas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstadisticasBuzonResponse> obtenerEstadisticas() {
        try {
            log.info("Obteniendo estadísticas del buzón");
            EstadisticasBuzonResponse response = buzonService.obtenerEstadisticas();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas", e);
            return ResponseEntity.ok(EstadisticasBuzonResponse.builder()
                    .success(false)
                    .build());
        }
    }

    /**
     * Descarga un archivo adjunto
     * GET /api/buzon/archivo/{nombreArchivo}
     */
    @GetMapping("/archivo/{nombreArchivo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> descargarArchivo(@PathVariable String nombreArchivo) {
        try {
            Resource archivo = buzonService.obtenerArchivo(nombreArchivo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + archivo.getFilename() + "\"")
                    .body(archivo);
        } catch (Exception e) {
            log.error("Error al descargar archivo", e);
            return ResponseEntity.notFound().build();
        }
    }

    // Clases auxiliares para respuestas
    private record MessageResponse(boolean success, String message) {
    }

    private record DetalleMensajeResponse(boolean success, MensajeBuzonDTO mensaje) {
    }

    private record EstadoUpdateRequest(String estado) {
    }
}
