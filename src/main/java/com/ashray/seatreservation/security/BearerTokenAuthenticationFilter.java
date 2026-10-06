package com.ashray.seatreservation.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

	private final String adminToken;

	public BearerTokenAuthenticationFilter(@Value("${app.security.admin-token}") String adminToken) {
		this.adminToken = adminToken;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring("Bearer ".length()).trim();
		if (token.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		if (token.equals(adminToken)) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("admin", null,
					List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

			SecurityContextHolder.getContext().setAuthentication(authentication);
		} else {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(token, null,
					List.of(new SimpleGrantedAuthority("ROLE_USER")));

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}
}