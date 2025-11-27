package pe.com.ikaza.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.CategoriaRequest;
import pe.com.ikaza.backend.dto.response.CategoriaResponse;
import pe.com.ikaza.backend.entity.Categoria;
import pe.com.ikaza.backend.repository.CategoriaRepository;
import pe.com.ikaza.backend.repository.ProductoRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar categorías
 */
@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Obtiene todas las categorías activas
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerCategoriasActivas() {
        return categoriaRepository.findByActivoTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las categorías (incluyendo inactivas)
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerTodasLasCategorias() {
        return categoriaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una categoría por ID
     */
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerCategoriaPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
        return convertirAResponse(categoria);
    }

    /**
     * Crea una nueva categoría
     */
    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        // Validar que no exista una categoría con el mismo nombre
        if (categoriaRepository.existsByNombreCategoria(request.getNombreCategoria())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria();
        categoria.setNombreCategoria(request.getNombreCategoria());
        categoria.setDescripcionCategoria(request.getDescripcionCategoria());
        categoria.setActivo(request.getActivo() != null ? request.getActivo() : true);

        Categoria guardada = categoriaRepository.save(categoria);
        return convertirAResponse(guardada);
    }

    /**
     * Actualiza una categoría existente
     */
    @Transactional
    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Validar nombre único (si cambió)
        if (!categoria.getNombreCategoria().equals(request.getNombreCategoria())) {
            if (categoriaRepository.existsByNombreCategoria(request.getNombreCategoria())) {
                throw new RuntimeException("Ya existe una categoría con ese nombre");
            }
        }

        categoria.setNombreCategoria(request.getNombreCategoria());
        categoria.setDescripcionCategoria(request.getDescripcionCategoria());
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        Categoria actualizada = categoriaRepository.save(categoria);
        return convertirAResponse(actualizada);
    }

    /**
     * Elimina una categoría (marca como inactiva)
     */
    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Verificar si tiene productos asociados
        long cantidadProductos = productoRepository.contarProductosPorCategoria(id);
        if (cantidadProductos > 0) {
            throw new RuntimeException(
                    "No se puede eliminar la categoría porque tiene " + cantidadProductos + " productos asociados");
        }

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    /**
     * Elimina definitivamente una categoría
     */
    @Transactional
    public void eliminarCategoriaDefinitivo(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Verificar si tiene productos
        long cantidadProductos = productoRepository.contarProductosPorCategoria(id);
        if (cantidadProductos > 0) {
            throw new RuntimeException(
                    "No se puede eliminar la categoría porque tiene productos asociados");
        }

        categoriaRepository.delete(categoria);
    }

    /**
     * Busca categorías por nombre
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> buscarCategorias(String texto) {
        return categoriaRepository.findByNombreCategoriaContainingIgnoreCase(texto)
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad a DTO de respuesta
     */
    private CategoriaResponse convertirAResponse(Categoria categoria) {
        long cantidadProductos = productoRepository.contarProductosPorCategoria(categoria.getIdCategoria());

        return new CategoriaResponse(
                categoria.getIdCategoria(),
                categoria.getNombreCategoria(),
                categoria.getDescripcionCategoria(),
                categoria.getActivo(),
                categoria.getFechaCreacion(),
                (int) cantidadProductos);
    }
}