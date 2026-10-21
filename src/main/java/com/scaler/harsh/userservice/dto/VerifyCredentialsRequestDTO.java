package com.scaler.harsh.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyCredentialsRequestDTO {
    private String username;
    private String password;
}