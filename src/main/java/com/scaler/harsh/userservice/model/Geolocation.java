package com.scaler.harsh.userservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Geolocation {
    private String lat;

    @JsonProperty("long")
    private String lon;
}
