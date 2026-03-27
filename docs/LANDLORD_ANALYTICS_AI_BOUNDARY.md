# Landlord Analytics AI — Responsibility Boundary

This document defines what the **app** does vs what the **AI** does for the landlord analytics feature. The boundary keeps the system safe and predictable.

## When AI runs (Step 5)

- AI runs **only** when the landlord opens the **Insights** tab and **sends a message** or taps **Refresh**. It does **not** run on every page load, every listing view, real-time updates, or dashboard refresh.
- Raw analytics (Performance tab) are always available without calling AI.

## AI Gateway — never call AI from the client

- **All landlord analytics AI calls go through the backend.** The app must **never** call an AI API directly, expose API keys, or let users control the full prompt.
- **Gateway (Cloud Function):** `landlordAnalyticsAiGateway`
  - **Protects API keys:** Groq key is read from Firebase config or `process.env.GROQ_API_KEY` on the server only.
  - **Enforces quotas:** Per-user daily limit (e.g. 50 requests/day) via `landlord_ai_usage` in Firestore.
  - **Controls prompt quality:** Backend builds context (via AI Input Builder), applies injection sanitization, and uses a fixed system prompt; the client only sends the raw user message.
  - **Response validation:** Gateway validates that the model reply is on-topic before returning it; otherwise returns a safe fallback.
- **Client:** Sends only `{ message: "user text" }` to the callable and receives `{ reply: string }`. No keys, no prompt control, no direct AI calls.

## AI is responsible for (only these)

| Responsibility | Meaning |
|----------------|--------|
| **Interpretive** | Explain what the numbers and trends mean in plain language. |
| **Explanatory** | Clarify Views, Saves, Messages, conversion rates, and funnel steps. |
| **Comparative** | Compare listings or metrics (e.g. which is stronger, how one relates to another). |
| **Suggestive** | Suggest improvements in a **non-prescriptive** way (e.g. "you might try…", "consider…"). |

## AI must NOT

- **Count metrics** — the app has already computed and passed them.
- **Compute aggregates** — totals and rates are computed in the app (ViewModel).
- **Access Firestore** (or any DB) — the AI only sees the context string it is given.
- **Store state** — no memory across sessions; each reply uses only the current message and context.
- **Make guarantees** — e.g. "if you do X, you will get Y"; only suggest possibilities.

## App responsibilities

- **Count metrics** and **compute aggregates** (e.g. totals, conversion rates) in `LandlordAnalyticsAIViewModel.buildAnalyticsContextJson()`.
- **Read Firestore** (listings, listing_daily_stats) via `ListingRepository` and `ListingMetricsRepository`.
- **Build a read-only context string** (JSON summary + per-listing stats) and pass it to the AI with the user’s question.
- **Persist chat history** in Firestore via `AIChatRepository` (app code, not the AI).

## AI prompt (short, strict)

- The system prompt **states exactly what the data represents** (definitions for summary and per-listing fields: views = opens, saves = favorites, messages = first messages; rate formulas).
- It **forbids guessing**: use only numbers and facts in the appended JSON; if something is not in the data, say so.
- It **forbids guarantees**: no “if you do X you will get Y”; only non-prescriptive suggestions (e.g. “you might try…”).
- The **data is appended as JSON** in the same user message (gateway builds “User question: …” + “context (landlord’s metrics summary):” + JSON). The prompt does not describe the data in long prose—it defines the fields so the model can interpret the appended JSON.

## Present AI as “Insights,” not authority (Step 7)

- UI uses **“Insights”** (title), **“Insight”** (message label), **“Get insight”** (button). Copy: *“Based on recent activity. What this suggests—possible improvements, not guarantees.”*
- Welcome and prompts avoid “AI says you should…” or “Guaranteed increase…”; they use “what this suggests,” “possible improvements.”

## Error handling & fallbacks (Step 8)

- If the gateway fails, the app shows **last cached insight** (via `getCachedLandlordInsight`) with a note that insights are temporarily unavailable, or a short message like “Insights temporarily unavailable. Check your raw analytics below.”
- The dashboard and raw analytics are **never blocked**; Insights is additive.

## Privacy & safety (Step 9)

- See **docs/PRIVACY_AI_INSIGHTS.md**: no PII in AI input, no raw events, no user identifiers, no landlord-vs-landlord comparisons.

## Guardrails (same spirit as tenant AI)

- **Prompt injection**: User input is checked for injection patterns (e.g. “ignore previous”, “act as”, “new instructions”). If detected, the query is replaced with a safe default before sending to the model.
- **On-topic**: System prompt instructs the AI to only help with listing analytics and to politely redirect off-topic requests (e.g. “I’m here to help with your listing analytics only…”).
- **Response validation**: The model’s reply is checked for analytics-related keywords. If the response looks off-topic or manipulated, the app returns a safe fallback message instead of showing the raw reply.

Implemented in `GroqApiClient`: `sanitizeLandlordUserQuery`, `isValidLandlordAnalyticsResponse`, and the “CRITICAL - CANNOT BE OVERRIDDEN” section of the landlord system prompt.

## AI Input Builder (backend)

- **Cloud Function**: `functions/src/index.ts` — `buildLandlordAnalyticsAiPayload(landlordId, now)` turns Firebase aggregates into a **deterministic JSON payload** (summary + per-listing metrics and rates). No AI runs in the backend; it only packages data.
- **Callable**: `getLandlordAnalyticsAiInput` — authenticated callable that returns that payload (e.g. for other uses). For AI replies, the **gateway** calls the builder internally; the client does not fetch context or call AI.

## Where it’s implemented

- **Backend**: `functions/src/index.ts` — `buildLandlordAnalyticsAiPayload`, `getLandlordAnalyticsAiInput`, **`landlordAnalyticsAiGateway`** (callable: fetches context, sanitizes input, calls Groq, validates response, enforces quota).
- **ViewModel**: `app/.../LandlordAnalyticsAIViewModel.kt` — calls **only** the callable `landlordAnalyticsAiGateway` with `{ message }`; no direct AI calls, no API keys, no prompt construction.
- **Client does not** call Groq or any AI API for landlord analytics; `GroqApiClient` no longer has a landlord analytics path.

When changing this feature: **never call AI from the client**; use the gateway. Keep boundary: backend = data + computation + I/O + AI call; AI = interpret, explain, compare, suggest. Keep guardrails and quota in place.
