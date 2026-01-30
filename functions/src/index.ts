import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

export const ANALYTICS_EVENT_TYPES = {
  LISTING_VIEW: "LISTING_VIEW",
  LISTING_SAVE: "LISTING_SAVE",
  LISTING_MESSAGE: "LISTING_MESSAGE",
  SEARCH_PERFORMED: "SEARCH_PERFORMED",
} as const;

type AnalyticsEventType =
  typeof ANALYTICS_EVENT_TYPES[keyof typeof ANALYTICS_EVENT_TYPES];

interface AnalyticsEvent {
  eventType: AnalyticsEventType;
  listingId?: string;
  landlordId?: string;
  userId?: string;
  sessionId?: string;
  chatId?: string;
  filterKeys?: string[];
  minPrice?: number;
  maxPrice?: number;
  amenities?: string[];
  price?: number;
  timestamp?: admin.firestore.Timestamp;
}

const ALLOWED_AMENITIES = new Set<string>([
  "Free Parking",
  "WiFi",
  "Air Conditioning",
  "Pets Allowed",
  "Kitchen Access",
  "Laundry",
  "Security",
  "CCTV",
  "Furnished",
  "Near Public Transport",
  "Gym Access",
  "Swimming Pool",
]);

function normalizeEventTimestamp(
  ts: admin.firestore.Timestamp | undefined,
): {timestamp: admin.firestore.Timestamp; eventDate: string} {
  const timestamp = ts ?? admin.firestore.Timestamp.now();
  const date = timestamp.toDate();
  const eventDate = date.toISOString().slice(0, 10); // YYYY-MM-DD
  return {timestamp, eventDate};
}

export const onAnalyticsEventCreate = functions.firestore
  .document("events/{eventId}")
  .onCreate(async (snap, context) => {
    const data = snap.data() as AnalyticsEvent | undefined;
    if (!data || !data.eventType) {
      console.warn("Analytics event missing or invalid:", context.params.eventId);
      return;
    }

    await processAnalyticsEvent(data);
  });

/**
 * Core aggregation logic (exported so we can run emulator-backed tests even when
 * the Functions emulator isn't available).
 */
export async function processAnalyticsEvent(data: AnalyticsEvent) {
  const {timestamp, eventDate} = normalizeEventTimestamp(data.timestamp);

  switch (data.eventType) {
  case ANALYTICS_EVENT_TYPES.LISTING_VIEW:
    await handleListingView(data, timestamp, eventDate);
    break;
  case ANALYTICS_EVENT_TYPES.LISTING_SAVE:
    await handleListingSave(data, timestamp, eventDate);
    break;
  case ANALYTICS_EVENT_TYPES.LISTING_MESSAGE:
    await handleMessageSent(data, timestamp, eventDate);
    break;
  case ANALYTICS_EVENT_TYPES.SEARCH_PERFORMED:
    await handleSearchFilters(data, timestamp, eventDate);
    break;
  default:
    console.warn("Unknown analytics event type:", data.eventType);
  }
}

// ---------- Shared helpers for date ranges ----------

function dateKeyDaysAgo(daysAgo: number, now: Date = new Date()): string {
  const d = new Date(Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
  ));
  d.setUTCDate(d.getUTCDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}

function isWithinLastNDays(dateStr: string, days: number, now: Date = new Date()): boolean {
  const target = new Date(dateStr + "T00:00:00.000Z");
  const diffMs = now.getTime() - target.getTime();
  const diffDays = diffMs / (1000 * 60 * 60 * 24);
  return diffDays >= 0 && diffDays < days + 0.0001; // small epsilon
}

async function handleListingView(
  event: AnalyticsEvent,
  timestamp: admin.firestore.Timestamp,
  eventDate: string,
) {
  const {listingId, landlordId, sessionId} = event;
  if (!listingId || !sessionId) {
    // Guardrail: require both listingId and sessionId
    console.warn("LISTING_VIEW missing listingId or sessionId, ignoring");
    return;
  }

  const metricsRef = db.collection("listing_metrics").doc(listingId);
  const sessionRef = metricsRef.collection("sessions").doc(sessionId);
  const dailySessionsRef = db
    .collection("listing_daily_stats")
    .doc(listingId)
    .collection("days")
    .doc(eventDate)
    .collection("sessions")
    .doc(sessionId);
  const dailyRef = db
    .collection("listing_daily_stats")
    .doc(listingId)
    .collection("days")
    .doc(eventDate);

  await db.runTransaction(async (tx) => {
    const sessionSnap = await tx.get(sessionRef);
    const dailySessionSnap = await tx.get(dailySessionsRef);

    if (!sessionSnap.exists) {
      tx.set(
        sessionRef,
        {
          firstViewAt: timestamp,
          eventDate,
        },
        {merge: true},
      );

      tx.set(
        metricsRef,
        {
          listingId,
          lastViewedAt: timestamp,
          lastViewedDate: eventDate,
          uniqueSessionViews: admin.firestore.FieldValue.increment(1),
        },
        {merge: true},
      );
    }

    // Daily aggregation: dedupe by (listingId, date, sessionId)
    if (!dailySessionSnap.exists) {
      tx.set(
        dailySessionsRef,
        {
          sessionId,
          firstViewAt: timestamp,
          eventDate,
        },
        {merge: true},
      );

      tx.set(
        dailyRef,
        {
          listingId,
          landlordId: landlordId ?? null,
          date: eventDate,
          lastViewedAt: timestamp,
          views: admin.firestore.FieldValue.increment(1),
          uniqueSessions: admin.firestore.FieldValue.increment(1),
        },
        {merge: true},
      );
    } else {
      // Still update lastViewedAt even if session already counted today
      tx.set(
        dailyRef,
        {
          listingId,
          landlordId: landlordId ?? null,
          date: eventDate,
          lastViewedAt: timestamp,
        },
        {merge: true},
      );
    }
  });
}

async function handleListingSave(
  event: AnalyticsEvent,
  timestamp: admin.firestore.Timestamp,
  eventDate: string,
) {
  const {listingId, landlordId, userId} = event;
  if (!listingId || !userId) {
    console.warn("LISTING_SAVE missing listingId or userId, ignoring");
    return;
  }

  const metricsRef = db.collection("listing_metrics").doc(listingId);
  const userSaveRef = metricsRef.collection("saves").doc(userId);
  const dailyRef = db
    .collection("listing_daily_stats")
    .doc(listingId)
    .collection("days")
    .doc(eventDate);

  await db.runTransaction(async (tx) => {
    const saveSnap = await tx.get(userSaveRef);
    if (saveSnap.exists) {
      // Already counted this user's save once for this listing
      return;
    }

    tx.set(
      userSaveRef,
      {
        userId,
        firstSavedAt: timestamp,
        eventDate,
      },
      {merge: true},
    );

    tx.set(
      metricsRef,
      {
        listingId,
        lastSavedAt: timestamp,
        lastSavedDate: eventDate,
        totalSaves: admin.firestore.FieldValue.increment(1),
      },
      {merge: true},
    );

    tx.set(
      dailyRef,
      {
        listingId,
        landlordId: landlordId ?? null,
        date: eventDate,
        lastSavedAt: timestamp,
        saves: admin.firestore.FieldValue.increment(1),
      },
      {merge: true},
    );
  });
}

async function handleMessageSent(
  event: AnalyticsEvent,
  timestamp: admin.firestore.Timestamp,
  eventDate: string,
) {
  const {listingId, landlordId, chatId} = event;
  if (!listingId || !chatId) {
    console.warn("LISTING_MESSAGE missing listingId or chatId, ignoring");
    return;
  }

  const metricsRef = db.collection("listing_metrics").doc(listingId);
  const chatRef = metricsRef.collection("chats").doc(chatId);
  const dailyRef = db
    .collection("listing_daily_stats")
    .doc(listingId)
    .collection("days")
    .doc(eventDate);

  await db.runTransaction(async (tx) => {
    const chatSnap = await tx.get(chatRef);
    if (chatSnap.exists) {
      // Already counted this chat once
      return;
    }

    tx.set(
      chatRef,
      {
        chatId,
        firstMessageAt: timestamp,
        eventDate,
      },
      {merge: true},
    );

    tx.set(
      metricsRef,
      {
        listingId,
        lastMessageAt: timestamp,
        lastMessageDate: eventDate,
        firstMessageCount: admin.firestore.FieldValue.increment(1),
      },
      {merge: true},
    );

    tx.set(
      dailyRef,
      {
        listingId,
        landlordId: landlordId ?? null,
        date: eventDate,
        lastMessageAt: timestamp,
        messages: admin.firestore.FieldValue.increment(1),
      },
      {merge: true},
    );
  });
}

async function handleSearchFilters(
  event: AnalyticsEvent,
  timestamp: admin.firestore.Timestamp,
  eventDate: string,
) {
  const {userId, sessionId, filterKeys, minPrice, maxPrice, amenities} = event;
  if (!userId && !sessionId) {
    console.warn("SEARCH_PERFORMED missing both userId and sessionId, ignoring");
    return;
  }

  // Guardrail: filter value whitelisting
  const safeAmenities = (amenities ?? []).filter((a) => ALLOWED_AMENITIES.has(a));

  const safeFilterKeys = (filterKeys ?? []).filter((key) => {
    if (key === "query" || key === "minPrice" || key === "maxPrice") return true;
    if (key.startsWith("amenity:")) {
      const label = key.substring("amenity:".length);
      return ALLOWED_AMENITIES.has(label);
    }
    // Drop unknown filter keys
    return false;
  });

  const filtersRef = db.collection("search_metrics").doc(eventDate);

  const increments: Record<string, admin.firestore.FieldValue> = {};
  safeFilterKeys.forEach((key) => {
    const fieldName = `filterUsage.${key}`;
    increments[fieldName] = admin.firestore.FieldValue.increment(1);
  });

  await filtersRef.set(
    {
      lastUpdatedAt: timestamp,
      ...(Object.keys(increments).length > 0 ? increments : {}),
      minPriceSamples: minPrice ?? 0,
      maxPriceSamples: maxPrice ?? 0,
      amenitiesUsed: safeAmenities,
    },
    {merge: true},
  );
}

// ---------- Task 17: Aggregated Metrics API ----------

interface ListingMetricsSummary {
  listingId: string;
  landlordId: string | null;
  views7d: number;
  uniqueSessions7d: number;
  saves7d: number;
  messages7d: number;
  views30d: number;
  uniqueSessions30d: number;
  saves30d: number;
  messages30d: number;
}

async function computeListingMetrics(
  listingId: string,
  landlordId: string,
  now: Date = new Date(),
): Promise<ListingMetricsSummary> {
  const sevenDaysAgoKey = dateKeyDaysAgo(7, now);
  const thirtyDaysAgoKey = dateKeyDaysAgo(30, now);

  const daysSnap = await db
    .collection("listing_daily_stats")
    .doc(listingId)
    .collection("days")
    .where("date", ">=", thirtyDaysAgoKey)
    .get();

  let views7d = 0;
  let uniqueSessions7d = 0;
  let saves7d = 0;
  let messages7d = 0;
  let views30d = 0;
  let uniqueSessions30d = 0;
  let saves30d = 0;
  let messages30d = 0;

  daysSnap.forEach((doc) => {
    const data = doc.data() as {
      date?: string;
      views?: number;
      uniqueSessions?: number;
      saves?: number;
      messages?: number;
    };
    const date = data.date;
    if (!date) return;

    // 30-day window (includes last 7 days)
    if (isWithinLastNDays(date, 30, now)) {
      views30d += data.views ?? 0;
      uniqueSessions30d += data.uniqueSessions ?? 0;
      saves30d += data.saves ?? 0;
      messages30d += data.messages ?? 0;
    }

    // 7-day window
    if (isWithinLastNDays(date, 7, now)) {
      views7d += data.views ?? 0;
      uniqueSessions7d += data.uniqueSessions ?? 0;
      saves7d += data.saves ?? 0;
      messages7d += data.messages ?? 0;
    }
  });

  return {
    listingId,
    landlordId,
    views7d,
    uniqueSessions7d,
    saves7d,
    messages7d,
    views30d,
    uniqueSessions30d,
    saves30d,
    messages30d,
  };
}

export const getListingMetrics = functions.https.onCall(async (data, context) => {
  const auth = context.auth;
  if (!auth || !auth.uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const listingId = (data && data.listingId) as string | undefined;
  if (!listingId || typeof listingId !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "listingId is required.");
  }

  const listingSnap = await db.collection("listings").doc(listingId).get();
  if (!listingSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Listing not found.");
  }

  const listingData = listingSnap.data() as {landlordId?: string; price?: number} | undefined;
  const landlordId = listingData?.landlordId ?? null;
  if (!landlordId || landlordId !== auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Not allowed to view metrics for this listing.");
  }

  const metrics = await computeListingMetrics(listingId, landlordId);

  // View → Save → Message funnel (30-day window)
  const views = metrics.views30d || 0;
  const saves = metrics.saves30d || 0;
  const messages = metrics.messages30d || 0;
  const saveRate = views > 0 ? saves / views : 0;
  const messageRateFromViews = views > 0 ? messages / views : 0;
  const messageRateFromSaves = saves > 0 ? messages / saves : 0;

  return {
    listingId,
    landlordId,
    metrics7d: {
      views: metrics.views7d,
      uniqueSessions: metrics.uniqueSessions7d,
      saves: metrics.saves7d,
      messages: metrics.messages7d,
    },
    metrics30d: {
      views,
      uniqueSessions: metrics.uniqueSessions30d,
      saves,
      messages,
    },
    funnel30d: {
      views,
      saves,
      messages,
      conversionRates: {
        savePerView: saveRate,
        messagePerView: messageRateFromViews,
        messagePerSave: messageRateFromSaves,
      },
    },
  };
});

// ---------- Task 18: AI-ready summaries ----------

export const getListingAnalyticsSummary = functions.https.onCall(async (data, context) => {
  const auth = context.auth;
  if (!auth || !auth.uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const listingId = (data && data.listingId) as string | undefined;
  if (!listingId || typeof listingId !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "listingId is required.");
  }

  const listingSnap = await db.collection("listings").doc(listingId).get();
  if (!listingSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Listing not found.");
  }

  const listingData = listingSnap.data() as {
    landlordId?: string;
    price?: number;
    deposit?: number;
    location?: string;
    title?: string;
  } | undefined;
  const landlordId = listingData?.landlordId ?? null;
  if (!landlordId || landlordId !== auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Not allowed to view metrics for this listing.");
  }

  const now = new Date();
  const metrics = await computeListingMetrics(listingId, landlordId, now);

  // Top filters over the last 30 days (global, not per-listing)
  const thirtyDaysAgoKey = dateKeyDaysAgo(30, now);
  const todayKey = dateKeyDaysAgo(0, now);
  const searchDocsSnap = await db
    .collection("search_metrics")
    .where(admin.firestore.FieldPath.documentId(), ">=", thirtyDaysAgoKey)
    .where(admin.firestore.FieldPath.documentId(), "<=", todayKey)
    .get();

  const filterUsageAggregate: Record<string, number> = {};

  searchDocsSnap.forEach((doc) => {
    const data = doc.data() as {filterUsage?: Record<string, number>} | undefined;
    const usage = data?.filterUsage ?? {};
    Object.keys(usage).forEach((key) => {
      const current = filterUsageAggregate[key] ?? 0;
      filterUsageAggregate[key] = current + (usage[key] ?? 0);
    });
  });

  const topFilters = Object.entries(filterUsageAggregate)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([key, count]) => ({key, count}));

  const views = metrics.views30d || 0;
  const saves = metrics.saves30d || 0;
  const messages = metrics.messages30d || 0;

  const summary = {
    listingId,
    landlordId,
    windowDays: 30,
    metrics: {
      views,
      uniqueSessions: metrics.uniqueSessions30d,
      saves,
      messages,
    },
    funnel: {
      views,
      saves,
      messages,
    },
    topFilters,
    priceSnapshot: {
      price: listingData?.price ?? null,
      deposit: listingData?.deposit ?? null,
      location: listingData?.location ?? null,
      title: listingData?.title ?? null,
    },
  };

  // Returned as JSON-ready object suitable for AI analysis.
  return summary;
});
