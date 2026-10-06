package com.ashray.seatreservation.dto;

import java.util.List;
import java.util.UUID;

public record CancelReservationResponse(UUID reservation_id, UUID show_id, String user_id, List<String> seats,
		Long amount_paise, String status) {
}