// CodigoVerificacion.java
package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_verificacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "telefono")
    private String telefono;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVerificacion tipo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    public enum TipoVerificacion {
        EMAIL, PHONE
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaExpiracion = LocalDateTime.now().plusMinutes(10);
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean isValido(String codigoIngresado) {
        return !usado && !isExpirado() && codigo.equals(codigoIngresado);
    }
}