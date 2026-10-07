package com.ashray.seatreservation.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	private static final String REQUEST_ID_HEADER = "X-Request-ID";
	private static final String MDC_REQUEST_ID = "requestId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestId = request.getHeader(REQUEST_ID_HEADER);

		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}

		MDC.put(MDC_REQUEST_ID, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		long start = System.currentTimeMillis();

		try {
			filterChain.doFilter(request, response);
		} finally {
			long duration = System.currentTimeMillis() - start;
			log.info("HTTP request completed method={} path={} status={} duration_ms={}", request.getMethod(),
					request.getRequestURI(), response.getStatus(), duration);
			MDC.remove(MDC_REQUEST_ID);
		}
	}
}