package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tarjetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarjeta")
    private Integer idMetodo;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo; 

    @Column(name = "alias", length = 50)
    private String alias; 

    @Column(name = "ultimos_4_digitos", length = 4)
    private String ultimos4Digitos;

    @Column(name = "nombre_titular", length = 100)
    private String nombreTitular;

    @Column(name = "banco_emisor", length = 30)
    private String bancoEmisor;

    @Column(name = "tipo_tarjeta", length = 20)
    private String tipoTarjeta;

    @Column(name = "fecha_expiracion", length = 7)
    private String fechaExpiracion;

    @Column(name = "token_pago", nullable = false, length = 500)
    private String tokenPago;

    @Column(name = "es_principal", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean esPrincipal; 

    @Column(name = "activo", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo;
    
    @Column(name = "fecha_creacion", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}