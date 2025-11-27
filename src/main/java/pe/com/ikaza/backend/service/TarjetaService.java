package pe.com.ikaza.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.dto.request.TarjetaRequest;
import pe.com.ikaza.backend.dto.response.TarjetaResponse;
import pe.com.ikaza.backend.entity.Tarjeta;
import pe.com.ikaza.backend.exception.ResourceNotFoundException;
import pe.com.ikaza.backend.repository.TarjetaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TarjetaService {

    @Autowired
    private TarjetaRepository metodoPagoRepository;

    /**
     * Conversión Entity a Response DTO.
     */
    private TarjetaResponse toResponse(Tarjeta entity) {
        TarjetaResponse response = new TarjetaResponse();
        response.setIdMetodo(entity.getIdMetodo());
        response.setTipo(entity.getTipo());
        response.setAlias(entity.getAlias());
        response.setUltimos4Digitos(entity.getUltimos4Digitos());
        response.setNombreTitular(entity.getNombreTitular());
        response.setBancoEmisor(entity.getBancoEmisor());
        response.setTipoTarjeta(entity.getTipoTarjeta());
        response.setFechaExpiracion(entity.getFechaExpiracion());
        response.setEsPrincipal(entity.getEsPrincipal());
        response.setActivo(entity.getActivo());
        response.setFechaCreacion(entity.getFechaCreacion());
        return response;
    }

    /**
     * Carga todas las tarjetas activas de un usuario.
     * @param idUsuario ID del usuario autenticado.
     * @return Lista de TarjetaResponse.
     */
    public List<TarjetaResponse> cargarTarjetas(Integer idUsuario) {
        return metodoPagoRepository.findByIdUsuarioAndActivoTrue(idUsuario).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Guarda una nueva tarjeta.
     * @param idUsuario ID del usuario autenticado.
     * @param request Datos de la tarjeta.
     * @return TarjetaResponse de la tarjeta guardada.
     */
    public TarjetaResponse guardarTarjeta(Integer idUsuario, TarjetaRequest request) {
        Tarjeta metodoPago = new Tarjeta();
        metodoPago.setIdUsuario(idUsuario);
        metodoPago.setTipo(request.getTipo());
        metodoPago.setTokenPago(request.getTokenPago());
        metodoPago.setUltimos4Digitos(request.getUltimos4Digitos());
        metodoPago.setAlias(request.getAlias());
        metodoPago.setNombreTitular(request.getNombreTitular());
        metodoPago.setBancoEmisor(request.getBancoEmisor());
        metodoPago.setTipoTarjeta(request.getTipoTarjeta());
        metodoPago.setFechaExpiracion(request.getFechaExpiracion());
        metodoPago.setEsPrincipal(request.getEsPrincipal() != null ? request.getEsPrincipal() : false);
        metodoPago.setActivo(true);
        
        Tarjeta guardado = metodoPagoRepository.save(metodoPago);
        return toResponse(guardado);
    }
    
    /**
     * Actualiza una tarjeta existente marcándola como principal.
     */
    @Transactional
    public TarjetaResponse actualizarTarjetaPrincipal(Integer idMetodo, Integer idUsuario, TarjetaRequest request) {
        Tarjeta existente = metodoPagoRepository.findByIdMetodoAndIdUsuario(idMetodo, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada para este usuario."));

        if (request.getEsPrincipal() != null && request.getEsPrincipal()) {
            metodoPagoRepository.desmarcarTodasPrincipales(idUsuario);
            existente.setEsPrincipal(true);
        } else {
            existente.setEsPrincipal(request.getEsPrincipal() != null ? request.getEsPrincipal() : existente.getEsPrincipal());
        }

        // Validación de integridad
        int principalesCount = metodoPagoRepository.countTarjetasPrincipales(idUsuario);
        if (principalesCount > 1) {
            throw new IllegalStateException("Error de integridad: se detectaron múltiples direcciones principales");
        }

        Tarjeta actualizada = metodoPagoRepository.save(existente);
        return toResponse(actualizada);
    }

    /**
     * Elimina una tarjeta
     * @param idMetodo ID de la tarjeta a eliminar.
     * @param idUsuario ID del usuario autenticado.
     */
    public void eliminarTarjeta(Integer idMetodo, Integer idUsuario) {
        Tarjeta existente = metodoPagoRepository.findByIdMetodoAndIdUsuario(idMetodo, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada para este usuario."));
        metodoPagoRepository.save(existente);
        metodoPagoRepository.delete(existente);
    }
}
