package com.rideshare.matchingservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
Response recevied from location service
when querying for neary by driver
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByDriverResponse {
    private String driverId;
    private double latitude;
    private double longitude;
    private double distanceInKm;
}
