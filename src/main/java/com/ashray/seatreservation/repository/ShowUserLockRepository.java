package com.ashray.seatreservation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ashray.seatreservation.entity.ShowUserLock;

import jakarta.persistence.LockModeType;

public interface ShowUserLockRepository extends JpaRepository<ShowUserLock, ShowUserLock.ShowUserLockId> {

	@Modifying
	@Query(value = """
			INSERT INTO show_user_locks(show_id, user_id)
			VALUES (:showId, :userId)
			ON CONFLICT (show_id, user_id) DO NOTHING
			""", nativeQuery = true)
	void createIfAbsent(@Param("showId") UUID showId, @Param("userId") String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    SELECT s
			    FROM ShowUserLock s
			    WHERE s.showId = :showId
			      AND s.userId = :userId
			""")
	Optional<ShowUserLock> findForUpdate(@Param("showId") UUID showId, @Param("userId") String userId);
}