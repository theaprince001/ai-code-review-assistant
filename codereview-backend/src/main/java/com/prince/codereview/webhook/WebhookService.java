package com.prince.codereview.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.prince.codereview.model.PullRequest;
import com.prince.codereview.model.Repository;
import com.prince.codereview.repository.PullRequestRepository;
import com.prince.codereview.repository.RepositoryRepository;
import com.prince.codereview.repository.ReviewJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ReviewJobRepository reviewJobRepository;

    public enum Result { CREATED, DUPLICATE }

    @Transactional
    public Result createJobIfAbsent(Long repositoryId, String deliveryId, JsonNode prNode) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalStateException(
                        "Repository vanished mid-request: " + repositoryId));

        int prNumber = prNode.get("number").asInt();
        String title = prNode.get("title").asText();
        String author = prNode.get("user").get("login").asText();
        String branch = prNode.get("head").get("ref").asText();

        PullRequest pr = pullRequestRepository
                .findByRepositoryIdAndGithubPrNumber(repositoryId, prNumber)
                .orElseGet(() -> {
                    PullRequest p = new PullRequest();
                    p.setRepository(repository);
                    p.setGithubPrNumber(prNumber);
                    p.setStatus("OPEN");
                    return p;
                });
        pr.setTitle(title);
        pr.setAuthor(author);
        pr.setBranch(branch);
        pr = pullRequestRepository.save(pr);

        int inserted = reviewJobRepository.insertJobIfAbsent(pr.getId(), deliveryId);
        if (inserted == 0) {
            log.info("Duplicate delivery {} ignored", deliveryId);
            return Result.DUPLICATE;
        }
        log.info("Created job for delivery {} PR #{}", deliveryId, prNumber);
        return Result.CREATED;
    }
}