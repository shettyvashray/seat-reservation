package com.ashray.seatreservation.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ReserveSeatRequest(

		@NotEmpty List<String> seats

) {
}