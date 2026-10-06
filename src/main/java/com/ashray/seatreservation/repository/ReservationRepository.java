package com.ashray.seatreservation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ashray.seatreservation.entity.Reservation;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

	@Query(value = """
			SELECT COUNT(*)
			FROM seats s
			JOIN reservations r
			  ON r.id = s.reservation_id
			WHERE r.show_id = :showId
			  AND r.user_id = :userId
			  AND r.status = 'CONFIRMED'
			  AND s.status = 'CONFIRMED'
			""", nativeQuery = true)
	long countConfirmedSeatsForUser(@Param("showId") UUID showId, @Param("userId") String userId);

	Optional<Reservation> findByShowIdAndUserIdAndIdempotencyKey(UUID showId, String userId, String idempotencyKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    SELECT r
			    FROM Reservation r
			    WHERE r.id = :reservationId
			""")
	Optional<Reservation> findByIdForUpdate(@Param("reservationId") UUID reservationId);
}