# Draftly — Demo Video Script (target: under 5 minutes)

This is a word-for-word guide for your screen-recording. Recommended tools:
**OBS Studio** or **Loom** (free). Speak naturally — the lines below are a guide,
not a script to read robotically.

**Before you hit record:**

1. Start the backend: `docker compose up --build` (wait for "Started DraftlyApplication").
2. Start the frontend: `cd frontend && npm install && npm run dev`.
3. Open `http://localhost:5173` in your browser (the React app). Also open
   `http://localhost:8080/swagger-ui.html` in a second tab for the reliability segment.
4. Have this script on a second screen / phone.
5. Close noisy apps; do a 10-second test recording to check mic + screen.

---

### [0:00–0:25] Intro — what & why

> "Hi, I'm <your name>. This is **Draftly**, my capstone for the Airtribe Backend
> AI Engineering course — a full-stack **Gmail AI reply agent**. It's a Spring
> Boot API with a **React frontend**, **multi-user accounts secured with JWT**,
> and it uses the **RAG** and **MCP** concepts from the course to draft replies
> in my own writing style. Let me show the architecture, then a live demo."

*(Show `docs/architecture.png` on screen for ~8 seconds.)*

### [0:25–0:50] Architecture (show the diagram)

> "It's a layered Spring Boot API behind a stateless **JWT** security filter.
> Controllers expose REST endpoints, services hold the business logic, and
> below that are **MCP-style tools** — Gmail and the LLM each sit behind an
> interface with mock and real implementations. **RAG** retrieves my past sent
> emails by embedding similarity using **pgvector**, and a retry scheduler
> handles failed sends. On top of all that is a **React frontend** for the
> whole review workflow."

### [0:50–1:25] Log in (the frontend)

*(Switch to the browser at `http://localhost:5173` — the "Sign in to Draftly" page.)*

> "Every account here is fully isolated — your inbox, drafts, style history and
> Gmail connection are all your own. I'll log in with the seeded demo account."

- The email field is pre-filled with `demo.user@draftly.app`; type the password
  `demo1234` → click **Sign in**.

> "That hits `POST /api/auth/login`, which returns a JWT. The frontend stores
> it and sends it as a Bearer token on every request from here on — and I could
> just as easily click 'Create one' to register a brand-new account with its
> own empty inbox."

### [1:25–2:10] Inbox — fetch & generate (RAG + LLM)

*(Land on the Inbox page.)*

> "This is the inbox. I'll fetch new emails — Draftly pulls from Gmail (or a
> mock inbox in mock mode), de-duplicating by message id so nothing's ever
> processed twice."

- Click **Fetch new emails**.

> "Now let's generate an AI reply for one of these."

- Click **Generate draft** (the sparkles button) on an email.

> "Behind the scenes, **RAG** embeds this email, finds the most similar emails
> I've sent before, and feeds my writing style plus my preferred tone — set on
> the Preferences page — into the LLM, OpenAI by default. That takes us
> straight to the draft."

### [2:10–2:50] Review the draft

*(Land on the draft detail page.)*

> "Here's the original email alongside the generated reply, tagged with its
> tone, with my signature already appended. Nothing gets sent without my
> approval — I can edit this text directly, save changes, or reject it. I'll
> approve it."

- Click **Approve**.

> "The status is now **APPROVED**, which makes it sendable."

### [2:50–3:15] Send via Gmail

- Click **Send via Gmail** → confirm the dialog.

> "Sending calls `POST /api/drafts/{id}/send`, which preserves the email
> thread via `In-Reply-To` and `threadId` — and the result comes right back as
> **SENT**."

### [3:15–4:15] Reliability: retry + idempotency

*(Switch to Swagger UI at `http://localhost:8080/swagger-ui.html` — this is a
demo-only failure switch the frontend doesn't expose.)*

> "One thing the UI doesn't expose is a demo-only failure simulator, so let me
> switch to Swagger to show the reliability story. I'll log in here too to get
> a token for the 'Authorize' button."

- `POST /api/auth/login` with the demo credentials → copy the `token` from the
  response → click **Authorize** → paste `Bearer <token>`.
- Generate and approve another draft the same way as before, then:
- `POST /api/drafts/{id}/send` with `simulateFailures` = `2` → **Execute**.

> "This tells Draftly to fail the first two send attempts — like a flaky
> network or an expired token. It comes back **FAILED** with an attempt count."

- Wait ~15–30 seconds (the scheduler runs every 15s), then **GET `/api/drafts/{id}`**.

> "A background **retry scheduler** automatically retries failed sends. After a
> couple of retries it's now **SENT**."

- Run `POST /api/drafts/{id}/send` again, with no params.

> "And sending the same draft again is **idempotent** — it returns the original
> result and does *not* send a duplicate email, enforced by a unique idempotency
> key in the database."

### [4:15–4:45] Wrap up

> "To recap: Draftly is a full-stack, multi-user AI reply agent — a React
> frontend, JWT auth with BCrypt passwords and AES-encrypted Gmail tokens,
> RAG-powered style-aware drafts, human-in-the-loop review, and reliable,
> idempotent sending with automatic retries. Everything's Dockerized — one
> command to run. The code, README and design docs are in the GitHub repo
> linked below. Thanks for watching!"

---

### Tips

- **Keep it under 5 minutes** — practice once first; the Swagger reliability
  segment is the easiest to trim or speed up.
- If you don't want to wait for the scheduler on camera, send with
  `simulateFailures=0` for an instant success and just *describe* the retry
  behaviour.
- To get a token into Swagger quickly: log in via `POST /api/auth/login` there
  too (same demo credentials), copy the `token` field, and paste
  `Bearer <token>` into the green **Authorize** button.
- Record at 1080p; zoom in the browser (Ctrl/Cmd +) so text — especially the
  JWT/token fields — is readable.
- Show your face in a small corner (Loom does this automatically) — it's
  friendlier.
