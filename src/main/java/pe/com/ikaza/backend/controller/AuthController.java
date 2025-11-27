package pe.com.ikaza.backend.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.LoginRequest;
import pe.com.ikaza.backend.dto.request.RegistroRequest;
import pe.com.ikaza.backend.dto.response.AuthResponse;
import pe.com.ikaza.backend.dto.response.MessageResponse;
import pe.com.ikaza.backend.service.AuthService;

/**
 * Controlador REST para autenticación
 * Maneja registro, login, refresh y logout con JWT nativo
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/registro
     * Registrar nuevo usuario con email/password (publico)
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody RegistroRequest request) {
        try {
            logger.info("Intentando registrar usuario: {}", request.getEmail());

            AuthResponse response = authService.registrarUsuario(request);

            logger.info("Usuario registrado exitosamente: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en registro: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));

        } catch (Exception e) {
            logger.error("Error inesperado en registro: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al registrar usuario", false));
        }
    }

    /**
     * POST /api/auth/login
     * Iniciar sesión con email/password (publico)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("Intento de login para: {}", request.getEmail());

            AuthResponse response = authService.login(request);

            logger.info("Login exitoso para: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("Credenciales inválidas: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Credenciales inválidas", false));

        } catch (DisabledException e) {
            logger.warn("Usuario inactivo: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Usuario inactivo. Contacte al administrador.", false));

        } catch (Exception e) {
            logger.error("Error inesperado en login: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * POST /api/auth/refresh
     * Renovar access token usando refresh token (publico)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        try {
            logger.info("Solicitando renovación de token");

            AuthResponse response = authService.refreshToken(refreshToken);

            logger.info("Token renovado exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error al renovar token: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error al renovar token: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/auth/verificar-token
     * Verificar si un token JWT es válido (publico)
     */
    @PostMapping("/verificar-token")
    public ResponseEntity<?> verificarToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Token no proporcionado", false));
            }

            String token = authHeader.substring(7);
            logger.info("Verificando token...");

            AuthResponse response = authService.verificarToken(token);

            logger.info("Token válido para: {}", response.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warn("Token inválido: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Token inválido o expirado", false));
        }
    }

    /**
     * POST /api/auth/logout
     * Cerrar sesión (invalida refresh token, requiere autenticacion)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String email) {
        try {
            logger.info("Cerrando sesión para: {}", email);

            authService.logout(email);

            return ResponseEntity.ok(new MessageResponse("Sesión cerrada exitosamente", true));

        } catch (Exception e) {
            logger.error("Error al cerrar sesión: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al cerrar sesión", false));
        }
    }

    /**
     * GET /api/auth/verificar-email/{email}
     * Verificar si un email ya está registrado (publico)
     */
    @GetMapping("/verificar-email/{email}")
    public ResponseEntity<?> verificarEmailDisponible(@PathVariable String email) {
        try {
            boolean disponible = authService.verificarEmailDisponible(email);

            return ResponseEntity.ok(new MessageResponse(
                    disponible ? "Email disponible" : "Email ya registrado",
                    disponible));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al verificar email", false));
        }
    }
}