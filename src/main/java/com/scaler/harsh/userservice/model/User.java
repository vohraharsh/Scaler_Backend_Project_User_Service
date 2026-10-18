package com.scaler.harsh.userservice.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class User implements Serializable {
    private Integer id;
    private String email;
    private String username;
    private String password;
    private Name name;
    private Address address;
    private String phone;
}
