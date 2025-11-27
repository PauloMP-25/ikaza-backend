package pe.com.ikaza.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtro JWT
 * Intercepta requests y valida tokens JWT propios
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // Rutas públicas excluidas del filtro
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/api/auth/registro",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/verificar-email",
            "/api/public",
            "/api/google-maps/",
            "/api/categorias/"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Saltar rutas públicas
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extraer JWT del header Authorization
            String jwt = parseJwt(request);

            // Si no hay token, dejar que Spring Security maneje
            if (jwt == null) {
                logger.debug("No hay token Bearer en la petición - URI: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtUtils.validateJwtToken(jwt)) {
                String email = jwtUtils.getUserEmailFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Establecer en contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Usuario autenticado: {} - URI: {}", email, requestURI);
            } else {
                logger.warn("Token JWT inválido - URI: {}", requestURI);
            }

        } catch (Exception e) {
            logger.error("Error al procesar autenticación JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extraer token JWT del header Authorization
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    /**
     * Determinar si el filtro debe saltarse para esta petición
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_URLS.stream().anyMatch(path::startsWith);
    }
}