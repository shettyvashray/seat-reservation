package com.ashray.seatreservation.exception;

public class PerUserLimitExceededException extends RuntimeException {

	public PerUserLimitExceededException(int limit) {
		super("User cannot reserve more than " + limit + " seats for this show");
	}
}