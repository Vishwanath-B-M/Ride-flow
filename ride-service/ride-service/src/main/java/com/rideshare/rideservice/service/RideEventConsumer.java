package com.rideshare.rideservice.service;

import com.rideshare.rideservice.event.RideMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {

    private final RideService rideService;

    @KafkaListener(
            topics = "ride.matched",
            groupId = "ride-service-group"
    )
    public void consumeRideMatchedEvent(RideMatchedEvent event) {

        Long rideId = Long.valueOf(event.getRideId());

        rideService.updateRideWithDriver(
                rideId,
                event.getDriverId()
        );

        log.info(
                "Ride {} matched with driver {}",
                rideId,
                event.getDriverId()
        );
    }
}