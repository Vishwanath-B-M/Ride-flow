package com.rideshare.rideservice.controller;

import com.rideshare.rideservice.dto.RideRequest;
import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@Slf4j
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    // Rider requests a new ride
    @PostMapping("/request")
    public ResponseEntity<RideResponse> requestRide(
            @Valid @RequestBody RideRequest rideRequest) {

        log.info("Ride request received from rider: {}", rideRequest.getRiderId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rideService.requestRide(rideRequest));
    }

    // Get ride by ID
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(
            @PathVariable Long rideId) {

        return ResponseEntity.ok(
                rideService.getRideById(rideId)
        );
    }

    // Get all rides of a rider
    @GetMapping("/rider/{riderId}")
    public ResponseEntity<List<RideResponse>> getRidesByRider(
            @PathVariable String riderId) {

        return ResponseEntity.ok(
                rideService.getRidesByRider(riderId)
        );
    }

    // Driver starts the ride
    @PutMapping("/{rideId}/start")
    public ResponseEntity<RideResponse> startRide(
            @PathVariable Long rideId) {

        return ResponseEntity.ok(
                rideService.startRide(rideId)
        );
    }

    // Complete the ride
    @PutMapping("/{rideId}/complete")
    public ResponseEntity<RideResponse> completeRide(
            @PathVariable Long rideId) {

        return ResponseEntity.ok(
                rideService.completeRide(rideId)
        );
    }

    // Cancel the ride
    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable Long rideId) {

        return ResponseEntity.ok(
                rideService.cancelRide(rideId)
        );
    }
}