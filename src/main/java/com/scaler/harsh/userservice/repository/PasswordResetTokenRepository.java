package com.scaler.harsh.userservice.repository;

import com.scaler.harsh.userservice.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    PasswordResetToken findByToken(String token);
}