# Draftly — Demo Video Script (target: under 5 minutes)

This is a word-for-word guide for your screen-recording. Recommended tools:
**OBS Studio** or **Loom** (free). Speak naturally — the lines below are a guide,
not a script to read robotically.

**Before you hit record:**
1. Start the app: `docker compose up --build` (wait for "Started DraftlyApplication").
2. Open `http://localhost:8080/swagger-ui.html` in your browser.
3. Have this script on a second screen / phone.
4. Close noisy apps; do a 10-second test recording to check mic + screen.

---

### [0:00–0:30] Intro — what & why
> "Hi, I'm <your name>. This is **Draftly**, my backend capstone for the
> Airtribe Backend AI Engineering course. It's a **Gmail AI reply agent**: it
> reads your inbox, drafts replies in your own writing style using an LLM, lets
> you review them, and then sends them safely. It's built with **Java and Spring
> Boot**, and it uses the **RAG** and **MCP** concepts from the course. Let me
> show the architecture, then a live demo."

*(Show `docs/architecture.png` on screen for ~10 seconds.)*

### [0:30–1:00] Architecture (show the diagram)
> "It's a clean **layered architecture**. Controllers expose REST APIs, services
> hold the logic, and below that I have **MCP-style tools** — Gmail and the LLM
> each sit behind an interface with a mock and a real version. There's a **RAG**
> component that learns my writing style, a database for drafts and send history,
> and a scheduler that retries failed sends. By default it runs fully offline in
> demo mode, so no Google login or API key is needed."

### [1:00–1:30] Fetch the inbox
*(Switch to Swagger UI.)*
> "Let me start the workflow. First I fetch the inbox."

- Expand **POST `/api/emails/fetch`** → **Try it out** → **Execute**.
> "These are mock emails seeded for the demo. You can see the sender, subject and
> body. Notice each has a Gmail message id — I de-duplicate on that so the same
> email is never processed twice."

### [1:30–2:30] Generate a draft (the AI + RAG part)
- Expand **POST `/api/drafts`** → **Try it out**.
- Body: `{ "emailId": 1, "tone": "friendly" }` → **Execute**.
> "Now the interesting part. I ask Draftly to generate a reply to email one, in a
> *friendly* tone. Behind the scenes it does **RAG** — it embeds this email,
> finds the most similar emails I've sent before, and feeds my writing style plus
> the tone into the LLM. Here's the generated draft, with my signature, and its
> status is **SUGGESTED**."

*(Optional, if time:)* Run it again with `"tone": "formal"`.
> "Same email, *formal* tone — and the draft changes accordingly. So tone is
> controllable."

### [2:30–3:15] Review → approve
> "Nothing gets sent without my approval. I can edit, reject, or approve."

- Expand **POST `/api/drafts/{id}/approve`** → id `1` → **Execute**.
> "I'll approve it. The status is now **APPROVED**, which means it's ready to send."

### [3:15–4:15] Send with retry + idempotency (the reliability part)
- Expand **POST `/api/drafts/{id}/send`** → id `1`, set `simulateFailures` = `2` → **Execute**.
> "When I send, I'm telling the demo to make the first two attempts fail — like a
> flaky network or an expired token. You can see it's marked **FAILED** with the
> attempt count."
- Wait ~15–30 seconds (the scheduler runs every 15s). Then **GET `/api/drafts/1`**.
> "A background **retry scheduler** automatically retries failed sends. After a
> couple of retries it succeeds — the status is now **SENT**."
- Run **POST `/api/drafts/1/send`** again (no failures).
> "And if I try to send the same draft again, it's **idempotent** — it returns
> the original result and does *not* send a duplicate email. That's enforced by a
> unique idempotency key in the database."

### [4:15–4:45] Wrap up
> "To recap: Draftly fetches email, generates style-aware AI drafts with RAG,
> keeps a human in the loop for review, and sends reliably with idempotency and
> retries. Tokens are encrypted at rest, and the whole thing is Dockerized — one
> command to run. The code, README and design docs are in the GitHub repo linked
> below. Thanks for watching!"

---

### Tips
- **Keep it under 5 minutes** — practice once first; trim the second-tone demo if tight.
- If you don't want to wait for the scheduler on camera, you can send with
  `simulateFailures=0` for a clean instant success, and just *explain* the retry.
- Record at 1080p; make the browser font larger (Ctrl/Cmd +) so text is readable.
- Show your face in a small corner (Loom does this automatically) — it's friendlier.
