package mk.ukim.finki.konsultacii.config;

import mk.ukim.finki.konsultacii.model.enumerations.AppRole;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;


public class AuthConfig {

    public HttpSecurity authorize(HttpSecurity http) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers("/Consultations/GetTermsByTeacherCode").permitAll()
                        .requestMatchers("/display/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/consultations").hasAnyAuthority(
                                AppRole.STUDENT.name(), AppRole.ADMIN.name(), AppRole.PROFESSOR.name())
                        .requestMatchers("/auth/*", "/", "", "/display/*", "/css/*", "/js/*", "/images/*").permitAll()
                        .requestMatchers("/calendar/*/callback").permitAll()
                        .requestMatchers("/events").authenticated()
                        .requestMatchers("/student-attendances/**").hasRole(AppRole.STUDENT.name())
                        .requestMatchers("/professor-attendances/**").hasAnyRole(AppRole.PROFESSOR.name(), AppRole.ADMIN.name())
                        .requestMatchers("/admin-attendances/**").hasRole(AppRole.ADMIN.name())
                        .requestMatchers("/reports/**").hasRole(AppRole.ADMIN.name())
                        .requestMatchers("/manage-consultations/import").hasRole(AppRole.ADMIN.name())
                        .requestMatchers("/manage-consultations/sample-tsv").hasRole(AppRole.ADMIN.name())
                        .requestMatchers("/manage-consultations/{professorId}/**").access(
                                new WebExpressionAuthorizationManager("#professorId == authentication.name or hasRole('ROLE_ADMIN')")
                        )
                        .requestMatchers("/consultations/schedule").hasRole(AppRole.STUDENT.name())
                        .requestMatchers("/consultations/cancel").hasRole(AppRole.STUDENT.name())
                        .requestMatchers("/consultations/student/**").hasRole(AppRole.STUDENT.name())
                        .anyRequest().authenticated()
                )
                .logout(LogoutConfigurer::permitAll);
    }
}