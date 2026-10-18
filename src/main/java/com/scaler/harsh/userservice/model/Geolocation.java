package com.scaler.harsh.userservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Geolocation implements Serializable {
    private String lat;

    @JsonProperty("long")
    private String lon;
}
