package pe.com.ikaza.backend.entity;

import pe.com.ikaza.backend.enums.EstadoPago;
import pe.com.ikaza.backend.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @OneToOne
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "id_metodo")
    private Long idMetodo;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_usado", nullable = false, length = 50)
    private MetodoPago metodoUsado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30)
    private EstadoPago estado;

    @Column(name = "transaccion_externa_id", length = 200)
    private String transaccionExternaId;

    @Column(name = "referencia_pasarela", length = 150)
    private String referenciaPasarela;

    @Column(name = "ultimos_4_digitos", length = 4)
    private String ultimos4Digitos;

    @Column(name = "tipo_tarjeta", length = 20)
    private String tipoTarjeta;

    @Column(name = "banco_emisor", length = 30)
    private String bancoEmisor;

    @Column(name = "datos_pasarela_json", columnDefinition = "TEXT")
    private String datosPasarelaJson;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPago.PENDIENTE;
        }
    }
}
