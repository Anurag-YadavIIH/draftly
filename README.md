# Draftly — Gmail AI Reply Agent

> A backend AI assistant that reads your inbox, drafts replies **in your own
> writing style**, lets you review them, and sends them safely — built with
> Java & Spring Boot.

This is my capstone project for the **Airtribe Backend AI Engineering** course.
It brings together the core topics from the program: REST API design, databases,
layered architecture, LLD/HLD, and AI (**RAG** + **MCP** concepts).

![Architecture](docs/architecture.png)

---

## ✨ What it does

- **Fetches** inbox emails (Gmail API + OAuth2, or a built-in mock inbox).
- **Generates** AI reply drafts in multiple tones — *formal*, *concise*, *friendly*.
- **Learns your style** from past sent emails using **RAG** (retrieval-augmented generation).
- **Human-in-the-loop review**: approve, edit, or reject before anything is sent.
- **Sends** approved replies on the correct thread (`In-Reply-To` / `threadId`).
- **Logs** every draft and send with a status, as an audit trail.
- **Reliable sending**: idempotent (no duplicate emails) + automatic retries +
  a notification when sending keeps failing (e.g. an expired token).
- **Secure**: OAuth tokens are **encrypted at rest** (AES-GCM); logout revokes them.

---

## 🚀 Quick start

Draftly runs in **real mode by default**: real Gmail (OAuth2), a real LLM
(OpenAI by default), and PostgreSQL via docker-compose.

### Option A — Real mode with Docker (recommended)
```bash
cp .env.example .env      # then fill in OPENAI_API_KEY + your Google OAuth creds
docker compose up --build
```
This starts PostgreSQL and the app together. Then:
1. Open **http://localhost:8080/swagger-ui.html**
2. Connect Gmail **once**: call `GET /api/auth/gmail/login`, open the returned
   URL, approve consent, and you'll be redirected back as "connected".
3. Run the flow below.

> See the "Google Cloud setup" and "Environment variables" sections for exactly
> what to put in `.env`.

### Option B — Mock mode (no accounts, zero setup)
Don't have credentials yet? Run everything against a fake inbox + mock LLM + an
in-memory H2 database — no Google account, no API key:

```bash
# Local:
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock      # macOS / Linux
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mock"  # Windows

# Or with Docker: add SPRING_PROFILES_ACTIVE=mock to your .env, then:
docker compose up --build
```

> Requires Java 17+. The `./mvnw` wrapper downloads Maven automatically the first
> time. To build a jar: `./mvnw clean package` then `java -jar target/*.jar`.

---

## 🎬 Try the full flow in 6 calls

Use Swagger UI (easiest) or curl. In **real mode**, first connect Gmail (step 2
above); in **mock mode** the inbox is pre-seeded.

```bash
# 1. Fetch the inbox (real Gmail, or the fake inbox in mock mode)
curl -X POST "http://localhost:8080/api/emails/fetch?max=10"

# 2. See the emails
curl "http://localhost:8080/api/emails"

# 3. Generate a friendly reply to email #1 (RAG + LLM happen here)
curl -X POST "http://localhost:8080/api/drafts" \
  -H "Content-Type: application/json" \
  -d '{"emailId": 1, "tone": "friendly"}'

# 4. Approve the draft
curl -X POST "http://localhost:8080/api/drafts/1/approve"

# 5. Send it (mock mode tip: add ?simulateFailures=2 to demo retry + idempotency)
curl -X POST "http://localhost:8080/api/drafts/1/send"

# 6. Check status
curl "http://localhost:8080/api/drafts/1"
```

Full endpoint reference: [`docs/API.md`](docs/API.md).

---

## 🧠 How the AI parts work

### RAG — learning your writing style
1. On startup, a few of "my" past sent emails are indexed as **style samples**.
2. When you generate a reply, Draftly **embeds** the incoming email and retrieves
   the **top-K most similar** past emails by **cosine similarity**.
3. Those samples + the chosen tone + your signature are built into a prompt for
   the LLM, so the reply sounds like you.
4. **Learning loop**: every successfully sent reply is indexed back into the style
   store, so the system keeps improving.

> The embedder uses a simple, dependency-free "hashing trick" so RAG works offline
> in the demo. It sits behind an `EmbeddingClient` interface — in production you'd
> swap in a real embedding model without changing anything else.

### MCP-style tools
The course covered **MCP (Model Context Protocol)** — giving a model well-defined
*tools*. Draftly mirrors that idea: **Gmail** and the **LLM** each sit behind a
small tool interface (`GmailTool`, `LlmClient`), selected at runtime by config.
Gmail has `mock` and `real` implementations; the LLM has **three** interchangeable
providers — **`openai`** (default, `gpt-4o-mini`), **`anthropic`**, and **`mock`**.
The business logic never depends on a specific vendor, so adding a provider is just
one new class plus a config value.

---

## 🏗️ Architecture & design

A layered Spring Boot application:

| Layer | Responsibility |
|-------|----------------|
| **Controllers** | REST endpoints, validation, DTO mapping |
| **Services** | Business logic: drafting, review, sending, retries |
| **Integration** | MCP-style tools (Gmail, LLM), RAG, token encryption |
| **Persistence** | Spring Data JPA → PostgreSQL (default) / H2 (mock mode) |

The full write-up is in [`docs/HLD.md`](docs/HLD.md).

**Draft status lifecycle:**
`SUGGESTED → APPROVED | EDITED → SENT`, with `REJECTED` and `FAILED` as the
other outcomes.

---

## 🔁 Reliability & security highlights

- **Idempotency** — a `UNIQUE` idempotency key in `sent_log` plus a pre-send check
  means retrying a send never creates a duplicate email.
- **Retries** — a `@Scheduled` job retries failed sends every 15s up to a capped
  number of attempts, then raises a notification.
- **Token encryption** — OAuth tokens stored with **AES/GCM**; access tokens
  auto-refresh; logout revokes them.
- **Quota-friendly** — emails are de-duplicated on fetch via the Gmail message id.

---

## 🛠️ Tech stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · H2 / PostgreSQL ·
springdoc-openapi (Swagger UI) · Docker · JUnit 5.

---

## ⚙️ Configuration

Real mode is the default. Copy `.env.example` to `.env` and fill in the values
below (compose auto-loads `.env`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `DRAFTLY_GMAIL_MODE` | `real` | `real` or `mock` Gmail |
| `DRAFTLY_LLM_PROVIDER` | `openai` | `openai`, `anthropic`, or `mock` |
| `OPENAI_API_KEY` | — | **required** when provider is `openai` |
| `OPENAI_MODEL` | `gpt-4o-mini` | OpenAI model to use |
| `ANTHROPIC_API_KEY` | — | needed when provider is `anthropic` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | **required** for real Gmail |
| `GOOGLE_REDIRECT_URI` | `…/api/auth/gmail/callback` | must match the Console |
| `DRAFTLY_ENCRYPTION_KEY` | demo key | **override in production** (Base64, 16/24/32 bytes) |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `draftly` | database credentials |
| `SPRING_PROFILES_ACTIVE` | — | set to `mock` for the zero-setup fallback |

**Switching to mock mode:** set `SPRING_PROFILES_ACTIVE=mock` (this swaps in H2 +
the fake inbox + the mock LLM, so no Postgres, Google account, or API key is
needed). See the Quick start above.

---

## 🧪 Tests

```bash
./mvnw test
```
Includes unit tests for the tone-aware draft generator and the RAG retrieval math
(deterministic, no network).

---

## 📁 Project structure

```
draftly/
├── src/main/java/com/airtribe/draftly/
│   ├── controller/   REST endpoints
│   ├── service/      business logic
│   │   ├── gmail/    GmailTool (mock / real)        ← MCP-style tool
│   │   ├── llm/      LlmClient (openai / anthropic / mock) ← MCP-style tool
│   │   ├── rag/      embeddings + cosine retrieval   ← RAG
│   │   └── auth/     token storage + AES encryption
│   ├── domain/       JPA entities
│   ├── repository/   Spring Data repositories
│   ├── dto/          request/response records
│   ├── exception/    error handling
│   ├── scheduler/    retry job
│   └── config/       properties, seeding, OpenAPI
├── docs/             HLD, API reference, demo script, architecture diagram
├── Dockerfile        multi-stage build
└── docker-compose.yml
```

---

## 📹 Demo video

A step-by-step script (with narration and exact clicks) for the under-5-minute
video is in [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md).

---

## 📝 Notes & limitations

This is a learning capstone, so some things are intentionally simplified: a single
fixed demo user (no multi-user auth), an in-Java cosine search instead of a vector
database, and a hashing embedder instead of a hosted embedding model. Each of
these sits behind an interface, so they can be upgraded without touching the core
logic.
