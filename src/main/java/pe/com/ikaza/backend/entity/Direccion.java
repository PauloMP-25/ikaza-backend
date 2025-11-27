package pe.com.ikaza.backend.entity;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "direcciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Integer idDireccion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;
    
    @Column(name = "alias", length = 100)
    private String alias;
    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "distrito", length = 100)
    private String distrito;
    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "region", length = 100)
    private String region;
    
    @Column(name = "pais", nullable = false, length = 100)
    private String pais;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "es_principal", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean esPrincipal;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}