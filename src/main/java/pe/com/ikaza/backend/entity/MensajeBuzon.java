package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_buzon")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeBuzon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "tipo_mensaje", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoMensaje tipoMensaje;

    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "categoria_reclamo", length = 50)
    @Enumerated(EnumType.STRING)
    private CategoriaReclamo categoriaReclamo;

    @Column(name = "reclamo_otro", length = 200)
    private String reclamoOtro;

    @Column(name = "urgencia_reclamo", length = 20)
    @Enumerated(EnumType.STRING)
    private UrgenciaReclamo urgenciaReclamo;

    @Column(name = "archivo_adjunto")
    private String archivoAdjunto;

    @Column(name = "archivo_evidencia")
    private String archivoEvidencia;

    @Column(name = "estado", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoMensaje estado = EstadoMensaje.PENDIENTE;

    @Column(name = "leido")
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por")
    private Usuario respondidoPor;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public enum TipoMensaje {
        RECOMENDACION, RECLAMO
    }

    public enum CategoriaReclamo {
        PRODUCTO, ENTREGA, ATENCION, FACTURACION, OTRO
    }

    public enum UrgenciaReclamo {
        NORMAL, ALTA
    }

    public enum EstadoMensaje {
        PENDIENTE, EN_REVISION, RESPONDIDO, CERRADO
    }

    public void marcarComoLeido() {
        this.leido = true;
        this.fechaLectura = LocalDateTime.now();
    }

    public void responder(String respuesta, Usuario respondidoPor) {
        this.respuesta = respuesta;
        this.fechaRespuesta = LocalDateTime.now();
        this.respondidoPor = respondidoPor;
        this.estado = EstadoMensaje.RESPONDIDO;
    }
}
