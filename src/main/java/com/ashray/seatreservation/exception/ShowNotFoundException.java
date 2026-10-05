package com.ashray.seatreservation.exception;

public class ShowNotFoundException extends RuntimeException {

	public ShowNotFoundException() {
		super("Show not found");
	}
}