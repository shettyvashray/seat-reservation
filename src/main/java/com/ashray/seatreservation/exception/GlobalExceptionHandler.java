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

	public record ErrorResponse(String code, String message) {
	}
}