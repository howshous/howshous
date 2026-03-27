# AI Gateway

All landlord analytics AI traffic goes through a **backend gateway**. The app never talks to the AI provider directly.

## Why use a gateway

| Goal | How the gateway helps |
|------|------------------------|
| **Protect API keys** | Keys live only in Firebase config / env; the client never sees them. |
| **Enforce quotas** | Per-user daily limits (e.g. in `landlord_ai_usage`) are enforced server-side. |
| **Control prompt quality** | Context and system prompt are built on the server; the client only sends the user’s message. |
| **Cache responses** | Gateway can cache by (user, message, context) to reduce cost and latency (optional). |

## Rules — never do this from the client

- **Never** call an AI API (Groq, OpenAI, etc.) directly from the app.
- **Never** ship or expose AI API keys in the client (no `local.properties` / BuildConfig for landlord AI).
- **Never** let the client control the full prompt (no raw “system” or “context” from the client for AI).

## How it works (landlord analytics)

1. **Client** calls the callable `landlordAnalyticsAiGateway` with `{ message: "user question" }`.
2. **Gateway** (Cloud Function):
   - Verifies the user is authenticated.
   - Checks daily quota (`landlord_ai_usage/{uid}`).
   - Fetches analytics context via `buildLandlordAnalyticsAiPayload(uid)` (AI Input Builder).
   - Sanitizes the message (injection guardrails).
   - Builds the full prompt (system + context + user message) on the server.
   - Calls Groq with the API key from config/env.
   - Validates the reply (on-topic); if not, returns a safe fallback.
   - Returns `{ reply: string }`.
3. **Client** displays the reply and saves it to chat history (e.g. Firestore via app code).

## Configuring the gateway

- Set the Groq API key for the Cloud Function:
  - **Firebase config:** `firebase functions:config:set groq.api_key="YOUR_GROQ_API_KEY"`
  - **Or** set `GROQ_API_KEY` in the Cloud Functions environment (e.g. Google Cloud Console or `.env` for emulator).
- Deploy: `firebase deploy --only functions`

## No Blaze plan (free Firebase tier)

Cloud Functions require the **Blaze (pay-as-you-go)** plan. If your project is on the free Spark plan:

- You **cannot** deploy `landlordAnalyticsAiGateway` or other Cloud Functions.
- The app will **fall back to client-side AI** for landlord Insights: when the gateway call fails (e.g. “function not found” / internal error), the app builds analytics context locally and calls the Groq API from the device using **GROQ_API_KEY** in `local.properties`.
- So Insights still work without paying for Firebase; you only need a Groq API key in `local.properties`. When you upgrade to Blaze and deploy functions, the app will use the gateway automatically (no code change).

## Adding more AI features

For any new AI feature:

1. Add a **callable** (or HTTP) function that accepts only the minimal input (e.g. user message or IDs).
2. Build context and prompts **in the function**; call the AI provider from the function; validate the response.
3. Have the **client** call only that function and display the result. Do not add new direct AI calls or keys in the app.
