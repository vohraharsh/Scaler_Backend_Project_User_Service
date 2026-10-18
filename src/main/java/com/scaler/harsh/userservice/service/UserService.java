package com.scaler.harsh.userservice.service;

import com.scaler.harsh.userservice.dto.CreateUserRequestDTO;
import com.scaler.harsh.userservice.model.User;

import java.util.List;

public interface UserService {
    User getUserById(Integer id);
    List<User> getAllUsers();
    User createUser(CreateUserRequestDTO request);
    User updateUser(Integer id, CreateUserRequestDTO request);
    void deleteUser(Integer id);
}