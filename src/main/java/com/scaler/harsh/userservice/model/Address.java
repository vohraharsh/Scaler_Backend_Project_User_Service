package com.scaler.harsh.userservice.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class Address implements Serializable {
    private String city;
    private String street;
    private int number;
    private String zipcode;

    @Embedded
    private Geolocation geolocation;
}