package mk.ukim.finki.konsultacii.config;

import mk.ukim.finki.konsultacii.repository.StudentRepository;
import mk.ukim.finki.konsultacii.repository.UserRepository;
import mk.ukim.finki.konsultacii.service.ProfessorService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Profile("cas")
@Service
public class CasUserDetailsService
        extends FacultyUserDetailsService implements AuthenticationUserDetailsService {

    public CasUserDetailsService(UserRepository userRepository, ProfessorService professorService,
                                 PasswordEncoder passwordEncoder, StudentRepository studentRepository) {
        super(userRepository, professorService, studentRepository, passwordEncoder);
    }

    @Override
    public UserDetails loadUserDetails(Authentication token) throws UsernameNotFoundException {
        String username = (String) token.getPrincipal();
        return super.loadUserByUsername(username.toLowerCase());
    }
}
