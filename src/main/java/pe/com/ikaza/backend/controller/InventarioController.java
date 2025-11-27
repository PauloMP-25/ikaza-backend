package pe.com.ikaza.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.AjusteStockRequest;
import pe.com.ikaza.backend.dto.response.InventarioResponse;
import pe.com.ikaza.backend.dto.response.MessageResponse;
import pe.com.ikaza.backend.dto.response.MovimientoInventarioResponse;
import pe.com.ikaza.backend.service.InventarioAdminService;
import pe.com.ikaza.backend.service.UsuarioService;
import pe.com.ikaza.backend.utils.SecurityUtils;

import java.util.List;

/**
 * Controlador REST para gestión administrativa de inventario
 */
@RestController
@RequestMapping("/api/inventario")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioAdminService inventarioAdminService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SecurityUtils securityUtils;

    // ========== METODO AUXILIAR ==========

    /**
     * Obtiene el ID del usuario autenticado a partir del token JWT.
     */
    private Integer getCurrentUserId() {
        String email = securityUtils.getCurrentUserEmail();

        if (email == null) {
            throw new RuntimeException("Usuario no autenticado o token no contiene email.");
        }
        return usuarioService.obtenerPorEmail(email).getIdUsuario();
    }

    // ========== ENDPOINTS PARA ADMINISTRADORES ==========

    /**
     * GET /api/inventario
     * Obtiene el inventario de todos los productos
     */
    @GetMapping
    public ResponseEntity<List<InventarioResponse>> obtenerTodosLosInventarios() {
        List<InventarioResponse> inventarios = inventarioAdminService.obtenerTodosLosInventarios();
        return ResponseEntity.ok(inventarios);
    }

    /**
     * GET /api/inventario/producto/{idProducto}
     * Obtiene el inventario de un producto específico
     */
    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<?> obtenerInventarioPorProducto(@PathVariable Long idProducto) {
        try {
            InventarioResponse inventario = inventarioAdminService.obtenerInventarioPorProducto(idProducto);
            return ResponseEntity.ok(inventario);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * POST /api/inventario/producto/{idProducto}/ajustar
     * Ajusta el stock de un producto (entrada, salida o ajuste manual)
     */
    @PostMapping("/producto/{idProducto}/ajustar")
    public ResponseEntity<?> ajustarStock(
            @PathVariable Long idProducto,
            @Valid @RequestBody AjusteStockRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Integer idUsuario = getCurrentUserId();

            InventarioResponse inventario = inventarioAdminService.ajustarStock(
                    idProducto,
                    request,
                    idUsuario);
            return ResponseEntity.ok(inventario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al ajustar stock: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/inventario/movimientos/producto/{idProducto}
     * Obtiene el historial de movimientos de un producto
     */
    @GetMapping("/movimientos/producto/{idProducto}")
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerMovimientosPorProducto(
            @PathVariable Long idProducto) {
        List<MovimientoInventarioResponse> movimientos = inventarioAdminService
                .obtenerMovimientosPorProducto(idProducto);
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/inventario/movimientos/ultimos
     * Obtiene los últimos movimientos del sistema
     */
    @GetMapping("/movimientos/ultimos")
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerUltimosMovimientos(
            @RequestParam(defaultValue = "50") int limite) {
        List<MovimientoInventarioResponse> movimientos = inventarioAdminService.obtenerUltimosMovimientos(limite);
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/inventario/stock-bajo
     * Obtiene productos con stock bajo (≤5)
     */
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<InventarioResponse>> obtenerInventariosConStockBajo() {
        List<InventarioResponse> inventarios = inventarioAdminService.obtenerInventariosConStockBajo();
        return ResponseEntity.ok(inventarios);
    }

    /**
     * GET /api/inventario/sin-stock
     * Obtiene productos sin stock
     */
    @GetMapping("/sin-stock")
    public ResponseEntity<List<InventarioResponse>> obtenerInventariosSinStock() {
        List<InventarioResponse> inventarios = inventarioAdminService.obtenerInventariosSinStock();
        return ResponseEntity.ok(inventarios);
    }
}
