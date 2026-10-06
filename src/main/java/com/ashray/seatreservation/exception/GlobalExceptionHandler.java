package com.ashray.seatreservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(SeatTakenException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleSeatTaken(SeatTakenException exception) {
		return new ErrorResponse("SEAT_TAKEN", exception.getMessage());
	}

	@ExceptionHandler(SeatNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleSeatNotFound(SeatNotFoundException exception) {
		return new ErrorResponse("SEAT_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(ShowNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleShowNotFound(ShowNotFoundException exception) {
		return new ErrorResponse("SHOW_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PerUserLimitExceededException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handlePerUserLimit(PerUserLimitExceededException exception) {
		return new ErrorResponse("PER_USER_LIMIT_EXCEEDED", exception.getMessage());
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleIdempotencyConflict(IdempotencyConflictException exception) {
		return new ErrorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage());
	}

	@ExceptionHandler(ReservationNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleReservationNotFound(ReservationNotFoundException exception) {
		return new ErrorResponse("RESERVATION_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(ReservationNotOwnedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleReservationNotOwned(ReservationNotOwnedException exception) {
		return new ErrorResponse("RESERVATION_NOT_OWNED", exception.getMessage());
	}

	@ExceptionHandler(ReservationNotCancellableException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleReservationNotCancellable(ReservationNotCancellableException exception) {
		return new ErrorResponse("RESERVATION_NOT_CANCELLABLE", exception.getMessage());
	}

	@ExceptionHandler(IdempotencyKeyAlreadyCancelledException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleCancelledIdempotencyKey(IdempotencyKeyAlreadyCancelledException exception) {
		return new ErrorResponse("IDEMPOTENCY_KEY_CANCELLED", exception.getMessage());
	}

	public record ErrorResponse(String code, String message) {
	}
}