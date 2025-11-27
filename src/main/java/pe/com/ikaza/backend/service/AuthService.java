package pe.com.ikaza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.LoginRequest;
import pe.com.ikaza.backend.dto.request.RegistroRequest;
import pe.com.ikaza.backend.dto.response.AuthResponse;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.UsuarioRepository;
import pe.com.ikaza.backend.security.JwtUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de autenticación
 * Maneja registro, login y tokens JWT propios
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private ClienteService clienteService;

    /**
     * REGISTRO: Crear nuevo usuario con email y password
     */
    public AuthResponse registrarUsuario(RegistroRequest request) {
        logger.info("Iniciando registro para: {}", request.getEmail());

        // Validar si el email ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Validar formato de email
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Formato de email inválido");
        }

        // Validar longitud de contraseña
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        
        // Validar username si se proporciona
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            if (usuarioRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("El nombre de usuario ya está en uso");
            }
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setEmail(request.getEmail());
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            usuario.setRol("CLIENTE");
            usuario.setActivo(true);
            usuario.setEmailVerificado(false);
            usuario.setProveedorAuth("LOCAL");
            
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                usuario.setUsername(request.getUsername().trim());
            } else {
                //Si no se proporciona, usar parte del email (se maneja en @PrePersist)
                usuario.setUsername(request.getEmail().split("@")[0]);
            }

            Usuario usuarioGuardado = usuarioRepository.save(usuario);

            logger.info("Usuario registrado exitosamente - ID: {}", usuarioGuardado.getIdUsuario());

            clienteService.crearPerfilInicial(usuarioGuardado.getEmail()); 
            logger.info("Perfil Cliente inicial creado automáticamente para: {}", usuarioGuardado.getEmail());
            
            String accessToken = jwtUtils.generateTokenFromUsername(usuarioGuardado.getEmail());
            String refreshToken = jwtUtils.generateRefreshToken(usuarioGuardado.getEmail());

            // Guardar refresh token
            usuarioGuardado.setRefreshToken(refreshToken);
            usuarioGuardado.setTokenExpiracion(LocalDateTime.now().plusDays(7));
            usuarioRepository.save(usuarioGuardado);

            return construirAuthResponse(usuarioGuardado, accessToken, refreshToken,
                    "Usuario registrado exitosamente. Complete su perfil para continuar.");

        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage());
        }
    }

    /**
     * LOGIN: Autenticar con email y password
     */
    public AuthResponse login(LoginRequest request) {
        logger.info("Intento de login para: {}", request.getEmail());

        try {
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

            if (usuario.estaBloqueado()) {
                throw new RuntimeException("Usuario bloqueado temporalmente. Intente más tarde.");
            }

            if (!usuario.getActivo()) {
                throw new DisabledException("Usuario inactivo. Contacte al administrador.");
            }

            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                usuario.resetearIntentosFallidos();

                usuario.setUltimoAcceso(LocalDateTime.now());

                String accessToken = jwtUtils.generateJwtToken(authentication);
                String refreshToken = jwtUtils.generateRefreshToken(usuario.getEmail());

                usuario.setRefreshToken(refreshToken);
                usuario.setTokenExpiracion(LocalDateTime.now().plusDays(7));
                usuarioRepository.save(usuario);

                logger.info("Login exitoso para: {}", request.getEmail());

                return construirAuthResponse(usuario, accessToken, refreshToken, "Login exitoso");

            } catch (BadCredentialsException e) {
                usuario.incrementarIntentosFallidos();
                usuarioRepository.save(usuario);

                logger.warn("Credenciales inválidas para: {}", request.getEmail());
                throw new BadCredentialsException("La contraseña es incorrecta");
            }

        } catch (BadCredentialsException e) {
            throw e;
        } catch (DisabledException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en login: {}", e.getMessage());
            throw new RuntimeException("Error al iniciar sesión: " + e.getMessage());
        }
    }

    /**
     * REFRESH: Renovar access token usando refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        logger.info("Solicitando renovación de token");

        try {
            if (!jwtUtils.validateJwtToken(refreshToken)) {
                throw new RuntimeException("Refresh token inválido o expirado");
            }

            String email = jwtUtils.getUserEmailFromJwtToken(refreshToken);

            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!refreshToken.equals(usuario.getRefreshToken())) {
                throw new RuntimeException("Refresh token no coincide");
            }

            if (usuario.getTokenExpiracion().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Refresh token expirado");
            }

            String newAccessToken = jwtUtils.generateTokenFromUsername(usuario.getEmail());

            logger.info("Token renovado para: {}", email);

            return construirAuthResponse(usuario, newAccessToken, refreshToken, "Token renovado exitosamente");

        } catch (Exception e) {
            logger.error("Error al renovar token: {}", e.getMessage());
            throw new RuntimeException("Error al renovar token: " + e.getMessage());
        }
    }

    /**
     * VERIFICAR TOKEN: Validar si un token es válido
     */
    @Transactional(readOnly = true)
    public AuthResponse verificarToken(String token) {
        try {
            if (!jwtUtils.validateJwtToken(token)) {
                throw new RuntimeException("Token inválido o expirado");
            }

            String email = jwtUtils.getUserEmailFromJwtToken(token);
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!usuario.getActivo()) {
                throw new RuntimeException("Usuario inactivo");
            }

            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);

            return construirAuthResponse(usuario, token, usuario.getRefreshToken(), "Token válido");

        } catch (Exception e) {
            logger.error("Error al verificar token: {}", e.getMessage());
            throw new RuntimeException("Token inválido o expirado");
        }
    }

    /**
     * LOGOUT: Invalidar refresh token
     */
    public void logout(String email) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                usuario.setRefreshToken(null);
                usuario.setTokenExpiracion(null);
                usuario.setUltimoAcceso(LocalDateTime.now());
                usuarioRepository.save(usuario);

                logger.info("Logout exitoso para: {}", email);
            }
        } catch (Exception e) {
            logger.error("Error en logout: {}", e.getMessage());
        }
    }

    /**
     * VERIFICAR EMAIL DISPONIBLE
     */
    @Transactional(readOnly = true)
    public boolean verificarEmailDisponible(String email) {
        return !usuarioRepository.existsByEmail(email);
    }

    /**
     * CONSTRUIR RESPUESTA AUTH
     */
    private AuthResponse construirAuthResponse(Usuario usuario, String accessToken, String refreshToken,
            String mensaje) {
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .idUsuario(usuario.getIdUsuario())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .isAdmin("ADMINISTRADOR".equals(usuario.getRol()))
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .mensaje(mensaje)
                .success(true)
                .build();
    }
}