package com.personalblog.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
        http
            .cors(Customizer.withDefaults())
            .csrf(config -> config.csrfTokenRepository(csrf)
                .ignoringRequestMatchers("/api/v1/admin/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verification/resend",
                    "/api/v1/auth/password/change").authenticated()
                .requestMatchers("/api/v1/newsletter/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/comments/*").authenticated()
                .requestMatchers("/api/v1/dashboard/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("BLOG_SESSION", "XSRF-TOKEN"))
            .httpBasic(config -> config.disable())
            .formLogin(config -> config.disable());
        return http.build();
    }
}
