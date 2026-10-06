package com.ashray.seatreservation.exception;

public class ReservationNotOwnedException extends RuntimeException {

	public ReservationNotOwnedException() {
		super("You are not allowed to cancel this reservation");
	}
}