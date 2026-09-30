package vn.iotstar.configs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("sangvu@hcmute.edu.vn").isEmpty()) {
            User user = new User();
            user.setFullName("Sang Vũ");
            user.setEmail("sangvu@hcmute.edu.vn");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setImages("u1.jpg");
            userRepository.save(user);
        }
    }
}
