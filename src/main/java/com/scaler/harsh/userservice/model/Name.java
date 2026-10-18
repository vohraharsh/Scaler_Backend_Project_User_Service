package com.scaler.harsh.userservice.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Name implements Serializable {
    private String firstname;
    private String lastname;
}
