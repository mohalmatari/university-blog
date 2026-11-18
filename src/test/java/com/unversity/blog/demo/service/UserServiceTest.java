package com.unversity.blog.demo.service;

import com.unversity.blog.demo.config.SecurityUtils;
import com.unversity.blog.demo.dto.UserRequestDTO;
import com.unversity.blog.demo.dto.UserResponseDTO;
import com.unversity.blog.demo.exception.ResourceNotFoundException;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserService userService;

    // helpers

    private User user(Long id, String username, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword("old-hash");
        u.setRole(role);
        return u;
    }

    private UserRequestDTO request(String username, String email, String password) {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    // createUser

    @Test
    @DisplayName("createUser stores a hashed password, never the plain one")
    void createUser_hashesThePassword() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request("mohammed", "m@test.com", "secret123"));

        // Capture the User that was handed to save() and look at its password.
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed-secret", captor.getValue().getPassword());
        assertNotEquals("secret123", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("createUser always creates a normal USER, never an admin")
    void createUser_setsRoleUserAndCreatedAt() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.createUser(request("mohammed", "m@test.com", "secret123"));

        assertEquals("mohammed", result.getUsername());
        assertEquals("m@test.com", result.getEmail());
        assertEquals(User.Role.USER, result.getRole());
        assertNotNull(result.getCreatedAt());
    }

    // getUserById/getAllUsers

    @Test
    @DisplayName("getUserById returns the user when it exists")
    void getUserById_whenExists_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "mohammed", User.Role.USER)));

        UserResponseDTO result = userService.getUserById(1L);

        assertEquals("mohammed", result.getUsername());
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException when the user does not exist")
    void getUserById_whenMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("getAllUsers maps the page of users to DTOs")
    void getAllUsers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user(1L, "mohammed", User.Role.USER)), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserResponseDTO> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("mohammed", result.getContent().get(0).getUsername());
    }

    //  updateUser

    @Test
    @DisplayName("updateUser lets a user edit their own account and re-hashes the password")
    void updateUser_byOwner_succeeds() {
        User owner = user(1L, "mohammed", User.Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.updateUser(1L, request("new_name", "new@test.com", "newpass123"));

        assertEquals("new_name", result.getUsername());
        assertEquals("new@test.com", result.getEmail());
        assertEquals("new-hash", owner.getPassword());
    }

    @Test
    @DisplayName("updateUser lets an admin edit someone else's account")
    void updateUser_byAdmin_succeeds() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User admin = user(3L, "admin", User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(admin);
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.updateUser(1L, request("moderated", "m@test.com", "newpass123"));

        assertEquals("moderated", result.getUsername());
    }

    @Test
    @DisplayName("updateUser rejects a user who is neither the account owner nor an admin")
    void updateUser_byOtherUser_throwsAccessDenied() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User stranger = user(2L, "someone_else", User.Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(stranger);

        assertThrows(AccessDeniedException.class,
                () -> userService.updateUser(1L, request("hacked", "h@test.com", "hacked123")));

        verify(userRepository, never()).save(any(User.class));
        assertEquals("mohammed", owner.getUsername()); // untouched
    }

    @Test
    @DisplayName("updateUser throws ResourceNotFoundException when the user does not exist")
    void updateUser_whenMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(99L, request("name", "n@test.com", "pass123")));

        verify(userRepository, never()).save(any(User.class));
    }

    // deleteUser

    @Test
    @DisplayName("deleteUser lets a user delete their own account")
    void deleteUser_byOwner_deletes() {
        User owner = user(1L, "mohammed", User.Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(owner);

        String message = userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
        assertEquals("User with id 1 was deleted successfully", message);
    }

    @Test
    @DisplayName("deleteUser lets an admin delete someone else's account")
    void deleteUser_byAdmin_deletes() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User admin = user(3L, "admin", User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(admin);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser rejects a user who is neither the account owner nor an admin")
    void deleteUser_byOtherUser_throwsAccessDenied() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User stranger = user(2L, "someone_else", User.Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUser()).thenReturn(stranger);

        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(1L));

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteUser throws ResourceNotFoundException when the user does not exist")
    void deleteUser_whenMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));

        verify(userRepository, never()).deleteById(any());
    }
}