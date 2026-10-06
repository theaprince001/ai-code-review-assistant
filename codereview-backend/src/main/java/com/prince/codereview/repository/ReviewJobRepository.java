package com.prince.codereview.repository;

import com.prince.codereview.model.ReviewJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReviewJobRepository extends JpaRepository<ReviewJob, Long> {

    Optional<ReviewJob> findByDeliveryId(String deliveryId);

    /** Locks the next PENDING job in a short transaction. */
    @Query(value = """
        SELECT * FROM review_jobs
        WHERE status = 'PENDING'
        ORDER BY created_at ASC
        FOR UPDATE SKIP LOCKED
        LIMIT 1
        """, nativeQuery = true)
    Optional<ReviewJob> findNextPendingJob();

    /**
     * Atomic idempotent insert — eliminates the check-then-insert race.
     * Returns 1 if inserted, 0 if delivery_id already existed.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO review_jobs (pull_request_id, delivery_id, status, attempts, created_at, version)
        VALUES (:pullRequestId, :deliveryId, 'PENDING', 0, NOW(), 0)
        ON CONFLICT (delivery_id) DO NOTHING
        """, nativeQuery = true)
    int insertJobIfAbsent(@Param("pullRequestId") Long pullRequestId,
                          @Param("deliveryId") String deliveryId);

    /**
     * Recovery pass. Stale PROCESSING jobs go PENDING — unless attempts have
     * already hit the ceiling, in which case they go straight to FAILED.
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE review_jobs
        SET status = CASE WHEN attempts >= :maxAttempts THEN 'FAILED' ELSE 'PENDING' END,
            locked_at = NULL,
            last_error = CASE
                WHEN attempts >= :maxAttempts
                    THEN 'Abandoned: exceeded max attempts (' || attempts || ')'
                ELSE last_error
            END,
            version = version + 1
        WHERE status = 'PROCESSING'
          AND locked_at < :timeout
        """, nativeQuery = true)
    int recoverStaleJobs(@Param("timeout") LocalDateTime timeout,
                         @Param("maxAttempts") int maxAttempts);
}