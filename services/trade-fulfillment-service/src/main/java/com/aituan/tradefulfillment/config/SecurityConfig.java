package com.aituan.tradefulfillment.config;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
      ObjectMapper objectMapper) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
            .accessDeniedHandler(accessDeniedHandler(objectMapper)))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers("/api/open/**", "/api/common/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/app/discovery/**", "/api/app/location/**").permitAll()
            .requestMatchers("/api/app/**").hasRole("USER")
            .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(jwtAuthenticationFilter, InternalServiceAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, authException) -> writeJson(objectMapper, response, 401, ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message());
  }

  private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, accessDeniedException) -> writeJson(objectMapper, response, 403, ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message());
  }

  private void writeJson(ObjectMapper objectMapper, jakarta.servlet.http.HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
    response.setStatus(httpStatus);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(code, message)));
  }
}
