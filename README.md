# TuneAcademy

A small web app for music learners and instructors, with two parts:

- **Practice:** a learner plays along with a reference recording, records themselves in the browser, and gets a score for timing and notes, with each note marked as hit, missed or wrong.
- **Marketplace:** learners browse instructors and send a lesson request. The instructor accepts or declines. Once a request is accepted, both sides can see each other's email and arrange lessons directly.

It started as a hackathon project on Firebase and Python. I rebuilt it on a stack I own end to end, and cut it down to the parts I can explain and test.

## Stack

| Part     | Choice                                                                                                            |
| -------- | ----------------------------------------------------------------------------------------------------------------- |
| Frontend | React single-page app (Vite, TypeScript, TanStack Router, Tailwind)                                               |
| Backend  | Java 21, Spring Boot. The same app also serves the built React files: one origin, so no CORS                      |
| Data     | Postgres. Flyway migrations, JPA                                                                                  |
| Auth     | Spring Security: a session in an httpOnly cookie, BCrypt passwords, CSRF protection, learner and instructor roles |
| Scoring  | Pure Java, using TarsosDSP's YIN pitch detector                                                                   |
| Hosting  | One AWS Lightsail server running Docker Compose (app and Postgres), with Caddy for HTTPS                          |
| CI/CD    | GitHub Actions: checks on every pull request, deploy on every push to `main`                                      |

## Design choices

- **Student audio is never stored.** The upload is scored in memory, and only the score and the detected notes are saved. That's enough to explain a bad score afterwards without keeping anyone's recording.
- **How scoring works.** A note starts where the loudness jumps compared with the moment before it. YIN gives each note's pitch. The first notes of the two recordings are lined up, and a note counts as on time within about 100 ms. Extra notes lower the timing score. The total is the average of the timing and note scores.
- **Privacy by default.** The public instructor list never reads email addresses from the database. Emails are only sent to the browser once a request is accepted, and only to the two people involved.
- **Owner-only data.** Asking for someone else's attempt or request returns 404, not 403, so the app doesn't confirm that it exists.
- **All-or-nothing writes.** Accepting a request marks it accepted and creates the engagement in one transaction. A test proves that a failure rolls back both.
- **Tests run against real Postgres.** Backend tests make real HTTP calls to the running app (with cookies and CSRF), and Testcontainers starts a throwaway database. The scoring engine has plain unit tests with audio generated in code.

## Known limits

- **Struck or plucked notes only.** Note starts come from loudness jumps, so piano and guitar work well, but sung or bowed notes that flow together can be missed.
- **Quick notes after a ringing note** can be missed, because the jump isn't large enough over the still-ringing note.
- **Chords aren't scored yet.** Pitch detection follows a single line (usually the bass), so the chord exercise is greyed out. The backend already has a timing-only scoring path for it.

## Demo accounts

Three demo instructors are seeded: `maya@example.com`, `diego@example.com` and `amara@example.com`, all with the password `demo-instructor`. Sign up as a learner to send them requests.

## Running TuneAcademy

Three pieces run locally: Postgres in Docker, the Spring Boot backend in `backend/`, and the Vite + React frontend at the repository root. You need Docker, Java 21 and Node 20+.

### Run locally

From the repository root. The backend and the frontend each keep running in their own terminal.

```bash
# 1. Database (Postgres on port 5432)
docker compose up -d

# 2. Backend (port 8080). Flyway creates the tables and seeds the exercises and demo instructors.
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 3. Frontend (port 5173). Vite forwards /api to the backend.
npm install
npm run dev
```

Then open `http://localhost:5173`. No `.env` file is needed.

### Checks

```bash
npx tsc --noEmit             # frontend type check
npm run build                # frontend build
cd backend && ./mvnw test    # backend tests (Docker must be running: they start their own Postgres)
```

### Deploy

Production is one Docker image: the `Dockerfile` builds React into the Spring Boot jar, which serves both the pages and `/api`. It runs on an AWS Lightsail server next to Postgres and Caddy (HTTPS), as set up in `deploy/`.

- Every pull request runs the checks above, the linter and a dependency vulnerability scan (`.github/workflows/ci.yml`).
- Every push to `main` runs the same checks, then builds the image, pushes it to GitHub's registry and restarts the app on the server (`.github/workflows/deploy.yml`).

One-time setup: the server needs Docker and a `~/tuneacademy/.env` file with `DOMAIN` and `DB_PASSWORD`, and the GitHub repository needs two secrets, `LIGHTSAIL_HOST` (the server's IP) and `LIGHTSAIL_SSH_KEY` (its private SSH key).
