package com.scaler.harsh.userservice.service;

import com.scaler.harsh.userservice.dto.CreateUserRequestDTO;
import com.scaler.harsh.userservice.exceptions.ServiceException;
import com.scaler.harsh.userservice.model.Address;
import com.scaler.harsh.userservice.model.Name;
import com.scaler.harsh.userservice.model.PasswordResetToken;
import com.scaler.harsh.userservice.model.User;
import com.scaler.harsh.userservice.repository.PasswordResetTokenRepository;
import com.scaler.harsh.userservice.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
public class SelfUserService implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public SelfUserService(UserRepository userRepository, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Integer id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ServiceException("User not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        return user.get();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User createUser(CreateUserRequestDTO request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());

        Name name = new Name();
        name.setFirstname(request.getName().getFirstname());
        name.setLastname(request.getName().getLastname());
        user.setName(name);

        Address address = new Address();
        address.setCity(request.getAddress().getCity());
        address.setStreet(request.getAddress().getStreet());
        address.setNumber(request.getAddress().getNumber());
        address.setZipcode(request.getAddress().getZipcode());
        address.setGeolocation(request.getAddress().getGeolocation());
        user.setAddress(address);

        return userRepository.save(user);
    }

    @Override
    @CachePut(value = "users", key = "#id")
    public User updateUser(Integer id, CreateUserRequestDTO request) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ServiceException("User not found for id: " + id, HttpStatus.NOT_FOUND);
        }

        User user = existing.get();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());

        Name name = new Name();
        name.setFirstname(request.getName().getFirstname());
        name.setLastname(request.getName().getLastname());
        user.setName(name);

        Address address = new Address();
        address.setCity(request.getAddress().getCity());
        address.setStreet(request.getAddress().getStreet());
        address.setNumber(request.getAddress().getNumber());
        address.setZipcode(request.getAddress().getZipcode());
        address.setGeolocation(request.getAddress().getGeolocation());
        user.setAddress(address);

        return userRepository.save(user);
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ServiceException("User not found for id: " + id, HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ServiceException("No user found for email: " + email, HttpStatus.NOT_FOUND);
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUserId(user.getId());
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(resetToken);

        System.out.println("Password reset link: http://localhost:8081/users/reset-password?token=" + resetToken.getToken());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null) {
            throw new ServiceException("Invalid reset token", HttpStatus.BAD_REQUEST);
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ServiceException("Reset token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ServiceException("User not found", HttpStatus.NOT_FOUND));

        user.setPassword(newPassword);
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
}