package com.ashray.seatreservation.dto;

import java.util.List;
import java.util.UUID;

public record ShowResponse(UUID id, String name, Long price_paise, Integer total_seats, Integer available, Integer held,
		Integer confirmed, List<SeatResponse> seats) {
}