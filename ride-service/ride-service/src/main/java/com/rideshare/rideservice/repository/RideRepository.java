package com.rideshare.rideservice.repository;

import com.rideshare.rideservice.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository  extends JpaRepository<Ride,Long> {
    List<Ride> findByRiderIdOrderByCreatedAtDesc(String riderId);
}
