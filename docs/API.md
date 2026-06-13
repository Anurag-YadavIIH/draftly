# Draftly — API Reference

Base URL: `http://localhost:8080`
Interactive docs (recommended): **`/swagger-ui.html`**

All endpoints operate as a single demo user (`demo.user@draftly.app`), so no
auth header is needed in demo mode.

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

## Auth (Gmail OAuth2) — `/api/auth`

> Only needed in **real** mode (`draftly.gmail-mode=real`). In demo mode the
> inbox is mocked and no Google account is required.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/auth/gmail/login` | Returns the Google consent URL |
| GET | `/api/auth/gmail/callback` | OAuth redirect target; exchanges `code` for tokens |
| GET | `/api/auth/status` | Reports mode + whether Gmail is connected |
| POST | `/api/auth/logout` | Revoke and delete stored tokens |

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
