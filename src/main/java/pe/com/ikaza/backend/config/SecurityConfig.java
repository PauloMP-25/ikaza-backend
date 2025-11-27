package pe.com.ikaza.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import pe.com.ikaza.backend.security.JwtAuthenticationFilter;
import pe.com.ikaza.backend.security.UserDetailsServiceImpl;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuración de seguridad con JWT nativo (sin Firebase)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private UserDetailsServiceImpl userDetailsService;

        @Value("${cors.allowed-origins:http://localhost:4200}")
        private String allowedOrigins;

        @Bean
        public GrantedAuthorityDefaults grantedAuthorityDefaults() {
                return new GrantedAuthorityDefaults("");
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
                        throws Exception {
                return authConfig.getAuthenticationManager();
        }

        // Configuración principal de seguridad
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Rutas públicas
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/public/**").permitAll()
                                                .requestMatchers("/api/contacto/**").permitAll()
                                                .requestMatchers("/api/verification/**").authenticated()

                                                // Rutas públicas de Producto (lectura/consulta)
                                                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**")
                                                .permitAll()

                                                // Rutas de Producto (administrador)
                                                .requestMatchers(HttpMethod.POST, "/api/productos")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/productos/**")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/productos/**")
                                                .hasRole("ADMINISTRADOR")

                                                // Rutas públicas de Categoria (lectura/consulta)
                                                .requestMatchers(HttpMethod.GET, "/api/categorias",
                                                                "/api/categorias/**")
                                                .permitAll()

                                                // Rutas de Categoria (administrador)
                                                .requestMatchers(HttpMethod.POST, "/api/categorias")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/categorias/**")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**")
                                                .hasRole("ADMINISTRADOR")

                                                // Rutas de Usuario/Cliente (lectura/consulta)
                                                .requestMatchers("/api/usuarios/**").hasAnyAuthority("CLIENTE")
                                                .requestMatchers("/api/clientes/**")
                                                .hasAnyAuthority("CLIENTE", "ADMINISTRADOR")

                                                // Eutas de Correo Cliente/Administrador
                                                .requestMatchers(HttpMethod.POST, "/api/buzon/enviar")
                                                .hasAnyAuthority("CLIENTE", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/buzon/mis-mensajes")
                                                .hasAnyAuthority("CLIENTE", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/buzon/*/marcar-leido")
                                                .hasAnyAuthority("CLIENTE", "ADMINISTRADOR")

                                                // Eutas de Correo Administrador
                                                .requestMatchers(HttpMethod.GET, "/api/buzon/admin/**")
                                                .hasAuthority("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/buzon/admin/**")
                                                .hasAuthority("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/buzon/archivo/**")
                                                .hasAuthority("ADMINISTRADOR")

                                                // Rutas de Inventario (administrador)
                                                .requestMatchers("/api/inventario/**").hasAuthority("ADMINISTRADOR")

                                                // Cualquier otra ruta requiere autenticación
                                                .anyRequest().authenticated())
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * Configuración CORS
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                List<String> originsList = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
                configuration.setAllowedOrigins(originsList);

                configuration.setAllowedMethods(
                                Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));

                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                configuration.setExposedHeaders(
                                Arrays.asList("Authorization", "Content-Type"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}