package com.mapletct.ftmes.bpiadapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final BpiAdapterProperties properties;

    public SecurityConfiguration(BpiAdapterProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2ResourceServer().jwt();
    }

    @Bean
    public JwtDecoder legacyJwtDecoder() {
        NimbusJwtDecoderJwkSupport decoder = new NimbusJwtDecoderJwkSupport(properties.getKeycloakJwkSetUri());
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.getKeycloakAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                new JwtTimestampValidator(), new JwtIssuerValidator(properties.getKeycloakIssuer()), audience));
        return decoder;
    }
}
