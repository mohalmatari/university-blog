package com.unversity.blog.demo.service;

import com.unversity.blog.demo.config.SecurityUtils;
import com.unversity.blog.demo.dto.UserRequestDTO;
import com.unversity.blog.demo.dto.UserResponseDTO;
import com.unversity.blog.demo.exception.ResourceNotFoundException;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityUtils securityUtils;

    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        User user = new User();
        user.setUsername(requestDTO.getUsername());
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setRole(User.Role.USER);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser);
    }

    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponseDTO::new);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return new UserResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner = existingUser.getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to modify this user");
        }
        existingUser.setUsername(requestDTO.getUsername());
        existingUser.setEmail(requestDTO.getEmail());
        existingUser.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        User updatedUser = userRepository.save(existingUser);
        return new UserResponseDTO(updatedUser);
    }

    @Transactional
    public String deleteUser(Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner = existingUser.getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to delete this user");
        }

        userRepository.deleteById(id);
        return "User with id " + id + " was deleted successfully";
    }
}