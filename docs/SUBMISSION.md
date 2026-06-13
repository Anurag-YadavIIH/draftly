# How to Submit — GitHub repo + PR link

Airtribe asks for a **GitHub PR link**, a **video**, and an **external link**.
Here is the simplest way to produce all three. You only need a free GitHub
account and Git installed.

---

## Part 1 — Put the code on GitHub and open a Pull Request

The trick reviewers use: create the repo, push an empty `main`, then push your
work on a **branch** and open a PR from that branch into `main`. The PR link is
what you submit.

```bash
# 0. Unzip the project and open a terminal inside the draftly/ folder
cd draftly

# 1. Start a git repo
git init
git add .
git commit -m "Draftly: Gmail AI reply agent (capstone)"

# 2. Create an EMPTY repo on github.com named "draftly" (no README/gitignore),
#    then connect it. Replace <your-username>:
git remote add origin https://github.com/<your-username>/draftly.git
git branch -M main
git push -u origin main
```

Now create a branch so you can open a PR:

```bash
# 3. Make a feature branch with the full project
git checkout -b feature/draftly-capstone
git push -u origin feature/draftly-capstone
```

Then on GitHub:
1. Open your repo → click **"Compare & pull request"** (it appears after the push).
2. Base = `main`, compare = `feature/draftly-capstone`.
3. Title it `Draftly capstone` and paste a short description (you can reuse the
   README intro).
4. Click **Create pull request**.
5. **Copy the PR URL** (looks like `https://github.com/<you>/draftly/pull/1`).
   That is your **GitHub PR link**.

> Tip: leave the PR open (don't merge) so reviewers can comment on it.

---

## Part 2 — Record the video (External / Video link)

1. Follow [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md) — it's timed for under 5 minutes.
2. Record with **Loom** (gives you a shareable link instantly) or **OBS**
   (records a file you upload to YouTube/Drive as *unlisted*).
3. The shareable video URL is your **Video / External link**.

> If using Google Drive/YouTube, set sharing to "anyone with the link can view".

---

## Part 3 — What to paste into the submission form

| Field | What to paste |
|-------|---------------|
| GitHub PR link | `https://github.com/<you>/draftly/pull/1` |
| Video | your Loom/YouTube/Drive link |
| External link | the repo URL `https://github.com/<you>/draftly` (or the same video link) |

---

## Quick pre-submission checklist
- [ ] `docker compose up --build` runs and Swagger opens at `/swagger-ui.html`
- [ ] You can fetch emails → generate a draft → approve → send
- [ ] README renders on GitHub with the architecture image showing
- [ ] PR is open (not merged) and the link works in an incognito window
- [ ] Video is under 5 minutes and the link is publicly viewable
