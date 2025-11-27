package pe.com.ikaza.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.ProductoRequest;
import pe.com.ikaza.backend.dto.request.ProductoUpdateRequest;
import pe.com.ikaza.backend.dto.response.ProductoDetalleResponse;
import pe.com.ikaza.backend.dto.response.ProductoResponse;
import pe.com.ikaza.backend.entity.*;
import pe.com.ikaza.backend.repository.CategoriaRepository;
import pe.com.ikaza.backend.repository.InventarioRepository;
import pe.com.ikaza.backend.repository.ProductoDetalleRepository;
import pe.com.ikaza.backend.repository.ProductoRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ProductoDetalleRepository productoDetalleRepository;

    @Transactional(readOnly = true)
    public Page<ProductoResponse> obtenerProductosPaginados(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(this::convertirAResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> obtenerProductosPorCategoria(Long idCategoria, Pageable pageable) {
        return productoRepository.findByCategoriaIdCategoria(idCategoria, pageable)
                .map(this::convertirAResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> buscarProductos(String texto, Pageable pageable) {
        return productoRepository.buscarPorTexto(texto, pageable)
                .map(this::convertirAResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return convertirAResponse(producto);
    }

    @Transactional(readOnly = true)
    public ProductoDetalleResponse obtenerDetalleProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        ProductoDetalleResponse response = new ProductoDetalleResponse();
        response.setIdProducto(producto.getIdProducto());
        response.setNombreProducto(producto.getNombreProducto());
        response.setDescripcionProducto(producto.getDescripcionProducto());
        response.setPrecio(producto.getPrecio());
        response.setStock(producto.getStock());
        response.setCalificacionPromedio(producto.getCalificacionPromedio());
        response.setNombreCategoria(producto.getCategoria().getNombreCategoria());
        response.setIdCategoria(producto.getCategoria().getIdCategoria());

        if (producto.getInventario() != null) {
            response.setStockDisponible(producto.getInventario().getStockDisponible());
            response.setStockReservado(producto.getInventario().getStockReservado());
        } else {
            response.setStockDisponible(producto.getStock());
            response.setStockReservado(0);
        }

        // Cargar detalles extendidos
        ProductoDetalle detalle = productoDetalleRepository.findByProductoIdProducto(id).orElse(null);
        if (detalle != null) {
            response.setCodigo(detalle.getCodigo());
            response.setMarca(detalle.getMarca());
            response.setModelo(detalle.getModelo());
            response.setGarantia(detalle.getGarantia());
            response.setAtributos(detalle.getAtributos());

            // Convertir imágenes
            if (detalle.getImagenes() != null && !detalle.getImagenes().isEmpty()) {
                response.setImagenes(detalle.getImagenes().stream()
                        .map(img -> new ProductoDetalleResponse.ImagenDto(
                                img.getUrl(), img.getAlt(), img.getEsPrincipal(), img.getOrden()))
                        .collect(Collectors.toList()));
            }

            // SEO
            if (detalle.getSeo() != null) {
                response.setSeo(new ProductoDetalleResponse.SeoDto(
                        detalle.getSeo().getSlug(),
                        detalle.getSeo().getMetaTitle(),
                        detalle.getSeo().getMetaDescription(),
                        detalle.getSeo().getKeywords()));
            }

            // Especificaciones
            if (detalle.getEspecificaciones() != null) {
                response.setEspecificaciones(detalle.getEspecificaciones().stream()
                        .map(esp -> new ProductoDetalleResponse.EspecificacionDto(
                                esp.getNombre(), esp.getValor(), esp.getUnidad()))
                        .collect(Collectors.toList()));
            }

            // Variantes
            if (detalle.getVariantes() != null) {
                response.setVariantes(detalle.getVariantes().stream()
                        .map(var -> new ProductoDetalleResponse.VarianteDto(
                                var.getSku(), var.getColor(), var.getTalla(),
                                var.getMaterial(), var.getStockAdicional(), var.getImagenUrl()))
                        .collect(Collectors.toList()));
            }
        }

        return response;
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombreProducto(request.getNombreProducto());
        producto.setDescripcionProducto(request.getDescripcionProducto());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setStockMinimo(request.getStockMinimo());

        Producto guardado = productoRepository.save(producto);

        // Crear inventario
        Inventario inventario = new Inventario();
        inventario.setProducto(guardado);
        inventario.setStockActual(request.getStock());
        inventario.setStockReservado(0);
        inventarioRepository.save(inventario);

        // Crear detalles si hay datos extendidos
        if (tieneDetallesExtendidos(request)) {
            crearProductoDetalle(guardado, request);
        }

        return convertirAResponse(guardado);
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long id, ProductoUpdateRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (request.getIdCategoria() != null) {
            Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            producto.setCategoria(categoria);
        }

        if (request.getNombreProducto() != null && !request.getNombreProducto().isEmpty()) {
            producto.setNombreProducto(request.getNombreProducto());
        }

        if (request.getDescripcionProducto() != null) {
            producto.setDescripcionProducto(request.getDescripcionProducto());
        }

        if (request.getPrecio() != null) {
            producto.setPrecio(request.getPrecio());
        }

        if (request.getStock() != null) {
            producto.setStock(request.getStock());
            Inventario inventario = inventarioRepository.findByProductoIdProducto(id)
                    .orElse(null);

            if (inventario != null) {
                inventario.setStockActual(request.getStock());
                inventarioRepository.save(inventario);
            }
        }

        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }

        Producto actualizado = productoRepository.save(producto);
        return convertirAResponse(actualizado);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        productoRepository.delete(producto);
    }

    @Transactional(readOnly = true)
    public ProductoDetalleResponse obtenerProductoMasVendido() {
        Producto producto = productoRepository.findProductoMasVendido().orElse(null);
        if (producto == null)
            return null;
        return obtenerDetalleProducto(producto.getIdProducto());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerProductosMasBaratos(int limite) {
        Pageable pageable = PageRequest.of(0, limite);
        return productoRepository.findProductosMasBaratos(pageable)
                .getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerProductosMasRecientes(int limite) {
        Pageable pageable = PageRequest.of(0, limite);
        return productoRepository.findProductosMasRecientes(pageable)
                .getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerProductosPorAgotarse(int limite) {
        Pageable pageable = PageRequest.of(0, limite);
        return productoRepository.findProductosPorAgotarse(pageable)
                .getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    // MÉTODOS AUXILIARES

    private boolean tieneDetallesExtendidos(ProductoRequest request) {
        return request.getCodigo() != null ||
                request.getMarca() != null ||
                request.getModelo() != null ||
                request.getGarantia() != null ||
                (request.getImagenesUrls() != null && !request.getImagenesUrls().isEmpty()) ||
                (request.getAtributos() != null && !request.getAtributos().isEmpty());
    }

    @Transactional
    protected void crearProductoDetalle(Producto producto, ProductoRequest request) {
        ProductoDetalle detalle = new ProductoDetalle();
        detalle.setProducto(producto);
        detalle.setCodigo(request.getCodigo());
        detalle.setMarca(request.getMarca());
        detalle.setModelo(request.getModelo());
        detalle.setGarantia(request.getGarantia());
        detalle.setAtributos(request.getAtributos() != null ? request.getAtributos() : new HashMap<>());

        // Agregar imágenes
        if (request.getImagenesUrls() != null && !request.getImagenesUrls().isEmpty()) {
            List<ProductoDetalle.ImagenDto> imagenes = new ArrayList<>();
            for (int i = 0; i < request.getImagenesUrls().size(); i++) {
                imagenes.add(new ProductoDetalle.ImagenDto(
                        request.getImagenesUrls().get(i),
                        request.getNombreProducto(),
                        i == 0,
                        i));
            }
            detalle.setImagenes(imagenes);
        }

        productoDetalleRepository.save(detalle);
    }

    private ProductoResponse convertirAResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setIdProducto(producto.getIdProducto());
        response.setNombreProducto(producto.getNombreProducto());
        response.setDescripcionProducto(producto.getDescripcionProducto());
        response.setPrecio(producto.getPrecio());
        response.setStock(producto.getStock());
        response.setStockMinimo(producto.getStockMinimo());
        response.setCalificacionPromedio(producto.getCalificacionPromedio());
        response.setNombreCategoria(producto.getCategoria().getNombreCategoria());
        response.setIdCategoria(producto.getCategoria().getIdCategoria());
        response.setFechaCreacion(producto.getFechaCreacion());
        response.setFechaActualizacion(producto.getFechaActualizacion());
        response.setDisponible(producto.getStock() > 0);

        // Cargar imagen principal y marca
        try {
            productoDetalleRepository.findByProductoIdProducto(producto.getIdProducto())
                    .ifPresent(detalle -> {
                        ProductoDetalle.ImagenDto imgPrincipal = detalle.getImagenPrincipal();
                        if (imgPrincipal != null) {
                            response.setImagenPrincipal(imgPrincipal.getUrl());
                        }
                        response.setMarca(detalle.getMarca());
                        response.setModelo(detalle.getModelo());
                    });
        } catch (Exception e) {
            System.err.println("Error cargando detalles: " + e.getMessage());
        }

        return response;
    }
}