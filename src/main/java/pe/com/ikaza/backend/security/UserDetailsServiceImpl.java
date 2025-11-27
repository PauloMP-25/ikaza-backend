package pe.com.ikaza.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ikaza.backend.entity.Usuario;
import pe.com.ikaza.backend.repository.UsuarioRepository;

import java.util.List;

/**
 * Implementación de UserDetailsService para autenticación JWT
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

        @Autowired
        private UsuarioRepository usuarioRepository;

        /**
         * Cargar usuario por email para autenticación
         */
        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Usuario no encontrado con email: " + email));

                if (!usuario.getActivo()) {
                        throw new UsernameNotFoundException("Usuario inactivo: " + email);
                }

                if (usuario.estaBloqueado()) {
                        throw new UsernameNotFoundException("Usuario bloqueado temporalmente: " + email);
                }

                List<GrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority(usuario.getRol()));

                // Retornar UserDetails
                return org.springframework.security.core.userdetails.User.builder()
                                .username(usuario.getEmail())
                                .password(usuario.getPassword())
                                .authorities(authorities)
                                .accountExpired(false)
                                .accountLocked(usuario.estaBloqueado())
                                .credentialsExpired(false)
                                .disabled(!usuario.getActivo())
                                .build();
        }

        /**
         * Cargar usuario por ID
         */
        @Transactional(readOnly = true)
        public UserDetails loadUserById(Integer id) {
                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Usuario no encontrado con id: " + id));

                List<GrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority(usuario.getRol()));

                return org.springframework.security.core.userdetails.User.builder()
                                .username(usuario.getEmail())
                                .password(usuario.getPassword())
                                .authorities(authorities)
                                .disabled(!usuario.getActivo())
                                .accountLocked(usuario.estaBloqueado())
                                .build();
        }

        /**
         * Obtener ID de usuario por email
         */
        @Transactional(readOnly = true)
        public Integer getUserIdByEmail(String email) {
                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "Usuario no encontrado con email: " + email));
                return usuario.getIdUsuario();
        }
}