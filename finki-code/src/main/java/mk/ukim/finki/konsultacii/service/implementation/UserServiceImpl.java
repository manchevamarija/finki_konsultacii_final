package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.User;
import mk.ukim.finki.konsultacii.model.exceptions.InvalidUsernameException;
import mk.ukim.finki.konsultacii.repository.UserRepository;
import mk.ukim.finki.konsultacii.service.UserService;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(InvalidUsernameException::new);
    }
}
