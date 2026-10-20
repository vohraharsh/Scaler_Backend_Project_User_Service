package com.scaler.harsh.userservice.controller;

import com.scaler.harsh.userservice.dto.CreateUserRequestDTO;
import com.scaler.harsh.userservice.dto.UserResponseDTO;
import com.scaler.harsh.userservice.exceptions.ServiceException;
import com.scaler.harsh.userservice.model.User;
import com.scaler.harsh.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.scaler.harsh.userservice.dto.ForgotPasswordRequestDTO;
import com.scaler.harsh.userservice.dto.ResetPasswordRequestDTO;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

    private UserService svc;

    public UserController(UserService svc) {
        this.svc = svc;
    }

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = svc.getAllUsers();
        List<UserResponseDTO> response = new ArrayList<>();
        for (User user : users) {
            response.add(convertToResponseDTO(user));
        }
        return response;
    }

    @GetMapping("/users/{id}")
    public UserResponseDTO getUserById(@PathVariable("id") Integer id) {
        if (id <= 0) {
            throw new ServiceException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        return convertToResponseDTO(svc.getUserById(id));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody CreateUserRequestDTO request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ServiceException("Username cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ServiceException("Email cannot be empty", HttpStatus.BAD_REQUEST);
        }
        User user = svc.createUser(request);
        return new ResponseEntity<>(convertToResponseDTO(user), HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}")
    public UserResponseDTO updateUser(@PathVariable("id") Integer id,
                                      @RequestBody CreateUserRequestDTO request) {
        if (id <= 0) {
            throw new ServiceException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        return convertToResponseDTO(svc.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") Integer id) {
        if (id <= 0) {
            throw new ServiceException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        svc.deleteUser(id);
        return "User with id " + id + " deleted";
    }

    private UserResponseDTO convertToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setAddress(user.getAddress());
        dto.setPhone(user.getPhone());
        return dto;
    }

    @PostMapping("/users/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ServiceException("Email cannot be empty", HttpStatus.BAD_REQUEST);
        }
        svc.forgotPassword(request.getEmail());
        return "If an account exists for this email, a reset link has been generated.";
    }

    @PostMapping("/users/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ServiceException("Token cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ServiceException("New password cannot be empty", HttpStatus.BAD_REQUEST);
        }
        svc.resetPassword(request.getToken(), request.getNewPassword());
        return "Password has been reset successfully.";
    }
}