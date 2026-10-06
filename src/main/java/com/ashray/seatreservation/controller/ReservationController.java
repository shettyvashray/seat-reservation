package com.ashray.seatreservation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ashray.seatreservation.dto.CancelReservationResponse;
import com.ashray.seatreservation.dto.ReservationResponse;
import com.ashray.seatreservation.dto.ReserveSeatRequest;
import com.ashray.seatreservation.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/shows")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping("/{showId}/reserve")
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse reserve(@PathVariable UUID showId, @RequestHeader("X-User-Id") String userId,
			@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody ReserveSeatRequest request) {
		return reservationService.reserve(showId, userId, request, idempotencyKey);
	}

	@PostMapping("/reservations/{reservationId}/cancel")
	@ResponseStatus(HttpStatus.OK)
	public CancelReservationResponse cancel(@PathVariable UUID reservationId,
			@RequestHeader("X-User-Id") String userId) {
		return reservationService.cancel(reservationId, userId);
	}
}