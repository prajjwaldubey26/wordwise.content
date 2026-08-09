# AI Content Generator & Plagiarism Detector

A full-stack college capstone app for creating AI-assisted content, summarizing textbook chapters from PDFs, generating quizzes, and checking originality against a growing local corpus.

## Stack

- Frontend: React 18, React Router v6, Bootstrap/react-bootstrap, Axios, Recharts
- Backend: Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, Validation, Maven, PDFBox
- Database: MySQL 8
- Authentication: BCrypt password hashing and stateless JWTs
- Payments: Razorpay Checkout in test mode

## Run locally

### 1. Create the MySQL database

Start MySQL 8, then run:

```sql
CREATE DATABASE ai_content_detector;
```

The default backend connection is `root` with an empty password. Override it before running if your MySQL setup differs:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
$env:DB_URL = "jdbc:mysql://localhost:3306/ai_content_detector?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

### 2. Run the backend

Install Java 17 and Maven, then:

```powershell
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. Hibernate creates and updates the tables automatically.

### 3. Run the frontend

In another terminal:

```powershell
cd frontend
npm install
npm start
```

The site opens at `http://localhost:3000`. The React development server proxies `/api` calls to Spring Boot.

## Deploying the website on Vercel

Vercel hosts the React frontend only. The Java/Spring Boot API and MySQL database must be deployed separately; without that API, registration, login, and every saved feature will fail.

1. Deploy the `backend` directory to a Docker-capable Java host (for example, Render, Railway, Fly.io, or a VPS) using [backend/Dockerfile](backend/Dockerfile).
2. Provision a MySQL 8 database and set these backend environment variables:

   ```text
   DB_URL=jdbc:mysql://YOUR_MYSQL_HOST:3306/ai_content_detector?useSSL=true&serverTimezone=UTC
   DB_USERNAME=YOUR_DATABASE_USER
   DB_PASSWORD=YOUR_DATABASE_PASSWORD
   JWT_SECRET=use-a-long-random-secret-of-at-least-32-characters
   CORS_ALLOWED_ORIGINS=https://wordwise-content.vercel.app,http://localhost:3000
   FRONTEND_URL=https://wordwise-content.vercel.app
   ```

3. Confirm the deployed backend responds at `https://YOUR-BACKEND-URL/api/health` with `{"status":"ok","service":"wordwise-api"}`.
4. In Vercel, open the frontend project, go to **Settings → Environment Variables**, and add this production variable:

   ```text
   REACT_APP_API_URL=https://YOUR-BACKEND-URL/api
   ```

5. Redeploy the Vercel project. Create React App reads `REACT_APP_*` variables during the build, so simply saving the variable is not enough.

The frontend includes `.env.example` with the same variable name. Do not place database passwords or API keys in Vercel frontend variables.

### Temporary ngrok backend

For a temporary demo tunnel, `frontend/.env.production` is configured to use `https://lavender-follicle-talisman.ngrok-free.dev/api`. The frontend adds ngrok's skip-warning header automatically. Restart the Spring Boot backend after pulling these changes so its CORS configuration allows `https://wordwise-content.vercel.app`, then rebuild/redeploy Vercel. ngrok free URLs can expire or change when the tunnel restarts; update `REACT_APP_API_URL` whenever that happens.

## Demo flow

1. Register a user. All accounts start as `USER` with a `FREE` plan.
2. Generate content; the generated text is saved in the comparison corpus.
3. Upload a text-based PDF on **Summary & Quiz**. The app extracts text, creates a summary, saves it, and renders an interactive 5-question quiz.
4. Paste a draft into **Originality**. It is compared against all saved generations, summaries, and earlier checks.
5. Visit **History** and **Reports** to see persisted results.

To demo admin reports, change a user’s `role` column to `ADMIN` in MySQL, then sign out and sign in again.

## AI provider configuration

The default provider is `mock`, a deterministic, entirely offline generator. No API key is needed for the full core demo.

Set these environment variables to select a cloud provider:

```powershell
# Default offline demo
$env:AI_PROVIDER = "mock"

# OpenAI
$env:AI_PROVIDER = "openai"
$env:OPENAI_API_KEY = "your-key"
$env:OPENAI_MODEL = "gpt-4o-mini"

# Anthropic
$env:AI_PROVIDER = "anthropic"
$env:ANTHROPIC_API_KEY = "your-key"
$env:ANTHROPIC_MODEL = "claude-3-5-haiku-latest"
```

If a configured cloud call fails or its key is missing, the backend automatically falls back to the mock provider. For quiz generation, malformed provider JSON is retried once with a stricter instruction before the mock quiz fallback runs.

## Razorpay test mode

Create test API keys at [Razorpay Dashboard → API Keys](https://dashboard.razorpay.com/app/keys), then set them before starting the backend:

```powershell
$env:RAZORPAY_KEY_ID = "rzp_test_..."
$env:RAZORPAY_KEY_SECRET = "your_test_secret"
$env:FRONTEND_URL = "http://localhost:3000"
```

The Pricing page creates a Razorpay order for the `₹499` Pro demo plan, opens Razorpay Checkout, then calls `/api/payments/confirm` with `orderId`, `paymentId`, and `signature`. The backend verifies the signature, creates `payments` and `subscriptions` records, and upgrades the account to `PRO`.

Use Razorpay test cards (for example `4111 1111 1111 1111`) from their docs while in test mode.

## Plagiarism algorithm, in plain English

The checker is intentionally local and does not use a paid plagiarism API:

1. It lowercases text and removes punctuation.
2. It turns each document into overlapping five-word phrases (shingles).
3. It compares the submitted document with every prior generated draft, chapter summary, and plagiarism-check document.
4. For each source it calculates Jaccard similarity (shared unique shingles divided by all unique shingles) and cosine similarity over shingle term-frequency vectors.
5. The final source score is `55% Jaccard + 45% cosine`; the highest source score is the overall similarity result.
6. Scores below 25% are **Original**, 25–59.99% show **Minor overlap detected**, and 60%+ is **Likely plagiarized**.

The API returns the top five non-zero matches so a presenter can explain exactly where an overlap came from.

## PDF-to-summary-and-quiz pipeline

The upload endpoint accepts only PDFs up to 10 MB. PDFBox (`Loader` + `PDFTextStripper`) extracts text server-side. Image-only scanned PDFs are rejected because there is no usable extractable text. The text is passed to the configured AI service to create a short (~100 word), medium (~250 word), or long (~500 word) summary and exactly five MCQs. The summary, source text (capped at 50,000 characters), filename, and MCQ JSON are persisted in `chapter_summaries`.

## API endpoints

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register and receive JWT |
| POST | `/api/auth/login` | No | Login and receive JWT |
| POST | `/api/content/generate` | User | Generate and save content |
| GET | `/api/content/history` | User | Content history |
| POST | `/api/chapters/summarize` | User | PDF upload, summary, and quiz |
| GET | `/api/chapters/history` | User | Chapter history |
| POST | `/api/plagiarism/check` | User | Run a local similarity check |
| GET | `/api/plagiarism/history` | User | Check history |
| GET | `/api/dashboard` | User | Personal totals and average similarity |
| GET | `/api/reports/admin` | Admin | Platform-wide metrics |
| POST | `/api/payments/create-order` | User | Create Razorpay order |
| POST | `/api/payments/confirm` | User | Verify Razorpay signature and activate Pro |

Protected calls require `Authorization: Bearer <jwt>`.

## Data model

Hibernate creates these tables: `users`, `content_generations`, `chapter_summaries`, `plagiarism_checks`, `payments`, and `subscriptions`.

## Validation

From `backend`, run:

```powershell
mvn test
```

From `frontend`, after `npm install`, run:

```powershell
npm run build
```

`npm run lint` is configured as the same production build check used by Create React App.
