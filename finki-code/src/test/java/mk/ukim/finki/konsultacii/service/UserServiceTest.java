//package mk.ukim.finki.konsultacii.service;
//
//import mk.ukim.finki.konsultacii.repository.UserRepository;
//import mk.ukim.finki.konsultacii.service.implementation.UserServiceImplementation;
//import org.junit.jupiter.api.BeforeEach;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@SpringBootTest
//public class UserServiceTest {
//
//    private UserService userService;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        userService = new UserServiceImplementation(userRepository, passwordEncoder);
//    }
//
////    @Test
////    public void testSaveUser() {
////        String username = "testuser";
////        String password = "testpassword";
////        String role = "STUDENT";
////
////        User user = new User(username, password, Role.ROLE_STUDENT);
////        when(userRepository.save(any(User.class))).thenReturn(user);
////
////        User savedUser = userService.save(username, password, role);
////
////        verify(userRepository).save(any(User.class));
////
////        Assertions.assertEquals(savedUser.getUsername(), username);
////        Assertions.assertEquals(savedUser.getPassword(), password);
////        Assertions.assertEquals(savedUser.getRole(), Role.ROLE_STUDENT);
////    }
//}
//
