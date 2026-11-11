package com.autorent.config;

import com.autorent.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, BloqueioLogin bloqueio) throws Exception {
        OncePerRequestFilter filtroBloqueio = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String email = emailDoCabecalho(req.getHeader("Authorization"));
                long restante = email == null ? 0 : bloqueio.segundosRestantes(req.getRemoteAddr(), email);
                if (restante > 0) {
                    long minutos = (restante + 59) / 60;
                    res.setStatus(429);
                    res.setHeader("Retry-After", String.valueOf(restante));
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":429,\"mensagem\":\"Muitas tentativas de login. Tente novamente em "
                            + minutos + (minutos == 1 ? " minuto." : " minutos.") + "\"}");
                    return;
                }
                chain.doFilter(req, res);
            }
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":401,\"mensagem\":\"E-mail ou senha inválidos.\"}");
                }))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/cadastro").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(filtroBloqueio, BasicAuthenticationFilter.class);
        return http.build();
    }

    static String emailDoCabecalho(String cabecalho) {
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String credenciais = new String(Base64.getDecoder().decode(cabecalho.substring(6).trim()), StandardCharsets.UTF_8);
            int separador = credenciais.indexOf(':');
            return separador > 0 ? credenciais.substring(0, separador) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
