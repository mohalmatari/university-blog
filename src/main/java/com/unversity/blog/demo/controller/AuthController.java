package com.unversity.blog.demo.controller;

import com.unversity.blog.demo.config.JwtUtil;
import com.unversity.blog.demo.dto.LoginRequestDTO;
import com.unversity.blog.demo.dto.LoginResponseDTO;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import com.unversity.blog.demo.dto.UserRequestDTO;
import com.unversity.blog.demo.dto.UserResponseDTO;
import com.unversity.blog.demo.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody UserRequestDTO requestDTO) {
        return userService.createUser(requestDTO);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDTO.getUsername(), requestDTO.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = (User) authentication.getPrincipal();

        String token = jwtUtil.generateToken(user.getUsername());

        return new LoginResponseDTO(token, user.getUsername(), user.getRole().name());
    }
}