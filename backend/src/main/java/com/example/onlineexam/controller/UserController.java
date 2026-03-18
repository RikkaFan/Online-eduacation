package com.example.onlineexam.controller;

import com.example.onlineexam.model.Role;
import com.example.onlineexam.model.User;
import com.example.onlineexam.payload.response.MessageResponse;
import com.example.onlineexam.payload.response.UserDTO;
import com.example.onlineexam.repository.ExamResultRepository;
import com.example.onlineexam.repository.RoleRepository;
import com.example.onlineexam.repository.StudentAnswerRepository;
import com.example.onlineexam.repository.UserRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is required."));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Password is required."));
        }
        if (Boolean.TRUE.equals(userRepository.existsByUsername(request.getUsername()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken."));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken."));
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail() == null ? null : request.getEmail().trim());
        user.setRoles(Set.of(resolveRole(request.getRole())));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        if (request.getUsername() != null && !request.getUsername().isBlank() && !request.getUsername().equals(user.getUsername())) {
            if (Boolean.TRUE.equals(userRepository.existsByUsername(request.getUsername()))) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken."));
            }
            user.setUsername(request.getUsername().trim());
        }

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.isEmpty() && !newEmail.equals(user.getEmail()) && Boolean.TRUE.equals(userRepository.existsByEmail(newEmail))) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken."));
            }
            user.setEmail(newEmail.isEmpty() ? null : newEmail);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRoles(Set.of(resolveRole(request.getRole())));
        }

        User updated = userRepository.save(user);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        studentAnswerRepository.deleteByStudent_Id(id);
        examResultRepository.deleteByStudent_Id(id);
        userRepository.delete(user);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully."));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Old password is required."));
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: New password is required."));
        }

        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Error: User is not found."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Old password is incorrect."));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully."));
    }

    private Role resolveRole(String roleInput) {
        String roleName = normalizeRoleName(roleInput);
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
    }

    private String normalizeRoleName(String roleInput) {
        if (roleInput == null || roleInput.isBlank()) {
            return "ROLE_STUDENT";
        }
        String normalized = roleInput.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return normalized;
    }

    private UserDTO toDTO(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(), roleNames);
    }

    @Data
    private static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String role;
    }

    @Data
    private static class UpdateUserRequest {
        private String username;
        private String password;
        private String email;
        private String role;
    }

    @Data
    private static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
