package com.example.onlineexam;

import com.example.onlineexam.model.Role;
import com.example.onlineexam.model.User;
import com.example.onlineexam.repository.RoleRepository;
import com.example.onlineexam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

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

			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("123456"));
				Role adminRole = roleRepository.findByName("ROLE_ADMIN")
						.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
				admin.setRoles(Set.of(adminRole));
				userRepository.save(admin);
			}
		};
	}
}
