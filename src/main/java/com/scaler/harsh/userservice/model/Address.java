package com.scaler.harsh.userservice.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Address implements Serializable {
    private String city;
    private String street;
    private int number;
    private String zipcode;
    private Geolocation geolocation;
}
