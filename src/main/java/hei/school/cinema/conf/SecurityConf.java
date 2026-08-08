package hei.school.cinema.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import hei.school.cinema.endpoint.rest.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConf {

  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/movies/**", "/rooms/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/movies", "/rooms")
                    .hasRole("MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/movies/**")
                    .hasRole("MANAGER")
                    .requestMatchers("/ping", "/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, e) ->
                            writeError(
                                request.getRequestURI(),
                                response,
                                "UNAUTHORIZED",
                                "Authentication required",
                                401))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            writeError(
                                request.getRequestURI(),
                                response,
                                "FORBIDDEN",
                                "Access denied",
                                403)));
    return http.build();
  }

  private void writeError(
          String path, HttpServletResponse response, String type, String message, int status) {
    try {
      response.setStatus(status);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      ApiError error =
              new ApiError(type, message, status, OffsetDateTime.now(), path);

      objectMapper.writeValue(response.getWriter(), error);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to write security error response", exception);
    }
  }
}