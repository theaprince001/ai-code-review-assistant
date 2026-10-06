package com.prince.codereview.job;

import com.prince.codereview.model.JobStatus;
import com.prince.codereview.model.ReviewJob;
import com.prince.codereview.repository.ReviewJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewJobWorker {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration STALE_LOCK_TIMEOUT = Duration.ofMinutes(10);

    private final ReviewJobRepository reviewJobRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 10_000)
    public void processNextJob() {
        ReviewJob job = claimNextJob();
        if (job == null) return;

        try {
            doWork(job);
            markCompleted(job);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Job {} was reclaimed by recovery while we were processing it — "
                    + "discarding our result to avoid clobbering the new worker's outcome", job.getId());
        } catch (Exception e) {
            log.error("Job {} failed on attempt {}: {}", job.getId(), job.getAttempts(), e.getMessage(), e);
            markFailedOrRetry(job, e.getMessage());
        }
    }

    // ---- Transaction 1: claim (short, commits immediately) ----
    private ReviewJob claimNextJob() {
        return transactionTemplate.execute(status -> {
            Optional<ReviewJob> opt = reviewJobRepository.findNextPendingJob();
            if (opt.isEmpty()) return null;

            ReviewJob job = opt.get();
            job.setStatus(JobStatus.PROCESSING);
            job.setLockedAt(LocalDateTime.now());
            job.setStartedAt(LocalDateTime.now());
            job.setAttempts(job.getAttempts() + 1);
            ReviewJob saved = reviewJobRepository.save(job);

            log.info("Claimed job {} (attempt {}/{})",
                    saved.getId(), saved.getAttempts(), MAX_ATTEMPTS);
            return saved;
        });
    }

    // ---- No transaction: the actual work ----
    private void doWork(ReviewJob job) throws InterruptedException {
        // Placeholder. Sprint 2 replaces this with: fetch diff → static analysis → AI → comments.
        Thread.sleep(5_000);
    }

    // ---- Transaction 2a: complete ----
    private void markCompleted(ReviewJob job) {
        transactionTemplate.executeWithoutResult(status -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setLockedAt(null);
            reviewJobRepository.save(job);
        });
        log.info("Job {} completed", job.getId());
    }

    // ---- Transaction 2b: fail or requeue ----
    private void markFailedOrRetry(ReviewJob job, String error) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                job.setLockedAt(null);
                job.setLastError(error == null ? "unknown"
                        : error.substring(0, Math.min(error.length(), 4000)));

                if (job.getAttempts() >= MAX_ATTEMPTS) {
                    job.setStatus(JobStatus.FAILED);
                    log.warn("Job {} exceeded max attempts, marked FAILED", job.getId());
                } else {
                    job.setStatus(JobStatus.PENDING);
                    log.info("Job {} requeued (attempt {}/{})",
                            job.getId(), job.getAttempts(), MAX_ATTEMPTS);
                }
                reviewJobRepository.save(job);
            });
        } catch (OptimisticLockingFailureException e) {
            log.warn("Job {} was reclaimed by recovery — skipping retry/FAILED update; "
                    + "the new worker owns the outcome", job.getId());
        }
    }

    // ---- Recovery pass ----
    @Scheduled(fixedDelay = 60_000)
    public void recoverStaleJobs() {
        LocalDateTime timeout = LocalDateTime.now().minus(STALE_LOCK_TIMEOUT);
        Integer recovered = transactionTemplate.execute(status ->
                reviewJobRepository.recoverStaleJobs(timeout, MAX_ATTEMPTS));
        if (recovered != null && recovered > 0) {
            log.warn("Recovery pass reset {} stale PROCESSING jobs", recovered);
        }
    }
}