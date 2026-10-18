package com.scaler.harsh.userservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class Geolocation implements Serializable {
    private String lat;

    @JsonProperty("long")
    private String lon;
}