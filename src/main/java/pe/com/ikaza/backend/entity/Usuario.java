package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad Usuario (Core de Autenticación)
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    // =========================================
    // DATOS DE AUTENTICACIÓN
    // =========================================
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol = "CLIENTE";
    
    @Column(name = "username", length = 100)
    private String username;

    // =========================================
    // TOKENS Y SEGURIDAD
    // =========================================
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "token_expiracion")
    private LocalDateTime tokenExpiracion;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    // =========================================
    // RELACIÓN CON CLIENTE
    // =========================================
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cliente cliente;

    // =========================================
    // PERFIL BÁSICO
    // =========================================
    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil; // URL de la imagen

    @Column(name = "proveedor_auth", length = 50)
    private String proveedorAuth = "LOCAL";

    // =========================================
    // METADATOS
    // =========================================
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // =========================================
    // MÉTODOS DE CICLO DE VIDA
    // =========================================
    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (ultimoAcceso == null) {
            ultimoAcceso = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
        if (rol == null) {
            rol = "CLIENTE";
        }
        if (emailVerificado == null) {
            emailVerificado = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        ultimoAcceso = LocalDateTime.now();
    }

    // =========================================
    // MÉTODOS AUXILIARES
    // =========================================

    /**
     * Verificar si el usuario está bloqueado temporalmente
     */
    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    /**
     * Resetear intentos fallidos después de login exitoso
     */
    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /**
     * Incrementar intentos fallidos (bloquear después de 5)
     */
    public void incrementarIntentosFallidos() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 5) {
            // Bloquear por 15 minutos
            this.bloqueadoHasta = LocalDateTime.now().plusMinutes(15);
        }
    }
}