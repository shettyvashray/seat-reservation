package com.ashray.seatreservation.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RequestHashService {

	public String hashSeats(List<String> seats) {
		String canonicalRequest = seats.stream().sorted().distinct().reduce((a, b) -> a + "," + b).orElse("");

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

			StringBuilder result = new StringBuilder();
			for (byte b : hash) {
				result.append(String.format("%02x", b));
			}

			return result.toString();

		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}
}