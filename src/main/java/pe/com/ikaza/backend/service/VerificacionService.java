package pe.com.ikaza.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.entity.CodigoVerificacion;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.ClienteRepository;
import pe.com.ikaza.backend.repository.CodigoVerificacionRepository;
import pe.com.ikaza.backend.repository.UsuarioRepository;
import pe.com.ikaza.backend.entity.Cliente;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificacionService {

    private final CodigoVerificacionRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    private static final SecureRandom random = new SecureRandom();

    // ============================================================================
    // VERIFICACIÓN DE EMAIL
    // ============================================================================

    /**
     * Enviar código de verificación por email
     */
    @Transactional
    public void sendEmailVerificationCode(String email) {
        try {
            // Generar código de 6 dígitos
            String codigo = generarCodigo();

            // Guardar en BD
            CodigoVerificacion codigoVerificacion = CodigoVerificacion.builder()
                    .email(email)
                    .codigo(codigo)
                    .tipo(CodigoVerificacion.TipoVerificacion.EMAIL)
                    .usado(false)
                    .build();

            codigoRepository.save(codigoVerificacion);

            // Enviar email
            emailService.enviarCodigoVerificacion(email, codigo);

            log.info("Código de verificación enviado a: {}", email);

        } catch (Exception e) {
            log.error("Error enviando código de verificación: {}", e.getMessage());
            throw new RuntimeException("Error al enviar código de verificación");
        }
    }

    /**
     * Verificar código de email
     */
    @Transactional
    public boolean verifyEmailCode(String email, String codigo) {
        CodigoVerificacion codigoVerificacion = codigoRepository
                .findTopByEmailAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        email,
                        CodigoVerificacion.TipoVerificacion.EMAIL)
                .orElseThrow(() -> new IllegalArgumentException("No hay código pendiente"));

        if (!codigoVerificacion.isValido(codigo)) {
            throw new IllegalArgumentException("Código inválido o expirado");
        }

        // Marcar código como usado
        codigoVerificacion.setUsado(true);
        codigoVerificacion.setFechaUso(LocalDateTime.now());
        codigoRepository.save(codigoVerificacion);

        // Marcar email como verificado en Usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        log.info("Email verificado: {}", email);
        return true;
    }

    /**
     * Verificar si el email está verificado
     */
    public boolean isEmailVerified(String email) {
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getEmailVerificado)
                .orElse(false);
    }

    // ============================================================================
    // VERIFICACIÓN DE TELÉFONO
    // ============================================================================

    /**
     * Enviar código de verificación por SMS
     */
    @Transactional
    public void sendPhoneVerificationCode(String telefono) {
        try {
            // Validar formato de teléfono
            if (!smsService.isValidPhoneNumber(telefono)) {
                throw new IllegalArgumentException(
                        "Formato de teléfono inválido. Debe incluir código de país (+51...)");
            }

            // Generar código
            String codigo = generarCodigo();

            // Guardar en BD
            CodigoVerificacion codigoVerificacion = CodigoVerificacion.builder()
                    .telefono(telefono)
                    .codigo(codigo)
                    .tipo(CodigoVerificacion.TipoVerificacion.PHONE)
                    .usado(false)
                    .build();

            codigoRepository.save(codigoVerificacion);

            // Enviar SMS
            smsService.enviarCodigoVerificacion(telefono, codigo);

            log.info("Código SMS enviado a: {}", telefono);

        } catch (Exception e) {
            log.error("Error enviando SMS: {}", e.getMessage());
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage());
        }
    }

    /**
     * Verificar código de teléfono
     */
    @Transactional
    public boolean verifyPhoneCode(String email, String telefono, String codigo) {
        CodigoVerificacion codigoVerificacion = codigoRepository
                .findTopByTelefonoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        telefono,
                        CodigoVerificacion.TipoVerificacion.PHONE)
                .orElseThrow(() -> new IllegalArgumentException("No hay código pendiente"));

        if (!codigoVerificacion.isValido(codigo)) {
            throw new IllegalArgumentException("Código inválido o expirado");
        }

        // Marcar código como usado
        codigoVerificacion.setUsado(true);
        codigoVerificacion.setFechaUso(LocalDateTime.now());
        codigoRepository.save(codigoVerificacion);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        // Marcar teléfono como verificado en Cliente
        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cliente.setTelefonoVerificado(true);
        clienteRepository.save(cliente);

        log.info("Teléfono verificado: {}", telefono);
        return true;
    }

    /**
     * Verificar si el teléfono está verificado
     */
    public boolean isPhoneVerified(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        // Marcar teléfono como verificado en Cliente
        return clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .map(Cliente::getTelefonoVerificado)
                .orElse(false);
    }

    // ============================================================================
    // UTILIDADES
    // ============================================================================

    /**
     * Generar código de 6 dígitos
     */
    private String generarCodigo() {
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    /**
     * Tarea programada para limpiar códigos expirados (cada hora)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void limpiarCodigosExpirados() {
        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        codigoRepository.eliminarCodigosExpirados(hace24Horas);
        log.info("🧹 Códigos expirados eliminados");
    }
}