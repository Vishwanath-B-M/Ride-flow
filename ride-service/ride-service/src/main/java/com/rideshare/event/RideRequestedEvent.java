package com.rideshare.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent {

    private String rideId;
    private String riderId;

    //PICKUP
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    //DROP
    private double dropLatitude;
    private double dropLongitude;
    private String dropAddress;
}