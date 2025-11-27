package pe.com.ikaza.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @autor Paulo
 * Entidad que representa el control de inventario de productos
 */
@Entity
@Table(name = "inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Long idInventario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(name = "stock_reservado", nullable = false)
    private Integer stockReservado = 0;

    @Column(name = "stock_disponible", insertable = false, updatable = false)
    private Integer stockDisponible;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaActualizacion = LocalDateTime.now();
        if (stockActual == null) {
            stockActual = 0;
        }
        if (stockReservado == null) {
            stockReservado = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Métodos de utilidad para gestionar el inventario
     */

    /**
     * Agrega stock al inventario (entrada)
     */
    public void agregarStock(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.stockActual += cantidad;
    }

    /**
     * Reduce stock del inventario (salida)
     */
    public void reducirStock(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (stockActual < cantidad) {
            throw new IllegalStateException("Stock insuficiente. Actual: " + stockActual);
        }
        this.stockActual -= cantidad;
    }

    /**
     * Reserva stock para un pedido pendiente
     */
    public void reservarStock(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        
        int disponible = stockActual - stockReservado;
        
        if (disponible < cantidad) {
            throw new IllegalStateException(
                "Stock disponible insuficiente. Disponible: " + disponible + 
                ", Solicitado: " + cantidad
            );
        }
        
        this.stockReservado += cantidad;
    }

    /**
     * Libera stock reservado (cuando se cancela un pedido)
     */
    public void liberarStockReservado(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (stockReservado < cantidad) {
            throw new IllegalStateException("No hay suficiente stock reservado para liberar");
        }
        this.stockReservado -= cantidad;
    }

    /**
     * Confirma una venta (reduce stock actual y reservado)
     */
    public void confirmarVenta(Integer cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (stockReservado < cantidad) {
            throw new IllegalStateException("Stock reservado insuficiente");
        }
        if (stockActual < cantidad) {
            throw new IllegalStateException("Stock actual insuficiente");
        }
        
        this.stockReservado -= cantidad;
        this.stockActual -= cantidad;
    }

    /**
     * Verifica si hay stock disponible
     */
    public boolean hayStockDisponible(Integer cantidad) {
        int disponible = stockActual - stockReservado;
        return disponible >= cantidad;
    }

    /**
     * Obtiene el stock disponible calculado
     */
    public Integer getStockDisponibleCalculado() {
        return stockActual - stockReservado;
    }
}