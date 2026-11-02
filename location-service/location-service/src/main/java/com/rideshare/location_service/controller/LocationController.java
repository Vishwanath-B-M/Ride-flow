
        package com.rideshare.location_service.controller;

import com.rideshare.location_service.dto.DriverLocationRequest;
import com.rideshare.location_service.dto.NearByDriverResponse;
import com.rideshare.location_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // Driver phone calls this every 3 seconds
    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(
            @RequestBody DriverLocationRequest driverLocationRequest) {

        locationService.updateDriverLocation(driverLocationRequest);

        return ResponseEntity.ok("Driver Location updated");
    }

    // Matching service calls this
    @GetMapping("/driver/nearby")
    public ResponseEntity<List<NearByDriverResponse>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radius) {

        return ResponseEntity.ok(
                locationService.findNearbyDrivers(
                        latitude,
                        longitude,
                        radius
                )
        );
    }

    // Called when driver goes offline
    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<String> removeDriver(
            @PathVariable String driverId) {

        locationService.removeDriver(driverId);

        return ResponseEntity.ok("Driver removed Successfully");
    }
}

