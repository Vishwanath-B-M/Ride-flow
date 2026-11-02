package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {

    private final MatchingService matchingService;

    /**
     * Listens to ride.requested kafka topic.
     * Triggered every time Ride Service publishes a new ride request.
     *
     * FLOW:
     * Ride Service
     *      ↓
     * Kafka: ride.requested
     *      ↓
     * This Consumer
     *      ↓
     * MatchingService
     */
    @KafkaListener(
            topics = "ride.requested",
            groupId = "matching-service-group"
    )
    public void consumeRideRequestedEvent(RideRequestedEvent event) {

        log.info("========================================");
        log.info("RIDE REQUEST RECEIVED BY MATCHING SERVICE");
        log.info("Ride ID: {}", event.getRideId());
        log.info("Rider ID: {}", event.getRiderId());
        log.info(
                "Pickup: {}, {}",
                event.getPickupLatitude(),
                event.getPickupLongitude()
        );
        log.info("========================================");

        try {

            matchingService.matchDriverForRide(event);

            log.info(
                    "Matching completed for ride: {}",
                    event.getRideId()
            );

        } catch (Exception e) {

            log.error(
                    "ERROR processing ride request: {}",
                    event.getRideId(),
                    e
            );
        }
    }
}