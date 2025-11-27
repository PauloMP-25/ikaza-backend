package pe.com.ikaza.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.ikaza.backend.dto.request.BuzonRequest;
import pe.com.ikaza.backend.dto.response.BuzonResponse;
import pe.com.ikaza.backend.dto.response.ListaMensajesResponse;
import pe.com.ikaza.backend.dto.response.MensajeBuzonDTO;
import pe.com.ikaza.backend.dto.response.EstadisticasBuzonResponse;
import pe.com.ikaza.backend.entity.MensajeBuzon;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.exception.ResourceNotFoundException;
import pe.com.ikaza.backend.repository.MensajeBuzonRepository;
import pe.com.ikaza.backend.repository.UsuarioRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuzonService {

    private final MensajeBuzonRepository mensajeBuzonRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    private static final String UPLOAD_DIR = "uploads/buzon/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * Procesa y envía un mensaje al buzón virtual
     */
    @Transactional
    public BuzonResponse enviarMensaje(BuzonRequest request, Integer idUsuario) {
        try {
            log.info("📬 Procesando mensaje de buzón - Tipo: {}, Usuario: {}",
                    request.getTipo(), idUsuario);

            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            validarRequest(request);

            // Construir entidad
            MensajeBuzon mensaje = construirMensaje(request, usuario);

            if (request.getArchivoAdjunto() != null && !request.getArchivoAdjunto().isEmpty()) {
                String rutaArchivo = guardarArchivo(request.getArchivoAdjunto(), "adjunto");
                mensaje.setArchivoAdjunto(rutaArchivo);
            }

            if (request.getArchivoEvidencia() != null && !request.getArchivoEvidencia().isEmpty()) {
                String rutaArchivo = guardarArchivo(request.getArchivoEvidencia(), "evidencia");
                mensaje.setArchivoEvidencia(rutaArchivo);
            }
            MensajeBuzon mensajeGuardado = mensajeBuzonRepository.save(mensaje);

            enviarEmailNotificacion(mensajeGuardado, usuario);

            log.info("Mensaje guardado exitosamente - ID: {}", mensajeGuardado.getIdMensaje());

            return BuzonResponse.builder()
                    .success(true)
                    .mensaje(construirMensajeExito(request.getTipo()))
                    .idMensaje(mensajeGuardado.getIdMensaje())
                    .build();

        } catch (Exception e) {
            log.error("Error al procesar mensaje de buzón", e);
            return BuzonResponse.builder()
                    .success(false)
                    .mensaje("Hubo un error al enviar tu mensaje. Por favor, intenta nuevamente.")
                    .build();
        }
    }

    /**
     * Obtiene todos los mensajes de un usuario
     */
    @Transactional(readOnly = true)
    public ListaMensajesResponse obtenerMensajesUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<MensajeBuzon> mensajes = mensajeBuzonRepository
                .findByUsuarioOrderByFechaCreacionDesc(usuario);

        List<MensajeBuzonDTO> mensajesDTO = mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ListaMensajesResponse.builder()
                .success(true)
                .mensajes(mensajesDTO)
                .total(mensajesDTO.size())
                .build();
    }

    /**
     * Marca un mensaje como leído
     */
    @Transactional
    public void marcarComoLeido(Integer idMensaje, Integer idUsuario) {
        MensajeBuzon mensaje = mensajeBuzonRepository.findById(idMensaje)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        // Verificar que el mensaje pertenece al usuario
        if (!mensaje.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new SecurityException("No tienes permiso para marcar este mensaje");
        }

        if (!mensaje.getLeido()) {
            mensaje.marcarComoLeido();
            mensajeBuzonRepository.save(mensaje);
            log.info("Mensaje {} marcado como leído", idMensaje);
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void validarRequest(BuzonRequest request) {
        if (request.getTipo() == BuzonRequest.TipoMensaje.RECLAMO) {
            if (request.getCategoriaReclamo() == null) {
                throw new IllegalArgumentException("La categoría del reclamo es obligatoria");
            }
            if (request.getCategoriaReclamo() == BuzonRequest.CategoriaReclamo.OTRO
                    && (request.getReclamoOtro() == null || request.getReclamoOtro().isBlank())) {
                throw new IllegalArgumentException("Debe especificar el tipo de reclamo");
            }
        }
    }

    private MensajeBuzon construirMensaje(BuzonRequest request, Usuario usuario) {
        MensajeBuzon.MensajeBuzonBuilder builder = MensajeBuzon.builder()
                .usuario(usuario)
                .tipoMensaje(MensajeBuzon.TipoMensaje.valueOf(request.getTipo().name()))
                .asunto(request.getAsunto())
                .descripcion(request.getDescripcion())
                .estado(MensajeBuzon.EstadoMensaje.PENDIENTE)
                .leido(false);

        if (request.getTipo() == BuzonRequest.TipoMensaje.RECLAMO) {
            builder.categoriaReclamo(
                    MensajeBuzon.CategoriaReclamo.valueOf(request.getCategoriaReclamo().name()));
            builder.reclamoOtro(request.getReclamoOtro());
            builder.urgenciaReclamo(
                    request.getUrgenciaReclamo() != null
                            ? MensajeBuzon.UrgenciaReclamo.valueOf(request.getUrgenciaReclamo().name())
                            : MensajeBuzon.UrgenciaReclamo.NORMAL);
        }

        return builder.build();
    }

    private String guardarArchivo(MultipartFile file, String tipo) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido (5MB)");
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String nombreArchivo = tipo + "_" + UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(nombreArchivo);

        // Guardar archivo
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Archivo guardado: {}", nombreArchivo);
        return nombreArchivo;
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    private void enviarEmailNotificacion(MensajeBuzon mensaje, Usuario usuario) {
        try {
            emailService.enviarMensajeBuzon(mensaje, usuario);
        } catch (Exception e) {
            log.error("Error al enviar email de notificación, pero el mensaje fue guardado", e);
        }
    }

    private String construirMensajeExito(BuzonRequest.TipoMensaje tipo) {
        if (tipo == BuzonRequest.TipoMensaje.RECLAMO) {
            return "¡Reclamo enviado exitosamente! Nuestro equipo lo revisará y te responderá pronto.";
        } else {
            return "¡Recomendación enviada exitosamente! Gracias por ayudarnos a mejorar.";
        }
    }

    private MensajeBuzonDTO convertirADTO(MensajeBuzon mensaje) {
        return MensajeBuzonDTO.builder()
                .idMensaje(mensaje.getIdMensaje())
                .tipoMensaje(mensaje.getTipoMensaje().name())
                .asunto(mensaje.getAsunto())
                .descripcion(mensaje.getDescripcion())
                .categoriaReclamo(mensaje.getCategoriaReclamo() != null
                        ? mensaje.getCategoriaReclamo().name()
                        : null)
                .reclamoOtro(mensaje.getReclamoOtro())
                .urgenciaReclamo(mensaje.getUrgenciaReclamo() != null
                        ? mensaje.getUrgenciaReclamo().name()
                        : null)
                .estado(mensaje.getEstado().name())
                .leido(mensaje.getLeido())
                .respuesta(mensaje.getRespuesta())
                .fechaCreacion(mensaje.getFechaCreacion())
                .fechaRespuesta(mensaje.getFechaRespuesta())
                .archivoAdjunto(mensaje.getArchivoAdjunto())
                .archivoEvidencia(mensaje.getArchivoEvidencia())
                .build();
    }

    // ========== MÉTODOS PARA ADMINISTRADORES ==========

    /**
     * Obtiene todos los mensajes (admin)
     */
    @Transactional(readOnly = true)
    public ListaMensajesResponse obtenerTodosMensajes() {
        List<MensajeBuzon> mensajes = mensajeBuzonRepository.findAll();
        mensajes.sort((a, b) -> {
            boolean aUrgente = a.getTipoMensaje() == MensajeBuzon.TipoMensaje.RECLAMO
                    && a.getUrgenciaReclamo() == MensajeBuzon.UrgenciaReclamo.ALTA;
            boolean bUrgente = b.getTipoMensaje() == MensajeBuzon.TipoMensaje.RECLAMO
                    && b.getUrgenciaReclamo() == MensajeBuzon.UrgenciaReclamo.ALTA;

            if (aUrgente && !bUrgente)
                return -1;
            if (!aUrgente && bUrgente)
                return 1;

            if (!a.getLeido() && b.getLeido())
                return -1;
            if (a.getLeido() && !b.getLeido())
                return 1;

            return b.getFechaCreacion().compareTo(a.getFechaCreacion());
        });

        List<MensajeBuzonDTO> mensajesDTO = mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ListaMensajesResponse.builder()
                .success(true)
                .mensajes(mensajesDTO)
                .total(mensajesDTO.size())
                .build();
    }

    /**
     * Obtiene mensajes por tipo (admin)
     */
    @Transactional(readOnly = true)
    public ListaMensajesResponse obtenerMensajesPorTipo(String tipo) {
        MensajeBuzon.TipoMensaje tipoMensaje = MensajeBuzon.TipoMensaje.valueOf(tipo.toUpperCase());
        List<MensajeBuzon> mensajes = mensajeBuzonRepository.findByTipoMensajeOrderByFechaCreacionDesc(tipoMensaje);

        List<MensajeBuzonDTO> mensajesDTO = mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return ListaMensajesResponse.builder()
                .success(true)
                .mensajes(mensajesDTO)
                .total(mensajesDTO.size())
                .build();
    }

    /**
     * Obtiene detalle completo de un mensaje (admin)
     */
    @Transactional(readOnly = true)
    public MensajeBuzonDTO obtenerDetalleMensaje(Integer idMensaje) {
        MensajeBuzon mensaje = mensajeBuzonRepository.findById(idMensaje)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        return convertirADTO(mensaje);
    }

    /**
     * Actualiza el estado de un mensaje (admin)
     */
    @Transactional
    public void actualizarEstado(Integer idMensaje, String nuevoEstado) {
        MensajeBuzon mensaje = mensajeBuzonRepository.findById(idMensaje)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));

        MensajeBuzon.EstadoMensaje estado = MensajeBuzon.EstadoMensaje.valueOf(nuevoEstado.toUpperCase());
        mensaje.setEstado(estado);

        // Si cambia a EN_REVISION, marcar como leído automáticamente
        if (estado == MensajeBuzon.EstadoMensaje.EN_REVISION && !mensaje.getLeido()) {
            mensaje.marcarComoLeido();
        }

        mensajeBuzonRepository.save(mensaje);
        log.info("Estado del mensaje {} actualizado a {}", idMensaje, estado);
    }

    /**
     * Obtiene estadísticas del buzón (admin)
     */
    @Transactional(readOnly = true)
    public EstadisticasBuzonResponse obtenerEstadisticas() {
        Long totalMensajes = mensajeBuzonRepository.count();
        Long totalReclamos = mensajeBuzonRepository.countByTipo(MensajeBuzon.TipoMensaje.RECLAMO);
        Long totalRecomendaciones = mensajeBuzonRepository.countByTipo(MensajeBuzon.TipoMensaje.RECOMENDACION);
        Long reclamosPendientes = mensajeBuzonRepository.countByTipoAndEstado(
                MensajeBuzon.TipoMensaje.RECLAMO,
                MensajeBuzon.EstadoMensaje.PENDIENTE);

        List<MensajeBuzon> reclamosUrgentes = mensajeBuzonRepository.findReclamosUrgentes();
        Long mensajesNoLeidos = mensajeBuzonRepository.countByLeidoFalse();

        return EstadisticasBuzonResponse.builder()
                .success(true)
                .totalMensajes(totalMensajes.intValue())
                .totalReclamos(totalReclamos.intValue())
                .totalRecomendaciones(totalRecomendaciones.intValue())
                .reclamosPendientes(reclamosPendientes.intValue())
                .reclamosUrgentes(reclamosUrgentes.size())
                .mensajesNoLeidos(mensajesNoLeidos.intValue())
                .build();
    }

    /**
     * Obtiene un archivo para descarga
     */
    public Resource obtenerArchivo(String nombreArchivo) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR).resolve(nombreArchivo).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new IOException("Archivo no encontrado: " + nombreArchivo);
        }
    }
}