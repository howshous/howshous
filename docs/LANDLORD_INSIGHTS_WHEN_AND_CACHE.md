# When Landlord Insights AI Runs & Caching

## Step 5 — When AI runs

**Good triggers (only these):**
- Landlord opens the **Insights** tab (chat screen) and **sends a message** or taps **Refresh**. AI runs only on that explicit action.
- Optionally: daily/weekly scheduled job (if you add one later).
- Optionally: after significant data change (e.g. 30-day window complete)—handled via cache invalidation when metrics change.

**Bad triggers (we do not do):**
- Every listing view
- Real-time updates
- Every dashboard refresh

The app never calls the gateway on dashboard load or when viewing the Performance tab. Raw analytics are always shown without invoking AI. AI runs only when the landlord is on the Insights screen and sends a question or taps Refresh.

## Step 6 — Cache AI output

- **Stored in Firestore:** `landlord_ai_insights/{uid}` with `lastReply`, `contextHash`, `messageHash`, `generatedAt`.
- **Cache hit:** Same landlord, same question (`messageHash`), same metrics (`contextHash`), and cache age ≤ 7 days → return cached reply without calling the AI or using quota.
- **Regenerate only when:**
  - **Time window / metrics change:** Payload (and thus `contextHash`) changes, so cache is missed and we regenerate.
  - **Landlord explicitly refreshes:** Client sends `refresh: true`; gateway skips cache and calls AI.
  - **Cache expired:** Older than 7 days.
- **Cost & UX:** Fewer AI calls, faster responses when cache hits, and quota preserved.

## Step 8 — Error handling & fallbacks

- If the gateway fails (e.g. AI down, quota exceeded, no API key):
  - The app tries **getCachedLandlordInsight** and, if a cached reply exists, shows it with the note: *"Insights temporarily unavailable for new questions. Showing last saved insight."*
  - Otherwise shows: *"Insights temporarily unavailable"* (and for quota: *"Daily insight limit reached. Try again tomorrow."*).
- **Never block the dashboard:** The Performance tab and raw analytics are always available. The Insights screen is additive; if it fails, the landlord can still use all other screens and data.
