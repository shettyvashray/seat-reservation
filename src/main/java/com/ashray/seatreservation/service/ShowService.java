package com.ashray.seatreservation.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashray.seatreservation.dto.CreateShowRequest;
import com.ashray.seatreservation.dto.SeatResponse;
import com.ashray.seatreservation.dto.ShowResponse;
import com.ashray.seatreservation.entity.Seat;
import com.ashray.seatreservation.entity.SeatStatus;
import com.ashray.seatreservation.entity.Show;
import com.ashray.seatreservation.exception.ShowNotFoundException;
import com.ashray.seatreservation.repository.SeatRepository;
import com.ashray.seatreservation.repository.ShowRepository;

@Service
public class ShowService {

	private static final int DEFAULT_PER_USER_LIMIT = 4;

	private final ShowRepository showRepository;
	private final SeatRepository seatRepository;

	public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
		this.showRepository = showRepository;
		this.seatRepository = seatRepository;
	}

	@Transactional
	public ShowResponse createShow(CreateShowRequest request) {

		Show show = new Show();
		show.setId(UUID.randomUUID());
		show.setName(request.name());
		show.setPricePaise(request.price_paise());
		show.setPerUserLimit(DEFAULT_PER_USER_LIMIT);
		show.setCreatedAt(OffsetDateTime.now());

		showRepository.save(show);

		OffsetDateTime now = OffsetDateTime.now();

		List<Seat> seats = request.seats().stream().distinct().map(seatNumber -> {
			Seat seat = new Seat();
			seat.setId(UUID.randomUUID());
			seat.setShowId(show.getId());
			seat.setSeatNumber(seatNumber);
			seat.setStatus(SeatStatus.AVAILABLE);
			seat.setUpdatedAt(now);
			return seat;
		}).toList();

		seatRepository.saveAll(seats);

		return toResponse(show, seats);
	}

	@Transactional(readOnly = true)
	public ShowResponse getShow(UUID showId) {

		Show show = showRepository.findById(showId).orElseThrow(() -> new ShowNotFoundException());

		List<Seat> seats = seatRepository.findByShowIdOrderBySeatNumber(showId);

		return toResponse(show, seats);
	}

	private ShowResponse toResponse(Show show, List<Seat> seats) {

		int available = 0;
		int held = 0;
		int confirmed = 0;

		for (Seat seat : seats) {
			switch (seat.getStatus()) {
			case AVAILABLE -> available++;
			case HELD -> held++;
			case CONFIRMED -> confirmed++;
			}
		}

		List<SeatResponse> seatResponses = seats.stream()
				.map(seat -> new SeatResponse(seat.getSeatNumber(), seat.getStatus().name().toLowerCase())).toList();

		return new ShowResponse(show.getId(), show.getName(), show.getPricePaise(), seats.size(), available, held,
				confirmed, seatResponses);
	}
}