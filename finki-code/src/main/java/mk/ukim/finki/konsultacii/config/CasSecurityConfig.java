package mk.ukim.finki.konsultacii.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;


@Profile(("cas"))
@Configuration
@EnableWebSecurity
public class CasSecurityConfig extends AuthConfig {

    @Autowired
    CasAuthenticationProvider provider;

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, CasAuthenticationConfigurer casConfigurer,
            AuthenticationUserDetailsService userDetailsService) throws Exception {
        this.authorize(http)
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
            .exceptionHandling(handler -> handler.authenticationEntryPoint(casConfigurer.casAuthenticationEntryPoint()))
            .apply(casConfigurer);
        http.authenticationProvider(provider);
        return http.build();
    }

}

