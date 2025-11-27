package pe.com.ikaza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.ProductoRequest;
import pe.com.ikaza.backend.dto.request.ProductoUpdateRequest;
import pe.com.ikaza.backend.dto.response.MessageResponse;
import pe.com.ikaza.backend.dto.response.ProductoDetalleResponse;
import pe.com.ikaza.backend.dto.response.ProductoResponse;
import pe.com.ikaza.backend.service.ProductoService;

import java.util.List;

/**
 * Controlador REST para gestión de productos
 */
@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    /**
     * GET /api/productos
     * Obtiene productos con paginación (público)
     */
    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> obtenerProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombreProducto") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<ProductoResponse> productos = productoService.obtenerProductosPaginados(pageable);

        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/categoria/{idCategoria}
     * Obtiene productos de una categoría con paginación (público)
     */
    @GetMapping("/categoria/{idCategoria}")
    public ResponseEntity<Page<ProductoResponse>> obtenerProductosPorCategoria(
            @PathVariable Long idCategoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombreProducto") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<ProductoResponse> productos = productoService.obtenerProductosPorCategoria(
                idCategoria, pageable);

        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/{id}
     * Obtiene un producto por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerProductoPorId(@PathVariable Long id) {
        try {
            ProductoResponse producto = productoService.obtenerProductoPorId(id);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * GET /api/productos/{id}/detalle
     * Obtiene el detalle completo de un producto (público)
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<?> obtenerDetalleProducto(@PathVariable Long id) {
        try {
            ProductoDetalleResponse detalle = productoService.obtenerDetalleProducto(id);
            return ResponseEntity.ok(detalle);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * GET /api/productos/buscar?texto=nombre
     * Busca productos por texto con paginación (público)
     */
    @GetMapping("/buscar")
    public ResponseEntity<Page<ProductoResponse>> buscarProductos(
            @RequestParam String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductoResponse> productos = productoService.buscarProductos(texto, pageable);

        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/mas-vendido
     * Obtiene el producto más vendido (público)
     */
    @GetMapping("/mas-vendido")
    public ResponseEntity<?> obtenerProductoMasVendido() {
        ProductoDetalleResponse producto = productoService.obtenerProductoMasVendido();

        if (producto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(producto);
    }

    /**
     * GET /api/productos/mas-baratos
     * Obtiene los productos más baratos (público)
     */
    @GetMapping("/mas-baratos")
    public ResponseEntity<List<ProductoResponse>> obtenerProductosMasBaratos(
            @RequestParam(defaultValue = "12") int limite) {

        List<ProductoResponse> productos = productoService.obtenerProductosMasBaratos(limite);
        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/mas-recientes
     * Obtiene los productos más recientes (público)
     */
    @GetMapping("/mas-recientes")
    public ResponseEntity<List<ProductoResponse>> obtenerProductosMasRecientes(
            @RequestParam(defaultValue = "12") int limite) {

        List<ProductoResponse> productos = productoService.obtenerProductosMasRecientes(limite);
        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/por-agotarse
     * Obtiene productos con stock entre 5 y 10 (público)
     */
    @GetMapping("/por-agotarse")
    public ResponseEntity<List<ProductoResponse>> obtenerProductosPorAgotarse(
            @RequestParam(defaultValue = "12") int limite) {

        List<ProductoResponse> productos = productoService.obtenerProductosPorAgotarse(limite);
        return ResponseEntity.ok(productos);
    }

    // ========== ENDPOINTS PARA ADMINISTRADORES ==========

    /**
     * POST /api/productos
     * Crea un nuevo producto
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoRequest request) {
        try {
            ProductoResponse producto = productoService.crearProducto(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(producto);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * PUT /api/productos/{id}
     * Actualiza un producto existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateRequest request) {
        try {
            ProductoResponse producto = productoService.actualizarProducto(id, request);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * DELETE /api/productos/{id}
     * Elimina un producto (solo admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        try {
            productoService.eliminarProducto(id);
            return ResponseEntity.ok(
                    new MessageResponse("Producto eliminado exitosamente", true));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }
}