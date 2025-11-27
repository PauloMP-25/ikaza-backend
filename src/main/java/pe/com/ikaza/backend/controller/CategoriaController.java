package pe.com.ikaza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.ikaza.backend.dto.request.CategoriaRequest;
import pe.com.ikaza.backend.dto.response.CategoriaResponse;
import pe.com.ikaza.backend.dto.response.MessageResponse;
import pe.com.ikaza.backend.service.CategoriaService;

import java.util.List;

/**
 * Controlador REST para gestión de categorías
 */
@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    /**
     * GET /api/categorias
     * Obtiene todas las categorías activas (público)
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> obtenerCategoriasActivas() {
        List<CategoriaResponse> categorias = categoriaService.obtenerCategoriasActivas();
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/categorias/buscar?texto=nombre
     * Busca categorías por nombre (público)
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CategoriaResponse>> buscarCategorias(
            @RequestParam String texto) {
        // @RequestParam: captura parámetros de la URL (?texto=...)
        List<CategoriaResponse> categorias = categoriaService.buscarCategorias(texto);
        return ResponseEntity.ok(categorias);
    }

    /**
     * GET /api/categorias/{id}
     * Obtiene una categoría por ID (público)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCategoriaPorId(@PathVariable Long id) {
        // @PathVariable: captura el {id} de la URL
        try {
            CategoriaResponse categoria = categoriaService.obtenerCategoriaPorId(id);
            return ResponseEntity.ok(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    // ========== ENDPOINTS PARA ADMINISTRADORES ==========

    /**
     * POST /api/categorias
     * Crea una nueva categoría
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        try {
            CategoriaResponse categoria = categoriaService.crearCategoria(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * PUT /api/categorias/{id}
     * Actualiza una categoría existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        try {
            CategoriaResponse categoria = categoriaService.actualizarCategoria(id, request);
            return ResponseEntity.ok(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * DELETE /api/categorias/{id}
     * Desactiva una categoría
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaService.eliminarCategoria(id);
            return ResponseEntity.ok(
                    new MessageResponse("Categoría desactivada exitosamente", true));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * DELETE /api/categorias/{id}/definitivo
     * Elimina definitivamente una categoría
     */
    @DeleteMapping("/{id}/definitivo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> eliminarCategoriaDefinitivo(@PathVariable Long id) {
        try {
            categoriaService.eliminarCategoriaDefinitivo(id);
            return ResponseEntity.ok(
                    new MessageResponse("Categoría eliminada definitivamente", true));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    /**
     * GET /api/categorias/todas
     * Obtiene todas las categorías incluyendo inactivas (solo admin)
     */
    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    // @PreAuthorize: requiere que el usuario tenga el rol ADMINISTRADOR
    public ResponseEntity<List<CategoriaResponse>> obtenerTodasLasCategorias() {
        List<CategoriaResponse> categorias = categoriaService.obtenerTodasLasCategorias();
        return ResponseEntity.ok(categorias);
    }
}