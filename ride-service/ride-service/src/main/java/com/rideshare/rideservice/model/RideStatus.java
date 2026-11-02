package com.rideshare.rideservice.model;

/**
 * FLOw
 * REQUESTED->MAtching->ACcepted->Driver_Arriving->ride_started->completed->cancelled
 *
 *
 */

public enum RideStatus {
    REQUESTED,
    MATCHING,
    ACCEPTED,
    DRIVER_ARRIVING,
    RIDE_STARTED,
    COMPLETED,
    CANCELLED



}
