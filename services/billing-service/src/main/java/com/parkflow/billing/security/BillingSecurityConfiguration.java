package com.parkflow.billing.security;

import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration @EnableMethodSecurity
public class BillingSecurityConfiguration {
    @Bean SecurityFilterChain billingSecurityFilterChain(HttpSecurity http,Converter<Jwt,AbstractAuthenticationToken> rolesConverter,@Qualifier("billingCorsConfigurationSource") CorsConfigurationSource cors)throws Exception{
        http.csrf(AbstractHttpConfigurer::disable).cors(c->c.configurationSource(cors)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.GET,"/api/billing/health").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/billing/tarifas/**","/api/billing/cajas/**","/api/billing/liquidaciones/**","/api/billing/pagos/**","/api/billing/comprobantes/**").hasAnyRole("ADMINISTRADOR","OPERADOR")
                .requestMatchers(HttpMethod.PUT,"/api/billing/tarifas/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST,"/api/billing/tarifas/**","/api/billing/cajas").hasRole("ADMINISTRADOR")
                .requestMatchers("/internal/v1/**").hasAnyRole("ADMINISTRADOR","OPERADOR")
                .requestMatchers("/api/billing/**").hasAnyRole("ADMINISTRADOR","OPERADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(rolesConverter)));
        return http.build();
    }
    @Bean CorsConfigurationSource billingCorsConfigurationSource(@Value("${PARKFLOW_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}") String originsCsv){
        List<String> origins=Arrays.stream(originsCsv.split(",")).map(String::trim).filter(origin->!origin.isEmpty()).toList();
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOriginPatterns(origins); c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key")); c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
    }
    @Bean Converter<Jwt,AbstractAuthenticationToken> billingRolesConverter(){
        return jwt->{List<String> roles=jwt.getClaimAsStringList("roles");
            var authorities=roles==null?List.<SimpleGrantedAuthority>of():roles.stream().map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList();
            return new JwtAuthenticationToken(jwt,authorities,jwt.getSubject());};
    }
    @Bean JwtDecoder billingJwtDecoder(@Value("${parkflow.security.jwt.jwk-set-uri}")String jwkUri,
                                       @Value("${parkflow.security.jwt.issuer}")String issuer,
                                       @Value("${parkflow.security.jwt.audience}")String audience){
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(jwkUri).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> issuerValidator=JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator=jwt->jwt.getAudience().contains(audience)
                ?org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                :org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token","JWT audience mismatch.",null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator,audienceValidator));
        return decoder;
    }
}
