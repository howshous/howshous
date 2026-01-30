"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onAnalyticsEventCreate = exports.ANALYTICS_EVENT_TYPES = void 0;
exports.processAnalyticsEvent = processAnalyticsEvent;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
if (admin.apps.length === 0) {
    admin.initializeApp();
}
const db = admin.firestore();
exports.ANALYTICS_EVENT_TYPES = {
    LISTING_VIEW: "LISTING_VIEW",
    LISTING_SAVE: "LISTING_SAVE",
    LISTING_MESSAGE: "LISTING_MESSAGE",
    SEARCH_PERFORMED: "SEARCH_PERFORMED",
};
const ALLOWED_AMENITIES = new Set([
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
function normalizeEventTimestamp(ts) {
    const timestamp = ts ?? admin.firestore.Timestamp.now();
    const date = timestamp.toDate();
    const eventDate = date.toISOString().slice(0, 10); // YYYY-MM-DD
    return { timestamp, eventDate };
}
exports.onAnalyticsEventCreate = functions.firestore
    .document("events/{eventId}")
    .onCreate(async (snap, context) => {
    const data = snap.data();
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
async function processAnalyticsEvent(data) {
    const { timestamp, eventDate } = normalizeEventTimestamp(data.timestamp);
    switch (data.eventType) {
        case exports.ANALYTICS_EVENT_TYPES.LISTING_VIEW:
            await handleListingView(data, timestamp, eventDate);
            break;
        case exports.ANALYTICS_EVENT_TYPES.LISTING_SAVE:
            await handleListingSave(data, timestamp, eventDate);
            break;
        case exports.ANALYTICS_EVENT_TYPES.LISTING_MESSAGE:
            await handleMessageSent(data, timestamp, eventDate);
            break;
        case exports.ANALYTICS_EVENT_TYPES.SEARCH_PERFORMED:
            await handleSearchFilters(data, timestamp, eventDate);
            break;
        default:
            console.warn("Unknown analytics event type:", data.eventType);
    }
}
async function handleListingView(event, timestamp, eventDate) {
    const { listingId, landlordId, sessionId } = event;
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
            tx.set(sessionRef, {
                firstViewAt: timestamp,
                eventDate,
            }, { merge: true });
            tx.set(metricsRef, {
                listingId,
                lastViewedAt: timestamp,
                lastViewedDate: eventDate,
                uniqueSessionViews: admin.firestore.FieldValue.increment(1),
            }, { merge: true });
        }
        // Daily aggregation: dedupe by (listingId, date, sessionId)
        if (!dailySessionSnap.exists) {
            tx.set(dailySessionsRef, {
                sessionId,
                firstViewAt: timestamp,
                eventDate,
            }, { merge: true });
            tx.set(dailyRef, {
                listingId,
                landlordId: landlordId ?? null,
                date: eventDate,
                lastViewedAt: timestamp,
                views: admin.firestore.FieldValue.increment(1),
                uniqueSessions: admin.firestore.FieldValue.increment(1),
            }, { merge: true });
        }
        else {
            // Still update lastViewedAt even if session already counted today
            tx.set(dailyRef, {
                listingId,
                landlordId: landlordId ?? null,
                date: eventDate,
                lastViewedAt: timestamp,
            }, { merge: true });
        }
    });
}
async function handleListingSave(event, timestamp, eventDate) {
    const { listingId, landlordId, userId } = event;
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
        tx.set(userSaveRef, {
            userId,
            firstSavedAt: timestamp,
            eventDate,
        }, { merge: true });
        tx.set(metricsRef, {
            listingId,
            lastSavedAt: timestamp,
            lastSavedDate: eventDate,
            totalSaves: admin.firestore.FieldValue.increment(1),
        }, { merge: true });
        tx.set(dailyRef, {
            listingId,
            landlordId: landlordId ?? null,
            date: eventDate,
            lastSavedAt: timestamp,
            saves: admin.firestore.FieldValue.increment(1),
        }, { merge: true });
    });
}
async function handleMessageSent(event, timestamp, eventDate) {
    const { listingId, landlordId, chatId } = event;
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
        tx.set(chatRef, {
            chatId,
            firstMessageAt: timestamp,
            eventDate,
        }, { merge: true });
        tx.set(metricsRef, {
            listingId,
            lastMessageAt: timestamp,
            lastMessageDate: eventDate,
            firstMessageCount: admin.firestore.FieldValue.increment(1),
        }, { merge: true });
        tx.set(dailyRef, {
            listingId,
            landlordId: landlordId ?? null,
            date: eventDate,
            lastMessageAt: timestamp,
            messages: admin.firestore.FieldValue.increment(1),
        }, { merge: true });
    });
}
async function handleSearchFilters(event, timestamp, eventDate) {
    const { userId, sessionId, filterKeys, minPrice, maxPrice, amenities } = event;
    if (!userId && !sessionId) {
        console.warn("SEARCH_PERFORMED missing both userId and sessionId, ignoring");
        return;
    }
    // Guardrail: filter value whitelisting
    const safeAmenities = (amenities ?? []).filter((a) => ALLOWED_AMENITIES.has(a));
    const safeFilterKeys = (filterKeys ?? []).filter((key) => {
        if (key === "query" || key === "minPrice" || key === "maxPrice")
            return true;
        if (key.startsWith("amenity:")) {
            const label = key.substring("amenity:".length);
            return ALLOWED_AMENITIES.has(label);
        }
        // Drop unknown filter keys
        return false;
    });
    const filtersRef = db.collection("search_metrics").doc(eventDate);
    const increments = {};
    safeFilterKeys.forEach((key) => {
        const fieldName = `filterUsage.${key}`;
        increments[fieldName] = admin.firestore.FieldValue.increment(1);
    });
    await filtersRef.set({
        lastUpdatedAt: timestamp,
        ...(Object.keys(increments).length > 0 ? increments : {}),
        minPriceSamples: minPrice ?? 0,
        maxPriceSamples: maxPrice ?? 0,
        amenitiesUsed: safeAmenities,
    }, { merge: true });
}
//# sourceMappingURL=index.js.map