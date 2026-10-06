package com.ashray.seatreservation.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(SeatTakenException.class)
	public ResponseEntity<ErrorResponse> handleSeatTaken(SeatTakenException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("SEAT_TAKEN", exception.getMessage()));
	}

	@ExceptionHandler(SeatNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleSeatNotFound(SeatNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("SEAT_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(ShowNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleShowNotFound(ShowNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("SHOW_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(PerUserLimitExceededException.class)
	public ResponseEntity<ErrorResponse> handlePerUserLimit(PerUserLimitExceededException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("PER_USER_LIMIT_EXCEEDED", exception.getMessage()));
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage()));
	}

	@ExceptionHandler(ReservationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorResponse("RESERVATION_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(ReservationNotOwnedException.class)
	public ResponseEntity<ErrorResponse> handleReservationNotOwned(ReservationNotOwnedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("RESERVATION_NOT_OWNED", exception.getMessage()));
	}

	@ExceptionHandler(ReservationNotCancellableException.class)
	public ResponseEntity<ErrorResponse> handleReservationNotCancellable(ReservationNotCancellableException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("RESERVATION_NOT_CANCELLABLE", exception.getMessage()));
	}

	@ExceptionHandler(IdempotencyKeyAlreadyCancelledException.class)
	public ResponseEntity<ErrorResponse> handleCancelledIdempotencyKey(
			IdempotencyKeyAlreadyCancelledException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("IDEMPOTENCY_KEY_CANCELLED", exception.getMessage()));
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("MISSING_HEADER", "Required header is missing: " + exception.getHeaderName()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("INVALID_JSON", "Request body is invalid"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("INVALID_REQUEST", "Invalid value for parameter: " + exception.getName()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("INVALID_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ErrorResponse("CONFLICT", "The request conflicts with the current resource state"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
		exception.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
	}

	public record ErrorResponse(String code, String message) {
	}
}