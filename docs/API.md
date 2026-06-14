# Draftly — API Reference

Base URL: `http://localhost:8080`
Interactive docs (recommended): **`/swagger-ui.html`**

Every endpoint below except `POST /api/auth/register`, `POST /api/auth/login`,
and `GET /api/auth/gmail/callback` requires `Authorization: Bearer <token>`,
obtained from `/api/auth/login` (or `/register`). A demo account is seeded on
first startup: `demo.user@draftly.app` / `demo1234`. See the
[`AuthController`](#auth--apiauth) section below for the auth endpoints.

---

## Emails — `/api/emails`

| Method | Path | Description | Params |
|--------|------|-------------|--------|
| POST | `/api/emails/fetch` | Fetch recent emails from Gmail (mock or real) and store new ones | `max` (query, default 10) |
| GET | `/api/emails` | List stored emails, newest first | — |
| GET | `/api/emails/{id}` | Get one email | `id` (path) |

**Example**
```bash
curl -X POST "http://localhost:8080/api/emails/fetch?max=10"
curl "http://localhost:8080/api/emails"
```

---

## Drafts — `/api/drafts`

| Method | Path | Description | Body / Params |
|--------|------|-------------|---------------|
| POST | `/api/drafts` | Generate an AI reply (RAG + LLM) | `{ "emailId": 1, "tone": "formal" }` |
| GET | `/api/drafts` | List all drafts, newest first | — |
| GET | `/api/drafts/{id}` | Get one draft | `id` (path) |
| POST | `/api/drafts/{id}/approve` | Approve → sendable | `id` (path) |
| PUT | `/api/drafts/{id}/edit` | Edit body, marks `EDITED` | `{ "content": "..." }` |
| POST | `/api/drafts/{id}/reject` | Reject (terminal) | `id` (path) |
| POST | `/api/drafts/{id}/send` | Send via Gmail (idempotent + retry) | `idempotencyKey` (query, optional), `simulateFailures` (query, default 0) |

`tone` accepts `formal`, `concise`, or `friendly`.
`simulateFailures=N` (demo only) makes the next N send attempts fail so you can
show the retry + idempotency behaviour.

**Example — full happy path**
```bash
# 1. generate
curl -X POST "http://localhost:8080/api/drafts" \
  -H "Content-Type: application/json" \
  -d '{"emailId": 1, "tone": "friendly"}'

# 2. approve (use the id returned above)
curl -X POST "http://localhost:8080/api/drafts/1/approve"

# 3. send
curl -X POST "http://localhost:8080/api/drafts/1/send"
```

**Example — show retry + idempotency**
```bash
# make the first 2 attempts fail; the scheduler retries automatically
curl -X POST "http://localhost:8080/api/drafts/1/send?simulateFailures=2"
# re-sending the same draft returns the same result, no duplicate email
curl -X POST "http://localhost:8080/api/drafts/1/send"
```

---

## Preferences — `/api/preferences`

| Method | Path | Description | Body |
|--------|------|-------------|------|
| GET | `/api/preferences` | Get signature & default tone | — |
| PUT | `/api/preferences` | Create/update preferences | `{ "signature": "...", "defaultTone": "formal" }` |

---

## Auth — `/api/auth`

| Method | Path | Auth required | Description | Body / Params |
|--------|------|---------------|-------------|---------------|
| POST | `/api/auth/register` | no | Create an account | `{ "email": "...", "password": "..." }` → `{ "token", "email" }` |
| POST | `/api/auth/login` | no | Log in | `{ "email": "...", "password": "..." }` → `{ "token", "email" }` |
| GET | `/api/auth/gmail/login` | yes | Returns a Google consent URL (with a signed `state` identifying you) | — |
| GET | `/api/auth/gmail/callback` | no (browser redirect) | OAuth redirect target; exchanges `code` for tokens using `state` to identify the user | `code`, `state` (query) |
| GET | `/api/auth/status` | yes | Reports mode + whether **your** Gmail is connected | — |
| POST | `/api/auth/logout` | yes | Revoke and delete your stored Gmail tokens | — |

> Gmail connect/status/logout are only meaningful in **real** mode
> (`draftly.gmail-mode=real`). In the mock profile the inbox is mocked and no
> Google account is required.

---

## Error format

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-06-13T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Draft 99 not found",
  "path": "/api/drafts/99"
}
```

| Status | When |
|--------|------|
| 400 | Validation failed (e.g. missing `emailId`) |
| 404 | Entity not found |
| 409 | Invalid state (e.g. sending a rejected draft) |
| 502 | Send failed after the attempt |
| 500 | Unexpected error |
