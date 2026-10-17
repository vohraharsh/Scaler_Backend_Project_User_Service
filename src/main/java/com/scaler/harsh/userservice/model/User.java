package com.scaler.harsh.userservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Integer id;
    private String email;
    private String username;
    private String password;
    private Name name;
    private Address address;
    private String phone;
}
