package com.scaler.harsh.userservice.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class Name implements Serializable {
    private String firstname;
    private String lastname;
}