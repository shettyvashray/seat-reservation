package com.ashray.seatreservation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ashray.seatreservation.dto.CreateShowRequest;
import com.ashray.seatreservation.dto.ShowResponse;
import com.ashray.seatreservation.service.ShowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/shows")
public class ShowController {

	private final ShowService showService;

	public ShowController(ShowService showService) {
		this.showService = showService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ShowResponse createShow(@Valid @RequestBody CreateShowRequest request) {
		return showService.createShow(request);
	}

	@GetMapping("/{id}")
	public ShowResponse getShow(@PathVariable UUID id) {
		return showService.getShow(id);
	}
}