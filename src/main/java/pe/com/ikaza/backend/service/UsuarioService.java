package pe.com.ikaza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.ActualizarFotoPerfilRequest;
import pe.com.ikaza.backend.dto.request.ActualizarUsernameRequest;
import pe.com.ikaza.backend.dto.request.ActualizarPasswordRequest;
import pe.com.ikaza.backend.dto.response.UsuarioResponse;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.UsuarioRepository;

/**
 * Servicio para gestión de perfil de Usuario
 * (username, foto_perfil, password)
 */
@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================
    // ACTUALIZAR USERNAME
    // =========================================

    /**
     * Actualizar username del usuario
     */
    @Transactional
    public UsuarioResponse actualizarUsername(String email, ActualizarUsernameRequest request) {
        logger.info("Actualizando username para: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si el username ya existe (opcional)
        if (!request.getUsername().equals(usuario.getUsername()) &&
                usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El username ya está en uso");
        }

        usuario.setUsername(request.getUsername());
        usuarioRepository.save(usuario);

        logger.info("Username actualizado exitosamente");
        return mapearUsuarioResponse(usuario);
    }

    // =========================================
    // ACTUALIZAR FOTO DE PERFIL
    // =========================================

    /**
     * Actualizar foto de perfil o icono
     */
    @Transactional
    public UsuarioResponse actualizarFotoPerfil(String email, ActualizarFotoPerfilRequest request) {
        logger.info("Actualizando foto de perfil para: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setFotoPerfil(request.getPhotoURL());
        usuarioRepository.save(usuario);

        logger.info("Foto de perfil actualizada exitosamente");
        return mapearUsuarioResponse(usuario);
    }

    // =========================================
    // CAMBIAR CONTRASEÑA
    // =========================================

    /**
     * Cambiar contraseña del usuario
     */
    @Transactional
    public void cambiarPassword(String email, ActualizarPasswordRequest request) {
        logger.info("Cambiando contraseña para: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // Validar que la nueva contraseña sea diferente
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
        }

        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);

        logger.info("Contraseña cambiada exitosamente");
    }

    // =========================================
    // OBTENER PERFIL
    // =========================================

    /**
     * Obtener información del usuario por email
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapearUsuarioResponse(usuario);
    }

    // =========================================
    // MAPEO
    // =========================================

    /**
     * Mapear Usuario a UsuarioResponse
     */
    private UsuarioResponse mapearUsuarioResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setEmail(usuario.getEmail());
        response.setUsername(usuario.getUsername());
        response.setFotoPerfil(usuario.getFotoPerfil());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());
        response.setEmailVerificado(usuario.getEmailVerificado());
        response.setProveedorAuth(usuario.getProveedorAuth());
        response.setFechaCreacion(usuario.getFechaCreacion());
        response.setUltimoAcceso(usuario.getUltimoAcceso());
        
        return response;
    }
}