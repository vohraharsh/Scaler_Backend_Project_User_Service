package com.scaler.harsh.userservice.dto;

import com.scaler.harsh.userservice.model.Address;
import com.scaler.harsh.userservice.model.Name;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserResponseDTO {
    private Integer id;
    private String email;
    private String username;
    private Name name;
    private Address address;
    private String phone;
}