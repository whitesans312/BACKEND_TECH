package com.parkflow.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean SecurityFilterChain identityFilterChain(HttpSecurity http,AuthenticationEntryPoint entryPoint,AccessDeniedHandler deniedHandler,IdentityJwtAuthenticationConverter jwtConverter,@Qualifier("identityCorsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).cors(c->c.configurationSource(cors))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/register","/api/auth/login","/.well-known/jwks.json").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/users/**","/api/roles/**").hasRole("ADMINISTRADOR")
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
    @Bean CorsConfigurationSource identityCorsConfigurationSource(@Value("${PARKFLOW_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}") String originsCsv){
        List<String> origins=Arrays.stream(originsCsv.split(",")).map(String::trim).filter(origin->!origin.isEmpty()).toList();
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOriginPatterns(origins); c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key")); c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
    }
    @Bean AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper){return (request,response,exception)->writeProblem(mapper,response,HttpStatus.UNAUTHORIZED,"Autenticación requerida","Se requiere un token válido.");}
    @Bean AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper){return (request,response,exception)->writeProblem(mapper,response,HttpStatus.FORBIDDEN,"Acceso denegado","El usuario no tiene el rol requerido.");}
    private static void writeProblem(ObjectMapper mapper,HttpServletResponse response,HttpStatus status,String title,String detail)throws java.io.IOException{
        ProblemDetail p=ProblemDetail.forStatusAndDetail(status,detail);p.setTitle(title);response.setStatus(status.value());response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);mapper.writeValue(response.getOutputStream(),p);
    }
}
