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
- A frontend (the API + Swagger UI is the deliverable)

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
3. **RAG retrieval**: `StyleRetriever` embeds the incoming email (OpenAI
   `text-embedding-3-small`, with a deterministic hashing embedder for the mock
   profile) and finds the top-K most similar *past sent emails* via a pgvector
   HNSW cosine-distance index (or in-Java cosine search in the mock profile).
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
| `style_sample` | RAG corpus | `text`, `embedding` (CSV), `embedding_vec` (pgvector) |
| `oauth_token` | Gmail tokens | `access_token_enc`, `refresh_token_enc` (AES-GCM) |
| `app_user` | login credentials | `email` (UNIQUE), `password_hash` (BCrypt) |

**Draft status lifecycle:** `SUGGESTED → APPROVED | EDITED → SENT`, with
`REJECTED` and `FAILED` as the other outcomes.

## 6. Authentication & Authorization

Draftly is multi-tenant: every user has their own inbox, drafts, preferences,
style corpus and Gmail connection, isolated by `user_email`.

- **Accounts**: `POST /api/auth/register` creates an `app_user` row (email +
  BCrypt password hash). `POST /api/auth/login` verifies credentials via
  Spring Security's `DaoAuthenticationProvider` and returns a signed JWT to
  send as `Authorization: Bearer <token>`.
- **Stateless sessions**: a `JwtAuthFilter` validates the bearer token on every
  request and populates the security context with the caller's email — no
  server-side session state. Every endpoint except `/api/auth/register`,
  `/api/auth/login`, the Gmail OAuth callback, and Swagger/H2-console requires
  a valid token.
- **Per-user data isolation**: every entity (`email_message`, `draft`,
  `user_preference`, `style_sample`, `oauth_token`) is keyed by `user_email`.
  Controllers resolve the caller's email from the JWT and scope all
  queries/writes to it. Fetching another user's draft or email by id returns
  `404` (not `403`), so existence isn't leaked to non-owners.
- **Gmail OAuth "state" problem**: Google's OAuth callback
  (`/api/auth/gmail/callback`) is an unauthenticated browser redirect with no
  `Authorization` header, so the app can't tell which Draftly user it belongs
  to from the request alone. `GET /api/auth/gmail/login` solves this by
  minting a short-lived (10 min), purpose-scoped JWT containing the caller's
  email and passing it as the OAuth `state` parameter; the callback decodes it
  to know whose Gmail tokens to store.
- **Demo account**: `demo.user@draftly.app` / `demo1234` is seeded on first
  startup so the existing demo flow (preferences + style samples) works out of
  the box — or register a new account via `/api/auth/register`.

## 7. Reliability, Security & Idempotency

- **Idempotency**: a `UNIQUE` constraint on `sent_log.idempotency_key` plus a
  pre-send check means retrying the same send never produces a duplicate email.
- **Retries**: transient send failures are retried by a scheduler with a capped
  attempt count, then escalated to a notification.
- **Security**: OAuth tokens are encrypted at rest with **AES/GCM**; access tokens
  are auto-refreshed; logout revokes and deletes them. Only the `gmail.modify`
  scope is requested.
- **Quota friendliness**: emails are de-duplicated on fetch so we never reprocess
  the same message.

## 8. Technology Choices

| Concern | Choice | Why |
|--------|--------|-----|
| Language / framework | Java 17, Spring Boot 3.3 | Course stack; mature, well-documented |
| Persistence | Spring Data JPA, H2 (demo) / PostgreSQL (prod) | Zero-setup demo, real DB for prod |
| LLM | Anthropic API, with a deterministic mock | Mock = offline demo & tests |
| Embeddings / RAG | OpenAI `text-embedding-3-small` + pgvector HNSW search (hashing embedder + in-Java cosine in the mock profile) | Real semantic search in real mode; zero external deps for offline demo/tests |
| Auth | Spring Security + JWT (jjwt), BCrypt password hashing | Stateless, standard, easy to demo via Swagger's "Authorize" button |
| API docs | springdoc-openapi (Swagger UI) | Interactive demo surface |
| Packaging | Docker (multi-stage) + docker-compose | One-command run for reviewers |

## 9. Future Improvements

- Webhook/push (Gmail `watch`) instead of manual fetch.
- A small web frontend on top of the existing API.
