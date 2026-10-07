package com.ashray.seatreservation.metrics;

import org.springframework.stereotype.Component;

import com.ashray.seatreservation.entity.SeatStatus;
import com.ashray.seatreservation.repository.SeatRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ReservationMetrics {

	private final Counter confirmedReservations;
	private final Counter seatTakenDeclines;
	private final Counter perUserLimitDeclines;
	private final Counter idempotentReplays;
	private final Counter idempotencyConflicts;

	public ReservationMetrics(MeterRegistry meterRegistry, SeatRepository seatRepository) {

		this.confirmedReservations = Counter.builder("reservations.confirmed")
				.description("Number of confirmed reservations").register(meterRegistry);

		this.seatTakenDeclines = Counter.builder("reservations.declined").tag("reason", "seat_taken")
				.description("Number of declined reservation requests").register(meterRegistry);

		this.perUserLimitDeclines = Counter.builder("reservations.declined").tag("reason", "per_user_limit")
				.description("Number of declined reservation requests").register(meterRegistry);

		this.idempotentReplays = Counter.builder("reservations.declined").tag("reason", "idempotent_replay")
				.description("Number of idempotent reservation replays").register(meterRegistry);

		this.idempotencyConflicts = Counter.builder("reservations.declined").tag("reason", "idempotency_conflict")
				.description("Number of idempotency conflicts").register(meterRegistry);

		Gauge.builder("seats.available", seatRepository, repository -> repository.countByStatus(SeatStatus.AVAILABLE))
				.description("Number of currently available seats").register(meterRegistry);
	}

	public void reservationConfirmed() {
		confirmedReservations.increment();
	}

	public void seatTaken() {
		seatTakenDeclines.increment();
	}

	public void perUserLimitExceeded() {
		perUserLimitDeclines.increment();
	}

	public void idempotentReplay() {
		idempotentReplays.increment();
	}

	public void idempotencyConflict() {
		idempotencyConflicts.increment();
	}
}