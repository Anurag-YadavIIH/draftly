# Draftly — High-Level Design (HLD)

## 1. Problem & Goal

Replying to email is repetitive. **Draftly** is a backend service that reads your
inbox, drafts replies in *your* writing style using an LLM, lets you review
(approve / edit / reject), and then sends the approved reply on the correct
thread — safely, idempotently, and with automatic retries.

It is built as a **capstone** to demonstrate the backend engineering concepts from
the course: REST API design, databases, layered/clean architecture, LLD/HLD,
and AI concepts (**RAG** and **MCP-style tools**).

## 2. Scope

**In scope**
- Fetch inbox emails (Gmail API + OAuth2, or a mock inbox for demos)
- Generate AI reply drafts in multiple tones (formal / concise / friendly)
- Learn the user's style from past sent emails (RAG)
- Review workflow: approve / edit / reject before anything is sent
- Send approved replies while preserving thread integrity (`In-Reply-To` / `threadId`)
- Persist drafts and a full send history with status
- Idempotent sends + automatic retry + notification on persistent failure
- Encrypt OAuth tokens at rest; support logout / token revocation

**Out of scope (intentionally, for a beginner-friendly capstone)**
- Multi-user accounts / auth UI (the demo uses a single fixed user)
- A frontend (the API + Swagger UI is the deliverable)
- Production-grade vector database (a simple in-Java cosine search is used instead)

## 3. Architecture Overview

A classic **layered architecture**:

```
Client (Swagger/curl)
        │
   Controllers      → thin HTTP layer, validation, DTO mapping
        │
   Services         → business logic & orchestration
        │
   Integration      → MCP-style tools (Gmail, LLM), RAG, Token/Encryption
        │
   Persistence      → Spring Data JPA repositories → H2 / PostgreSQL
        │
   External APIs    → Gmail API, Google OAuth2, Anthropic API
```

See `docs/architecture.png` for the full diagram.

### Why "MCP-style tools"?
The course covered **MCP (Model Context Protocol)** — the idea of giving a model
well-defined *tools* it can call. Draftly mirrors that pattern in its own code:
Gmail and the LLM each sit behind a small **tool interface** (`GmailTool`,
`LlmClient`) with interchangeable **mock** and **real** implementations chosen at
runtime. This keeps the core logic independent of any vendor and makes the whole
system runnable offline for the demo.

## 4. Key Flows

### 4.1 Generate a draft (RAG + LLM)
1. `POST /api/drafts` with `emailId` and a `tone`.
2. `DraftService` loads the email and the user's preferences (signature, default tone).
3. **RAG retrieval**: `StyleRetriever` embeds the incoming email and finds the
   top-K most similar *past sent emails* by cosine similarity.
4. Those style samples + tone + signature are packed into a prompt and sent to the
   `LlmClient`, which returns a reply body.
5. The draft is saved with status `SUGGESTED`.

### 4.2 Review
- `approve` → `APPROVED`, `edit` → `EDITED` (both sendable), `reject` → `REJECTED`
  (terminal). Already-sent or rejected drafts cannot be re-reviewed (guarded).

### 4.3 Send (idempotent + retry)
1. `POST /api/drafts/{id}/send` with an optional `idempotencyKey`
   (defaults to `draft-{id}`).
2. `SendService` checks the `sent_log` by idempotency key — if it is already
   `SENT`, it returns the previous result and does **not** send again.
3. Otherwise it calls `GmailTool.sendReply(...)`, preserving the thread via the
   original message id / `threadId`.
4. **Success** → draft + log marked `SENT`, and the reply is **indexed back into
   the style store** so future drafts learn from it (the learning loop).
5. **Failure** → attempts incremented, log marked `FAILED`, error stored. The
   `RetryScheduler` (`@Scheduled`, every 15s) retries up to `maxSendRetries`.
   When attempts are exhausted, `NotificationService` raises a user-facing alert
   (e.g. "Gmail token expired — please reconnect").

## 5. Data Model

| Table | Purpose | Notable columns |
|------|---------|-----------------|
| `email_message` | stored inbox emails | `gmail_message_id` (UNIQUE → dedupe) |
| `draft` | generated replies | `status`, `tone`, `content` |
| `sent_log` | send history / audit | `idempotency_key` (UNIQUE), `attempts`, `last_error` |
| `user_preference` | signature & default tone | `user_email` (UNIQUE) |
| `style_sample` | RAG corpus | `text`, `embedding` (CSV) |
| `oauth_token` | Gmail tokens | `access_token_enc`, `refresh_token_enc` (AES-GCM) |

**Draft status lifecycle:** `SUGGESTED → APPROVED | EDITED → SENT`, with
`REJECTED` and `FAILED` as the other outcomes.

## 6. Reliability, Security & Idempotency

- **Idempotency**: a `UNIQUE` constraint on `sent_log.idempotency_key` plus a
  pre-send check means retrying the same send never produces a duplicate email.
- **Retries**: transient send failures are retried by a scheduler with a capped
  attempt count, then escalated to a notification.
- **Security**: OAuth tokens are encrypted at rest with **AES/GCM**; access tokens
  are auto-refreshed; logout revokes and deletes them. Only the `gmail.modify`
  scope is requested.
- **Quota friendliness**: emails are de-duplicated on fetch so we never reprocess
  the same message.

## 7. Technology Choices

| Concern | Choice | Why |
|--------|--------|-----|
| Language / framework | Java 17, Spring Boot 3.3 | Course stack; mature, well-documented |
| Persistence | Spring Data JPA, H2 (demo) / PostgreSQL (prod) | Zero-setup demo, real DB for prod |
| LLM | Anthropic API, with a deterministic mock | Mock = offline demo & tests |
| Embeddings / RAG | Hashing embedder + cosine search in Java | No external service; shows the pipeline clearly |
| API docs | springdoc-openapi (Swagger UI) | Interactive demo surface |
| Packaging | Docker (multi-stage) + docker-compose | One-command run for reviewers |

## 8. Future Improvements
- Real embedding model + a vector store (pgvector) behind the same interface.
- Multi-user accounts and per-user OAuth.
- Webhook/push (Gmail `watch`) instead of manual fetch.
- A small web frontend on top of the existing API.
