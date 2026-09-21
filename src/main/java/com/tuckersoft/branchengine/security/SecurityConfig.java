package com.tuckersoft.branchengine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO (estrella Seguridad): esta clase es solo un punto de partida para que
 * Partidas/Decisiones se puedan probar mientras se construye la seguridad real.
 * Falta agregar:
 * - Un OncePerRequestFilter que lea "Authorization: Bearer <token>", valide el
 *   JWT (jjwt) y ponga la Authentication en el SecurityContextHolder. El
 *   Authentication.getName() debe ser el EMAIL del usuario (CurrentUserService
 *   ya asume ese contrato).
 * - Un UserDetailsService que cargue al usuario por email (las authorities SE
 *   LEEN DE LA BASE DE DATOS en cada peticion, nunca del token).
 * - Reglas por endpoint: /auth/** publico, POST /nodes y /users/** solo
 *   hasRole("ADMIN"), el resto autenticado.
 * - Un AuthenticationEntryPoint (401) y un AccessDeniedHandler (403) que
 *   escriban com.tuckersoft.branchengine.common.ApiErrorResponse en el body
 *   (Spring Security los devuelve vacios por defecto).
 * - Un DataInitializer que cree el admin desde ADMIN_NAME/ADMIN_EMAIL/ADMIN_PASSWORD.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO: reemplazar por reglas reales por rol una vez este el filtro JWT.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
