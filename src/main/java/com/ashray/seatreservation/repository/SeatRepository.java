package com.ashray.seatreservation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ashray.seatreservation.entity.Seat;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

	List<Seat> findByShowIdOrderBySeatNumber(UUID showId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    SELECT s
			    FROM Seat s
			    WHERE s.showId = :showId
			      AND s.seatNumber IN :seatNumbers
			    ORDER BY s.seatNumber
			""")
	List<Seat> findSeatsForUpdate(@Param("showId") UUID showId, @Param("seatNumbers") List<String> seatNumbers);

	@Query("""
			    SELECT s.seatNumber
			    FROM Seat s
			    WHERE s.reservationId = :reservationId
			    ORDER BY s.seatNumber
			""")
	List<String> findSeatNumbersByReservationId(@Param("reservationId") UUID reservationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    SELECT s
			    FROM Seat s
			    WHERE s.reservationId = :reservationId
			    ORDER BY s.seatNumber
			""")
	List<Seat> findSeatsForReservationForUpdate(@Param("reservationId") UUID reservationId);
}