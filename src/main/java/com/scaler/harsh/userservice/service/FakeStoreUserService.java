package com.scaler.harsh.userservice.service;

import com.scaler.harsh.userservice.dto.CreateUserRequestDTO;
import com.scaler.harsh.userservice.dto.FakeStoreUserDTO;
import com.scaler.harsh.userservice.exceptions.ServiceException;
import com.scaler.harsh.userservice.model.User;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeStoreUserService implements UserService {

    private final RestTemplate restTemplate;

    public FakeStoreUserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Integer id) {
        ResponseEntity<FakeStoreUserDTO> response;
        try {
            response = restTemplate.getForEntity(
                    "https://fakestoreapi.com/users/" + id, FakeStoreUserDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("User not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        return response.getBody().toUser();
    }

    @Override
    public List<User> getAllUsers() {
        ResponseEntity<FakeStoreUserDTO[]> response;
        try {
            response = restTemplate.getForEntity(
                    "https://fakestoreapi.com/users", FakeStoreUserDTO[].class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("User list not found", HttpStatus.NOT_FOUND);
        }

        List<User> users = new ArrayList<>();
        for (FakeStoreUserDTO dto : response.getBody()) {
            users.add(dto.toUser());
        }
        return users;
    }

    @Override
    public User createUser(CreateUserRequestDTO request) {
        FakeStoreUserDTO requestDto = FakeStoreUserDTO.from(request);
        FakeStoreUserDTO responseDto;

        try {
            responseDto = restTemplate.postForObject(
                    "https://fakestoreapi.com/users", requestDto, FakeStoreUserDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (responseDto == null) {
            throw new ServiceException("User creation failed", HttpStatus.BAD_REQUEST);
        }

        User user = requestDto.toUser();
        user.setId(responseDto.getId());
        return user;
    }

    @Override
    @CachePut(value = "users", key = "#id")
    public User updateUser(Integer id, CreateUserRequestDTO request) {
        FakeStoreUserDTO requestDto = FakeStoreUserDTO.from(request);
        HttpEntity<FakeStoreUserDTO> requestEntity = new HttpEntity<>(requestDto);

        ResponseEntity<FakeStoreUserDTO> response;
        try {
            response = restTemplate.exchange(
                    "https://fakestoreapi.com/users/" + id,
                    HttpMethod.PUT, requestEntity, FakeStoreUserDTO.class);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response.getBody() == null) {
            throw new ServiceException("User not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        User user = requestDto.toUser();
        user.setId(id);
        return user;
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Integer id) {
        try {
            restTemplate.delete("https://fakestoreapi.com/users/" + id);
        } catch (RestClientException e) {
            throw new ServiceException("Could not reach FakeStoreAPI", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void forgotPassword(String email) {
        throw new UnsupportedOperationException("Not supported by FakeStoreUserService");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        throw new UnsupportedOperationException("Not supported by FakeStoreUserService");
    }

    @Override
    public User verifyCredentials(String username, String password) {
        throw new UnsupportedOperationException("Not supported by FakeStoreUserService");
    }
}