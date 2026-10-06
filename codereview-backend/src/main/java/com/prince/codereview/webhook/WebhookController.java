package com.prince.codereview.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prince.codereview.model.Repository;
import com.prince.codereview.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final RepositoryRepository repositoryRepository;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{repositoryId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable Long repositoryId,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {

        Optional<Repository> repoOpt = repositoryRepository.findById(repositoryId);
        if (repoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Repository not found");
        }

        if (!signatureVerifier.verifySignature(payload, signature, repoOpt.get().getWebhookSecret())) {
            log.warn("Invalid signature for repository {}", repositoryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("Event ignored");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Malformed JSON");
        }

        String action = root.path("action").asText();
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            return ResponseEntity.ok("Action ignored");
        }

        JsonNode prNode = root.get("pull_request");
        if (!isValidPrPayload(prNode)) {
            log.warn("Malformed PR payload for delivery {}", deliveryId);
            return ResponseEntity.badRequest().body("Missing required pull_request fields");
        }

        WebhookService.Result result = webhookService.createJobIfAbsent(repositoryId, deliveryId, prNode);
        return result == WebhookService.Result.DUPLICATE
                ? ResponseEntity.ok("Duplicate ignored")
                : ResponseEntity.ok("Job created");
    }

    private boolean isValidPrPayload(JsonNode pr) {
        if (pr == null || pr.isNull() || pr.isMissingNode()) return false;
        if (!pr.hasNonNull("number") || !pr.get("number").isInt()) return false;
        if (!pr.hasNonNull("title") || pr.get("title").asText().isBlank()) return false;
        JsonNode user = pr.get("user");
        if (user == null || !user.hasNonNull("login") || user.get("login").asText().isBlank()) return false;
        JsonNode head = pr.get("head");
        if (head == null || !head.hasNonNull("ref") || head.get("ref").asText().isBlank()) return false;
        return true;
    }
}