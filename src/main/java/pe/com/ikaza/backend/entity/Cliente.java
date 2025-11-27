package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Cliente: datos personales
 * Extiende la información de la entidad Usuario (1:1).
 */
@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    // RELACIÓN UNO A UNO con Usuario
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true, nullable = false)
    private Usuario usuario;

    @Column(name = "nombres_cliente", nullable = false, length = 100)
    private String nombresCliente;

    @Column(name = "apellidos_cliente", nullable = false, length = 100)
    private String apellidosCliente;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", unique = true, length = 20)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "prefijo_telefono", length = 5)
    private String prefijoTelefono;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "telefono_verificado", nullable = false)
    private Boolean telefonoVerificado = false;

    @Column(name = "genero", length = 15)
    private String genero;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    public String getNombreCompleto() {
        return nombresCliente + " " + apellidosCliente;
    }
    @PrePersist
    protected void onCreate() {
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
    }
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}