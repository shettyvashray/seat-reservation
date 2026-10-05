package com.ashray.seatreservation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ashray.seatreservation.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}