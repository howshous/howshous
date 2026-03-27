# Privacy & Safety — Landlord AI Insights

Before going live, verify the following. No PII, no raw events, no user identifiers, and no comparisons that expose other landlords.

## Checklist

| Check | Status |
|-------|--------|
| **No PII in AI input** | ✅ Only listing-level aggregates and listing metadata (listingId, title, price). No tenant names, emails, phones, or identifiers. |
| **No raw events sent** | ✅ Only pre-aggregated counts (views7d/30d, saves7d/30d, messages7d/30d) and derived rates. No event-level data. |
| **No user identifiers** | ✅ Landlord is identified only by auth context on the server; the AI payload does not contain landlord email, uid in the prompt, or any other account PII. |
| **No landlord comparisons that expose others** | ✅ Each landlord sees only their own data. We do not compare one landlord to others or send any cross-landlord data to the AI. |

## What is sent to the AI (gateway builds this)

- **summary:** `totalViews30d`, `totalSaves30d`, `totalMessages30d`, and three percentage rates (all over the landlord’s own listings).
- **listings:** For each of the landlord’s listings: `listingId`, `title`, `price`, and the same aggregate counts and rates.

No tenant IDs, no chat content, no emails, no raw events. Listing titles may be user-generated; avoid putting highly sensitive text in listing titles if concerned.

## Where it’s enforced

- **AI Input Builder** (`buildLandlordAnalyticsAiPayload`) and **Gateway** only use data from Firestore that the app already uses for the Performance tab (listings + listing_daily_stats). No new PII or events are introduced.
- **Prompt** instructs the AI not to guess or guarantee; it does not receive or refer to other landlords.
