package mk.ukim.finki.konsultacii.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Profile(("!cas"))
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration extends AuthConfig {

    @Bean
    @Order(1100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        this.authorize(http);
        http.httpBasic((basic) -> basic.realmName("wp.finki.ukim.mk"));
        return http.build();
    }

}
