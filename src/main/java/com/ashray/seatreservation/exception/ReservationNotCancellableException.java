package com.ashray.seatreservation.exception;

public class ReservationNotCancellableException extends RuntimeException {

	public ReservationNotCancellableException() {
		super("Reservation cannot be cancelled");
	}
}