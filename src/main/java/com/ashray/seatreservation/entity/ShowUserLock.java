package com.ashray.seatreservation.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "show_user_locks")
@IdClass(ShowUserLock.ShowUserLockId.class)
public class ShowUserLock {

	@Id
	@Column(name = "show_id", nullable = false)
	private UUID showId;

	@Id
	@Column(name = "user_id", nullable = false)
	private String userId;

	public ShowUserLock() {
	}

	public ShowUserLock(UUID showId, String userId) {
		this.showId = showId;
		this.userId = userId;
	}

	public UUID getShowId() {
		return showId;
	}

	public void setShowId(UUID showId) {
		this.showId = showId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public static class ShowUserLockId implements Serializable {

		private UUID showId;
		private String userId;

		public ShowUserLockId() {
		}

		public ShowUserLockId(UUID showId, String userId) {
			this.showId = showId;
			this.userId = userId;
		}

		public UUID getShowId() {
			return showId;
		}

		public void setShowId(UUID showId) {
			this.showId = showId;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}
	}
}