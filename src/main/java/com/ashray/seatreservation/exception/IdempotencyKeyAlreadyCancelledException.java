package com.ashray.seatreservation.exception;

public class IdempotencyKeyAlreadyCancelledException extends RuntimeException {

	public IdempotencyKeyAlreadyCancelledException() {
		super("Idempotency key belongs to a cancelled reservation");
	}
}