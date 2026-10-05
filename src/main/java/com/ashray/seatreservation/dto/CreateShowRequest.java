package com.ashray.seatreservation.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateShowRequest(

		@NotBlank String name,

		@NotEmpty List<String> seats,

		@Min(1) Long price_paise

) {
}