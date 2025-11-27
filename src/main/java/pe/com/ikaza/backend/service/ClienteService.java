package pe.com.ikaza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.ActualizarClienteRequest;
import pe.com.ikaza.backend.dto.response.ClienteResponse;
import pe.com.ikaza.backend.entity.Cliente;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.ClienteRepository;
import pe.com.ikaza.backend.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión del perfil Cliente (Datos personales) y Administración de Clientes.
 */
@Service
@Transactional
public class ClienteService {

    private static final Logger logger = LoggerFactory.getLogger(ClienteService.class);
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    // ===============================================
    // LÓGICA DE SINCRONIZACIÓN (Post-Registro/Login)
    // ===============================================

    /**
     * POST /api/clientes/crear-perfil-inicial
     * Crea el registro inicial en la tabla Cliente después del registro en AuthService.
     * @param email El email del usuario (obtenido del token JWT).
     * @return ClienteResponse con los datos iniciales.
     */
    public ClienteResponse crearPerfilInicial(String email) {
        logger.info("Creando perfil inicial Cliente para email: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
        if (clienteOpt.isPresent()) {
            logger.warn("Perfil Cliente ya existe para email: {}", email);
            return convertirAResponse(usuario, clienteOpt.get());
        }

        Cliente nuevoCliente = new Cliente();

        Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
        logger.info("Perfil Cliente inicial creado para Usuario ID: {}", usuario.getIdUsuario());

        return convertirAResponse(usuario, clienteGuardado);
    }

    // ===============================================
    // MÉTODOS PÚBLICOS (para perfil de usuario)
    // ===============================================

    /**
     * GET /api/clientes/perfil/{email}
     * Obtener perfil del usuario autenticado (combinando Usuario y Cliente).
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Datos de perfil Cliente no encontrados."));

        return convertirAResponse(usuario, cliente);
    }

    /**
     * PUT /api/clientes/perfil/{email}
     * Actualizar perfil del usuario (solo campos de Cliente).
     */
    public ClienteResponse actualizarCliente(String email, ActualizarClienteRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Datos de perfil Cliente no encontrados."));

        actualizarDatosCliente(cliente, request);

        Cliente actualizado = clienteRepository.save(cliente);
        logger.info("Perfil Cliente actualizado: {}", usuario.getEmail());

        return convertirAResponse(usuario, actualizado);
    }

    // ===============================================
    // MÉTODOS ADMINISTRATIVOS (Funcionalidad Limitada)
    // ===============================================

    /**
     * GET /api/clientes (Listar)
     * Listar todos los clientes/usuarios con paginación (Solo datos esenciales).
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarClientesPaginados(Pageable pageable) {
        Page<Usuario> usuarios = usuarioRepository.findAll(pageable);

        return usuarios.map(usuario -> {
            Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .orElse(new Cliente());
            return convertirAResponse(usuario, cliente);
        });
    }

    /**
     * PUT /api/clientes/{id}/activar (Admin)
     * Activar el usuario (campo 'activo' en la tabla Usuario).
     */
    public void activarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        logger.info("Usuario activado ID: {}", id);
    }

    /**
     * PUT /api/clientes/{id}/desactivar (Admin)
     * Desactivar el usuario (campo 'activo' en la tabla Usuario).
     */
    public void desactivarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        logger.info("Usuario desactivado ID: {}", id);
    }

    /**
     * GET /api/clientes/buscar (Admin)
     * Buscar clientes por email, documento o teléfono.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientes(String email, String documento, String telefono) {

        List<Usuario> usuarios = usuarioRepository.findAll(); 

        List<ClienteResponse> resultados = usuarios.stream()
                .map(usuario -> {
                    Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                            .orElse(new Cliente());
                    return new Object[] { usuario, cliente };
                })
                .filter(par -> {
                    Usuario u = (Usuario) par[0];
                    Cliente c = (Cliente) par[1];

                    boolean matchEmail = email != null && u.getEmail().toLowerCase().contains(email.toLowerCase());
                    boolean matchDocumento = documento != null && c.getNumeroDocumento() != null
                            && c.getNumeroDocumento().contains(documento);
                    boolean matchTelefono = telefono != null && c.getTelefono() != null
                            && c.getTelefono().contains(telefono);

                    return matchEmail || matchDocumento || matchTelefono;
                })
                .map(par -> convertirAResponse((Usuario) par[0], (Cliente) par[1]))
                .collect(Collectors.toList());

        return resultados;
    }

    /**
     * GET /api/clientes/estadisticas (Admin)
     * Obtener estadísticas administrativas detalladas (demográficas, actividad,
     * etc.).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        List<Usuario> todosUsuarios = usuarioRepository.findAll();
        long totalClientes = todosUsuarios.size();

        long activos = todosUsuarios.stream().filter(Usuario::getActivo).count();
        long inactivos = totalClientes - activos;

        Long datosIncompletos = clienteRepository.countClientesConDatosIncompletos();
        Long telefonoVerificado = clienteRepository.countClientesConTelefonoVerificado();

        Long varones = clienteRepository.countByGenero("HOMBRE");
        Long mujeres = clienteRepository.countByGenero("MUJER");

        LocalDateTime hoyInicio = LocalDateTime.now().toLocalDate().atStartOfDay();
        long registradosHoy = todosUsuarios.stream()
                .filter(u -> u.getFechaCreacion().isAfter(hoyInicio))
                .count();

        LocalDateTime mesInicio = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        long registradosMes = todosUsuarios.stream()
                .filter(u -> u.getFechaCreacion().isAfter(mesInicio))
                .count();

        Map<String, Object> stats = new HashMap<>();

        stats.put("usuariosTotales", totalClientes);
        stats.put("usuariosActivos", activos);
        stats.put("usuariosInactivos", inactivos);
        stats.put("usuariosRegistradosMes", registradosMes);
        stats.put("usuariosRegistradosHoy", registradosHoy);

        stats.put("clientesConDatosIncompletos", datosIncompletos);
        stats.put("clientesConTelefonoVerificado", telefonoVerificado);
        stats.put("clientesVarones", varones);
        stats.put("clientesMujeres", mujeres);

        logger.info("Estadísticas administrativas generadas exitosamente.");
        return stats;
    }

    // ===============================================
    // MÉTODOS AUXILIARES
    // ===============================================

    /**
     * Obtener el ID de usuario por email
     */
    @Transactional(readOnly = true)
    public Integer obtenerIdPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getIdUsuario();
    }

    /**
     * Actualizar datos del Cliente desde request (solo campos permitidos).
     */
    private void actualizarDatosCliente(Cliente cliente, ActualizarClienteRequest request) {
        if (request.getNombres() != null && !request.getNombres().isEmpty()) {
            cliente.setNombresCliente(request.getNombres());
        }

        if (request.getApellidos() != null && !request.getApellidos().isEmpty()) {
            cliente.setApellidosCliente(request.getApellidos());
        }

        if (request.getTipoDocumento() != null) {
            cliente.setTipoDocumento(request.getTipoDocumento());
        }

        if (request.getNumeroDocumento() != null) {
            if (!request.getNumeroDocumento().equals(cliente.getNumeroDocumento())) {
                if (clienteRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
                    throw new RuntimeException("El número de documento ya está registrado");
                }
            }
            cliente.setNumeroDocumento(request.getNumeroDocumento());
        }

        if (request.getFechaNacimiento() != null) {
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }

        if (request.getPrefijoTelefono() != null) {
            cliente.setPrefijoTelefono(request.getPrefijoTelefono());
        }

        if (request.getTelefono() != null) {
            cliente.setTelefono(request.getTelefono());
        }

        if (request.getGenero() != null) {
            cliente.setGenero(request.getGenero());
        }

        if (request.getTelefonoVerificado() != null) {
            cliente.setTelefonoVerificado(request.getTelefonoVerificado());
        }
    }

    /**
     * Convertir entidad Usuario + Cliente a DTO ClienteResponse.
     */
    private ClienteResponse convertirAResponse(Usuario usuario, Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setFechaActualizacion(cliente.getFechaActualizacion());

        // Datos de Cliente (Perfil)
        response.setNombres(cliente.getNombresCliente());
        response.setApellidos(cliente.getApellidosCliente());
        response.setNombreCompleto(cliente.getNombreCompleto());
        response.setTipoDocumento(cliente.getTipoDocumento());
        response.setNumeroDocumento(cliente.getNumeroDocumento());
        response.setFechaNacimiento(cliente.getFechaNacimiento());
        response.setEdad(calcularEdad(cliente.getFechaNacimiento()));
        response.setPrefijoTelefono(cliente.getPrefijoTelefono());
        response.setTelefono(cliente.getTelefono());
        response.setTelefonoVerificado(cliente.getTelefonoVerificado());
        response.setGenero(cliente.getGenero());
        // Datos completos
        response.setDatosCompletos(tieneDatosCompletos(cliente));

        return response;
    }

    /**
     * Calcular edad desde fecha de nacimiento
     */
    private Integer calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null)
            return null;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /**
     * Verifica si el Cliente tiene todos los datos de perfil requeridos
     * Incluye validaciones de nulos y campos vacíos.
     */
    private boolean tieneDatosCompletos(Cliente cliente) {
        if (cliente == null) {
            logger.warn("Verificación de datos completos fallida: cliente es null");
            return false;
        }

        boolean datosCompletos = cliente.getNombresCliente() != null && !cliente.getNombresCliente().isBlank() &&
                cliente.getApellidosCliente() != null && !cliente.getApellidosCliente().isBlank() &&
                cliente.getTipoDocumento() != null &&
                cliente.getNumeroDocumento() != null && !cliente.getNumeroDocumento().isBlank() &&
                cliente.getFechaNacimiento() != null &&
                cliente.getPrefijoTelefono() != null && !cliente.getPrefijoTelefono().isBlank() &&
                cliente.getTelefono() != null && !cliente.getTelefono().isBlank() && 
                cliente.getNumeroDocumento() != null && !cliente.getNumeroDocumento().isBlank() &&
                Boolean.TRUE.equals(cliente.getTelefonoVerificado());

        if (!datosCompletos) {
            logger.info("Perfil incompleto para cliente con ID: {}",
                    cliente.getUsuario() != null ? cliente.getUsuario().getIdUsuario() : "desconocido");
        }

        return datosCompletos;
    }
}
