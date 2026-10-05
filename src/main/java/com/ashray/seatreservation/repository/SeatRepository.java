package com.ashray.seatreservation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ashray.seatreservation.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

	List<Seat> findByShowIdOrderBySeatNumber(UUID showId);
}