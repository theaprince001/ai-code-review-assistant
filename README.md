# AI-Powered Code Review Assistant

An automated first-pass code reviewer that analyzes GitHub Pull Requests using static analysis and AI, then posts structured findings back to the PR. Built as a demonstration of durable, crash-resistant backend architecture on a ₹0 infrastructure budget.

**Status:** Sprint 1 complete — durable webhook ingestion pipeline working end-to-end.

---

## What It Does

When a developer opens or updates a Pull Request, GitHub sends a webhook. The system:

1. Verifies the webhook is genuinely from GitHub (HMAC-SHA256)
2. Deduplicates it (GitHub can and does redeliver)
3. Persists a durable review job to PostgreSQL
4. A background worker claims the job and processes it asynchronously
5. Posts the review back to the PR and stores it for the dashboard

Sprint 1 delivers steps 1–4. The AI review pipeline (step 5) lands in Sprint 2.

---

## Why the Architecture Looks Like This

Most webhook handlers look like this:

```java
@PostMapping("/webhook")
public void handle(@RequestBody Payload p) {
    // do slow work inline
    aiReview(p);          // 30+ seconds
    postComment(p);
}
