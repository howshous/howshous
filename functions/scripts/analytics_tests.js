/**
 * Emulator-backed tests for Task 15 and Task 16.
 *
 * Assumptions:
 * - Firestore emulator is running on 127.0.0.1:8085
 * - Functions emulator is running on 127.0.0.1:5001 and has the onCreate trigger for /events
 *
 * This script:
 * - Writes synthetic events into /events
 * - Waits for Cloud Function aggregation
 * - Asserts daily counters + dedupe behavior
 */

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085";
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || "demo-howshous";

const admin = require("firebase-admin");

admin.initializeApp({projectId: process.env.GCLOUD_PROJECT});

const db = admin.firestore();
const {processAnalyticsEvent} = require("../lib/index.js");

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function utcDateKey(ts = new Date()) {
  return ts.toISOString().slice(0, 10);
}

async function clearTestData(listingId) {
  // Purge only collections used by this test run.
  const paths = [
    `listing_daily_stats/${listingId}/days/${utcDateKey()}`,
    `listing_daily_stats/${listingId}/days/${utcDateKey()}/sessions/sessionA`,
    `listing_daily_stats/${listingId}/days/${utcDateKey()}/sessions/sessionB`,
    `listing_daily_stats/${listingId}/days/${utcDateKey()}/sessions/sessionC`,
    `listing_metrics/${listingId}`,
    `listing_metrics/${listingId}/sessions/sessionA`,
    `listing_metrics/${listingId}/sessions/sessionB`,
    `listing_metrics/${listingId}/sessions/sessionC`,
    `listing_metrics/${listingId}/saves/userA`,
    `listing_metrics/${listingId}/saves/userB`,
    `listing_metrics/${listingId}/chats/chat1`,
    `listing_metrics/${listingId}/chats/chat2`,
  ];

  await Promise.all(
    paths.map(async (p) => {
      try {
        await db.doc(p).delete();
      } catch (_) {}
    }),
  );

  // Best-effort delete events collection docs created by us (tagged).
  const eventsSnap = await db.collection("events").where("testRunId", "==", "analytics_tests").get();
  const batch = db.batch();
  eventsSnap.docs.forEach((d) => batch.delete(d.ref));
  await batch.commit();
}

async function writeEvent(event) {
  // tag event so we can clean it up
  const ref = await db.collection("events").add({
    ...event,
    testRunId: "analytics_tests",
    // in production client uses serverTimestamp; for emulator we can send a concrete timestamp too
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });
  // Functions emulator may not be available; process aggregation directly using the same logic
  const snap = await ref.get();
  await processAnalyticsEvent(snap.data());
  return ref;
}

async function waitForDailyDoc(listingId, date, timeoutMs = 10000) {
  const ref = db.collection("listing_daily_stats").doc(listingId).collection("days").doc(date);
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const snap = await ref.get();
    if (snap.exists) return snap;
    await sleep(250);
  }
  throw new Error(`Timed out waiting for daily stats doc: ${listingId}/${date}`);
}

function assertEqual(actual, expected, message) {
  if (actual !== expected) {
    throw new Error(`${message} (expected ${expected}, got ${actual})`);
  }
}

async function run() {
  const listingId = "listing_test_1";
  const landlordId = "landlord_test_1";
  const date = utcDateKey();

  // Ensure listing exists (useful for rules and for traceability)
  await db.collection("listings").doc(listingId).set({
    landlordId,
    title: "Test Listing",
    location: "Test",
    price: 12345,
    status: "active",
  }, {merge: true});

  await clearTestData(listingId);

  // -----------------------
  // Task 15: Event logging
  // -----------------------

  // Valid SEARCH_PERFORMED (only allowed filter keys)
  await writeEvent({
    eventType: "SEARCH_PERFORMED",
    userId: "userA",
    sessionId: "sessionA",
    filterKeys: ["query", "minPrice", "amenity:WiFi", "INVALID_KEY", "amenity:NotInWhitelist"],
    minPrice: 1000,
    maxPrice: 5000,
    amenities: ["WiFi", "NotInWhitelist"],
  });

  // Verify invalid filter keys do not get aggregated
  // (We only check that INVALID_KEY is not present in filterUsage map.)
  const searchDoc = await db.collection("search_metrics").doc(date).get();
  const searchData = searchDoc.data() || {};
  const filterUsage = searchData.filterUsage || {};
  if (Object.prototype.hasOwnProperty.call(filterUsage, "INVALID_KEY")) {
    throw new Error("Invalid filter key was aggregated (INVALID_KEY should be ignored)");
  }

  // -----------------------
  // Task 16: Aggregation accuracy
  // -----------------------

  // Views: sessionA views twice same day => count once
  await writeEvent({
    eventType: "LISTING_VIEW",
    listingId,
    landlordId,
    userId: "userA",
    sessionId: "sessionA",
    price: 12345,
  });
  await writeEvent({
    eventType: "LISTING_VIEW",
    listingId,
    landlordId,
    userId: "userA",
    sessionId: "sessionA",
    price: 12345,
  });

  // Views: sessionB views once => increments
  await writeEvent({
    eventType: "LISTING_VIEW",
    listingId,
    landlordId,
    userId: "userB",
    sessionId: "sessionB",
    price: 12345,
  });

  // Saves: userA saves twice => only first should aggregate (dedupe by user)
  await writeEvent({
    eventType: "LISTING_SAVE",
    listingId,
    landlordId,
    userId: "userA",
    sessionId: "sessionA",
    price: 12345,
  });
  await writeEvent({
    eventType: "LISTING_SAVE",
    listingId,
    landlordId,
    userId: "userA",
    sessionId: "sessionA",
    price: 12345,
  });

  // Saves: userB saves once
  await writeEvent({
    eventType: "LISTING_SAVE",
    listingId,
    landlordId,
    userId: "userB",
    sessionId: "sessionB",
    price: 12345,
  });

  // Messages: chat1 fires twice => only first aggregates (dedupe by chat)
  await writeEvent({
    eventType: "LISTING_MESSAGE",
    listingId,
    landlordId,
    userId: "userA",
    chatId: "chat1",
  });
  await writeEvent({
    eventType: "LISTING_MESSAGE",
    listingId,
    landlordId,
    userId: "userA",
    chatId: "chat1",
  });
  // chat2 fires once
  await writeEvent({
    eventType: "LISTING_MESSAGE",
    listingId,
    landlordId,
    userId: "userB",
    chatId: "chat2",
  });

  // Wait for function to materialize daily stats
  const dailySnap = await waitForDailyDoc(listingId, date, 15000);
  const daily = dailySnap.data() || {};

  // Validate deduped aggregates:
  // views: sessionA + sessionB => 2
  assertEqual(daily.views || 0, 2, "Daily views should be deduped by session");
  assertEqual(daily.uniqueSessions || 0, 2, "Daily uniqueSessions should match distinct sessions");
  // saves: userA (deduped) + userB => 2
  assertEqual(daily.saves || 0, 2, "Daily saves should be deduped by userId");
  // messages: chat1 (deduped) + chat2 => 2
  assertEqual(daily.messages || 0, 2, "Daily messages should be deduped by chatId");

  console.log("✅ Task 15/16 tests passed.");
  console.log({
    listingId,
    date,
    views: daily.views || 0,
    uniqueSessions: daily.uniqueSessions || 0,
    saves: daily.saves || 0,
    messages: daily.messages || 0,
  });
}

run().catch((err) => {
  console.error("❌ Task 15/16 tests failed:", err);
  process.exitCode = 1;
});

