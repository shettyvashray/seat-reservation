package com.ashray.seatreservation.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "seats", uniqueConstraints = {
		@UniqueConstraint(name = "uq_show_seat", columnNames = { "show_id", "seat_number" }) })
public class Seat {

	@Id
	private UUID id;

	@Column(name = "show_id", nullable = false)
	private UUID showId;

	@Column(name = "seat_number", nullable = false)
	private String seatNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SeatStatus status;

	@Column(name = "reservation_id")
	private UUID reservationId;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Seat() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getShowId() {
		return showId;
	}

	public void setShowId(UUID showId) {
		this.showId = showId;
	}

	public String getSeatNumber() {
		return seatNumber;
	}

	public void setSeatNumber(String seatNumber) {
		this.seatNumber = seatNumber;
	}

	public SeatStatus getStatus() {
		return status;
	}

	public void setStatus(SeatStatus status) {
		this.status = status;
	}

	public UUID getReservationId() {
		return reservationId;
	}

	public void setReservationId(UUID reservationId) {
		this.reservationId = reservationId;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}