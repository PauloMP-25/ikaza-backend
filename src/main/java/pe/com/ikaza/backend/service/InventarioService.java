package pe.com.ikaza.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.ItemPedidoRequest;
import pe.com.ikaza.backend.entity.Inventario;
import pe.com.ikaza.backend.entity.MovimientoInventario;
import pe.com.ikaza.backend.entity.Producto;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.InventarioRepository;
import pe.com.ikaza.backend.repository.MovimientoInventarioRepository;
import pe.com.ikaza.backend.repository.ProductoRepository;

import java.util.List;

/**
 * Servicio responsable de la gestión de inventario y movimientos
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    /**
     * Valida que hay stock disponible para todos los items
     * @throws StockInsuficienteException si no hay stock suficiente
     */
    public void validarStockDisponible(List<ItemPedidoRequest> items) {
        log.info("Validando stock para {} items", items.size());
        
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getIdProducto()));

            Inventario inventario = inventarioRepository.findByProductoIdProducto(producto.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Inventario no encontrado para producto: " + producto.getIdProducto()));

            if (!inventario.hayStockDisponible(item.getCantidad())) {
                throw new StockInsuficienteException(
                    String.format("Stock insuficiente para %s. Disponible: %d, Solicitado: %d",
                        producto.getNombreProducto(), 
                        inventario.getStockDisponibleCalculado(), 
                        item.getCantidad())
                );
            }
        }
        
        log.info("Validación de stock exitosa");
    }

    /**
     * Reserva stock para un pedido pendiente
     * Se usa cuando se crea un pedido con MercadoPago (pago asíncrono)
     */
    @Transactional
    public void reservarStock(List<ItemPedidoRequest> items, Long idPedido, Usuario usuario) {
        log.info("Reservando stock para pedido: {}", idPedido);
        
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            Inventario inventario = obtenerOCrearInventario(producto);
            
            inventario.reservarStock(item.getCantidad());
            inventarioRepository.save(inventario);

            registrarMovimiento(
                usuario,
                producto,
                MovimientoInventario.TipoMovimiento.AJUSTE,
                item.getCantidad(),
                inventario.getStockReservado() - item.getCantidad(),
                inventario.getStockReservado(),
                "Reserva de stock para pedido #" + idPedido
            );
        }
        
        log.info("Stock reservado exitosamente");
    }

    /**
     * Confirma la venta y reduce el stock
     * Se usa cuando el pago es aprobado
     */
    @Transactional
    public void confirmarVenta(List<ItemPedidoRequest> items, Long idPedido, Usuario usuario) {
        log.info("Confirmando venta para pedido: {}", idPedido);
        
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            Inventario inventario = inventarioRepository.findByProductoIdProducto(producto.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

            int stockAnterior = inventario.getStockActual();
            
            inventario.confirmarVenta(item.getCantidad());
            inventarioRepository.save(inventario);

            producto.setStock(inventario.getStockActual());
            productoRepository.save(producto);

            registrarMovimiento(
                usuario,
                producto,
                MovimientoInventario.TipoMovimiento.SALIDA,
                item.getCantidad(),
                stockAnterior,
                inventario.getStockActual(),
                "Venta confirmada - Pedido #" + idPedido
            );
        }
        
        log.info("Venta confirmada y stock actualizado");
    }

    /**
     * Libera stock reservado cuando un pedido es cancelado
     */
    @Transactional
    public void liberarStockReservado(List<ItemPedidoRequest> items, Long idPedido, Usuario usuario) {
        log.info("Liberando stock reservado para pedido: {}", idPedido);
        
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            Inventario inventario = inventarioRepository.findByProductoIdProducto(producto.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

            int stockReservadoAnterior = inventario.getStockReservado();
            
            inventario.liberarStockReservado(item.getCantidad());
            inventarioRepository.save(inventario);

            registrarMovimiento(
                usuario,
                producto,
                MovimientoInventario.TipoMovimiento.AJUSTE,
                item.getCantidad(),
                stockReservadoAnterior,
                inventario.getStockReservado(),
                "Liberación de stock - Pedido cancelado #" + idPedido
            );
        }
        
        log.info("Stock reservado liberado");
    }

    /**
     * Devuelve stock cuando hay un reembolso
     */
    @Transactional
    public void devolverStock(List<ItemPedidoRequest> items, Long idPedido, Usuario usuario) {
        log.info("Devolviendo stock para pedido: {}", idPedido);
        
        for (ItemPedidoRequest item : items) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            Inventario inventario = inventarioRepository.findByProductoIdProducto(producto.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

            int stockAnterior = inventario.getStockActual();
            
            inventario.agregarStock(item.getCantidad());
            inventarioRepository.save(inventario);

            producto.setStock(inventario.getStockActual());
            productoRepository.save(producto);

            registrarMovimiento(
                usuario,
                producto,
                MovimientoInventario.TipoMovimiento.DEVOLUCION,
                item.getCantidad(),
                stockAnterior,
                inventario.getStockActual(),
                "Devolución - Pedido #" + idPedido
            );
        }
        
        log.info("Stock devuelto exitosamente");
    }

    /**
     * Obtiene o crea un inventario para un producto
     */
    private Inventario obtenerOCrearInventario(Producto producto) {
        return inventarioRepository.findByProductoIdProducto(producto.getIdProducto())
                .orElseGet(() -> {
                    Inventario nuevoInventario = new Inventario();
                    nuevoInventario.setProducto(producto);
                    nuevoInventario.setStockActual(producto.getStock());
                    nuevoInventario.setStockReservado(0);
                    return inventarioRepository.save(nuevoInventario);
                });
    }

    /**
     * Registra un movimiento de inventario
     */
    private void registrarMovimiento(
            Usuario usuario,
            Producto producto,
            MovimientoInventario.TipoMovimiento tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            String motivo) {
        
        MovimientoInventario movimiento = new MovimientoInventario(
            usuario,
            producto,
            tipo,
            cantidad,
            stockAnterior,
            stockNuevo,
            motivo
        );
        
        movimientoRepository.save(movimiento);
        log.debug("Movimiento registrado: {} - {} unidades", tipo, cantidad);
    }

    /**
     * Excepción personalizada para stock insuficiente
     */
    public static class StockInsuficienteException extends RuntimeException {
        public StockInsuficienteException(String mensaje) {
            super(mensaje);
        }
    }
}