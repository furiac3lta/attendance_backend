package com.marcedev.attendance.security.config;

import com.marcedev.attendance.security.CustomAuthenticationEntryPoint;
import com.marcedev.attendance.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, CustomAuthenticationEntryPoint customEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customEntryPoint = customEntryPoint;
    }

    // 🔥 FILTER CHAIN COMPLETO
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customEntryPoint))
                .authorizeHttpRequests(auth -> auth

                        // Public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Organizaciones
                        .requestMatchers("/api/organizations/**").hasRole("SUPER_ADMIN")

                        // Usuarios
                        .requestMatchers(HttpMethod.GET, "/api/users/visible").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // Cursos
                        .requestMatchers(HttpMethod.POST, "/api/users/*/assign-courses").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/courses/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR")

                        // Clases
                        .requestMatchers("/api/classes/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "INSTRUCTOR")

                        // Asistencia
                        .requestMatchers(HttpMethod.POST, "/api/attendance/**").hasAnyRole("SUPER_ADMIN","ADMIN","INSTRUCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/attendance/**").hasAnyRole("SUPER_ADMIN","ADMIN","INSTRUCTOR")

                        .anyRequest().authenticated()
                )

                // JWT FILTER
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .securityContext(context -> context.requireExplicitSave(false));

        http.setSharedObject(HttpFirewall.class, relaxedHttpFirewall());
        return http.build();
    }

    // 🔧 FIREWALL
    @Bean
    public HttpFirewall relaxedHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setUnsafeAllowAnyHttpMethod(true);
        firewall.setAllowedParameterNames(param -> true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedDoubleSlash(true);
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedPeriod(true);
        return firewall;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 🔥 CORS SOLO PARA LOCALHOST Y NETLIFY
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://gleaming-dodol-e386b2.netlify.app"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
