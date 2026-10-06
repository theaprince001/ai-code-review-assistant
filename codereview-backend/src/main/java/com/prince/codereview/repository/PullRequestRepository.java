package com.prince.codereview.repository;

import com.prince.codereview.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByRepositoryIdAndGithubPrNumber(Long repositoryId, Integer prNumber);
}