package com.example.onlineexam;

import com.example.onlineexam.model.Role;
import com.example.onlineexam.model.User;
import com.example.onlineexam.repository.RoleRepository;
import com.example.onlineexam.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OnlineExamApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineExamApplication.class, args);
	}

	@Bean
	CommandLineRunner initial(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (roleRepository.findByName("ROLE_STUDENT").isEmpty()) {
				roleRepository.save(new Role("ROLE_STUDENT"));
			}
			if (roleRepository.findByName("ROLE_TEACHER").isEmpty()) {
				roleRepository.save(new Role("ROLE_TEACHER"));
			}
			if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
				roleRepository.save(new Role("ROLE_ADMIN"));
			}

			Role adminRole = roleRepository.findByName("ROLE_ADMIN")
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			Role studentRole = roleRepository.findByName("ROLE_STUDENT")
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));

			ensureUserWithRole(userRepository, passwordEncoder, "admin", "123456", adminRole);
			ensureUserWithRole(userRepository, passwordEncoder, "teacher", "123456", teacherRole);
			ensureUserWithRole(userRepository, passwordEncoder, "student", "123456", studentRole);
		};
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	private void ensureUserWithRole(UserRepository userRepository, PasswordEncoder passwordEncoder, String username, String rawPassword, Role role) {
		User user = userRepository.findByUsername(username).orElseGet(User::new);
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setRoles(Set.of(role));
		userRepository.save(user);
	}
}
