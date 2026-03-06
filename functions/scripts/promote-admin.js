/**
 * Promote an existing Firebase Auth user to administrator.
 *
 * Usage:
 *   node scripts/promote-admin.js <uid> [email]
 *
 * Environment:
 *   GOOGLE_APPLICATION_CREDENTIALS must point to a service account JSON key
 *   or use another supported Application Default Credentials source.
 */
const admin = require("firebase-admin");

if (admin.apps.length === 0) {
  admin.initializeApp();
}

async function main() {
  const uid = process.argv[2];
  const email = process.argv[3] || "";
  if (!uid) {
    throw new Error("Missing uid. Usage: node scripts/promote-admin.js <uid> [email]");
  }

  await admin.auth().setCustomUserClaims(uid, {admin: true});

  const db = admin.firestore();
  await db.collection("users").doc(uid).set(
    {
      uid,
      role: "administrator",
      email,
      isBanned: false,
      promotedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    {merge: true}
  );

  console.log(`User ${uid} promoted to administrator.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
