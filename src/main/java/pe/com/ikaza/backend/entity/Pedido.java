package pe.com.ikaza.backend.entity;

import pe.com.ikaza.backend.enums.EstadoPedido;
import pe.com.ikaza.backend.enums.EstadoPago;
import pe.com.ikaza.backend.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Column(name = "numero_pedido", unique = true, nullable = false, length = 50)
    private String numeroPedido;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 30)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 30)
    private EstadoPago estadoPago;

    @Column(name = "transaccion_id", length = 100)
    private String transaccionId;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Pago pago;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<HistorialEstadoPedido> historial = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPedido.PENDIENTE;
        }
        if (estadoPago == null) {
            estadoPago = EstadoPago.PENDIENTE;
        }
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void registrarHistorial(EstadoPedido estadoAnterior, EstadoPedido estadoNuevo) {
        HistorialEstadoPedido historialItem = new HistorialEstadoPedido();
        historialItem.setPedido(this);
        historialItem.setEstadoAnterior(estadoAnterior);
        historialItem.setEstadoNuevo(estadoNuevo);
        historialItem.setFechaCambio(LocalDateTime.now());
        historial.add(historialItem);
    }
}