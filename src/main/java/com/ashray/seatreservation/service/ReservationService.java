package com.ashray.seatreservation.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashray.seatreservation.dto.ReservationResponse;
import com.ashray.seatreservation.dto.ReserveSeatRequest;
import com.ashray.seatreservation.entity.Reservation;
import com.ashray.seatreservation.entity.ReservationStatus;
import com.ashray.seatreservation.entity.Seat;
import com.ashray.seatreservation.entity.SeatStatus;
import com.ashray.seatreservation.entity.Show;
import com.ashray.seatreservation.exception.IdempotencyConflictException;
import com.ashray.seatreservation.exception.PerUserLimitExceededException;
import com.ashray.seatreservation.exception.SeatNotFoundException;
import com.ashray.seatreservation.exception.SeatTakenException;
import com.ashray.seatreservation.exception.ShowNotFoundException;
import com.ashray.seatreservation.repository.ReservationRepository;
import com.ashray.seatreservation.repository.SeatRepository;
import com.ashray.seatreservation.repository.ShowRepository;
import com.ashray.seatreservation.repository.ShowUserLockRepository;

@Service
public class ReservationService {

	private final ShowRepository showRepository;
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;
	private final ShowUserLockRepository showUserLockRepository;
	private final RequestHashService requestHashService;

	public ReservationService(ShowRepository showRepository, SeatRepository seatRepository,
			ReservationRepository reservationRepository, ShowUserLockRepository showUserLockRepository,
			RequestHashService requestHashService) {
		this.showRepository = showRepository;
		this.seatRepository = seatRepository;
		this.reservationRepository = reservationRepository;
		this.showUserLockRepository = showUserLockRepository;
		this.requestHashService = requestHashService;
	}

	@Transactional
	public ReservationResponse reserve(UUID showId, String userId, ReserveSeatRequest request, String idempotencyKey) {

		// Find the show
		Show show = showRepository.findById(showId).orElseThrow(ShowNotFoundException::new);

		// Sort seats to get them in the same order
		List<String> requestedSeats = request.seats().stream().distinct().sorted().toList();

		// Generate request hash for idempotency
		String requestHash = requestHashService.hashSeats(requestedSeats);

		// Create lock for show user limit
		showUserLockRepository.createIfAbsent(showId, userId);

		// Acquire lock for show user limit
		showUserLockRepository.findForUpdate(showId, userId)
				.orElseThrow(() -> new IllegalStateException("Unable to acquire user/show lock"));

		// Idempotency check
		Optional<Reservation> existingReservation = reservationRepository.findByShowIdAndUserIdAndIdempotencyKey(showId,
				userId, idempotencyKey);

		if (existingReservation.isPresent()) {

			Reservation reservation = existingReservation.get();

			if (!reservation.getRequestHash().equals(requestHash)) {
				throw new IdempotencyConflictException();
			}

			List<String> existingSeats = seatRepository.findSeatNumbersByReservationId(reservation.getId());

			return new ReservationResponse(reservation.getId(), reservation.getShowId(), reservation.getUserId(),
					existingSeats, reservation.getAmountPaise(), reservation.getStatus().name());
		}

		// Enforce per user limit
		long currentSeatCount = reservationRepository.countConfirmedSeatsForUser(showId, userId);

		if (currentSeatCount + requestedSeats.size() > show.getPerUserLimit()) {
			throw new PerUserLimitExceededException(show.getPerUserLimit());
		}

		// Lock all requested seats.
		List<Seat> seats = seatRepository.findSeatsForUpdate(showId, requestedSeats);

		// Check if all seats are present
		if (seats.size() != requestedSeats.size()) {
			for (String requestedSeat : requestedSeats) {
				boolean exists = seats.stream().anyMatch(seat -> seat.getSeatNumber().equals(requestedSeat));

				if (!exists) {
					throw new SeatNotFoundException(requestedSeat);
				}
			}
		}

		// Check all seat status
		for (Seat seat : seats) {
			if (seat.getStatus() != SeatStatus.AVAILABLE) {
				throw new SeatTakenException(seat.getSeatNumber());
			}
		}

		// Create reservation
		Reservation reservation = new Reservation();

		UUID reservationId = UUID.randomUUID();

		reservation.setId(reservationId);
		reservation.setShowId(showId);
		reservation.setUserId(userId);

		long amount = show.getPricePaise() * requestedSeats.size();

		reservation.setAmountPaise(amount);
		reservation.setStatus(ReservationStatus.CONFIRMED);

		reservation.setIdempotencyKey(idempotencyKey);
		reservation.setRequestHash(requestHash);

		OffsetDateTime now = OffsetDateTime.now();

		reservation.setCreatedAt(now);
		reservation.setUpdatedAt(now);

		reservationRepository.save(reservation);

		// Mark seats as confirmed
		for (Seat seat : seats) {
			seat.setStatus(SeatStatus.CONFIRMED);
			seat.setReservationId(reservationId);
			seat.setUpdatedAt(now);
		}

		seatRepository.saveAll(seats);

		// Return result
		return new ReservationResponse(reservationId, showId, userId, requestedSeats, amount, "confirmed");
	}
}