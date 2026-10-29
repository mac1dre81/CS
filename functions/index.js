/**
 * Import function triggers from their respective submodules:
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { setGlobalOptions } = require("firebase-functions/v2");
const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const { google } = require("googleapis");

// The Firebase Admin SDK to access Firestore.
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

// For cost control, limit max concurrent containers per function.
// NOTE: We deliberately do NOT set serviceAccount globally here.
// Only verifyPurchase needs the Play-scoped service account.
setGlobalOptions({ maxInstances: 10 });

// ---------------------------------------------------------------------------
// Sample: HTTP endpoint — writes a message to Firestore
// ---------------------------------------------------------------------------
exports.addmessage = onRequest(async (req, res) => {
  // Grab the text parameter.
  const original = req.query.text;
  // Push the new message into Firestore using the Firebase Admin SDK.
  const writeResult = await getFirestore()
    .collection("messages")
    .add({ original });
  // Send back a message that we've successfully written the message
  res.json({ result: `Message with ID: ${writeResult.id} added.` });
});

// ---------------------------------------------------------------------------
// Sample: Firestore trigger — uppercases new messages
// ---------------------------------------------------------------------------
exports.makeuppercase = onDocumentCreated("/messages/{documentId}", (event) => {
  // Grab the current value of what was written to Firestore.
  const original = event.data.data().original;

  // Access the parameter `{documentId}` with `event.params`
  logger.log("Uppercasing", event.params.documentId, original);

  const uppercase = original.toUpperCase();

  // You must return a Promise when performing asynchronous tasks inside a
  // function such as writing to Firestore.
  return event.data.ref.set({ uppercase }, { merge: true });
});

// ---------------------------------------------------------------------------
// Google Play purchase verification
// ---------------------------------------------------------------------------

const PACKAGE_NAME = "com.credstow.app";
const ALLOWED_PRODUCTS = ["pro_unlock", "pro_monthly", "pro_yearly"];

exports.verifyPurchase = onCall(
  {
    // Runs as the dedicated Play-verifying service account.
    // Grant it 'Google Play Android Developer' (GCP IAM) + 'Release manager' (Play Console).
    serviceAccount: "play-verifier@credstor-backend.iam.gserviceaccount.com",
    // NOTE: Grant this SA these GCP IAM roles:
    //   1. "roles/androidpublisher.user" (Google Play Android Developer)
    //   2. "roles/iam.serviceAccountUser" (Service Account User)
    // Then add it as a "Release manager" in Play Console → Setup → App integrity → API access
  },
  async (request) => {
    // 1. Require authentication
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in required");
    }

    // 2. Validate inputs
    const { purchaseToken, productId } = request.data;
    if (!purchaseToken || !ALLOWED_PRODUCTS.includes(productId)) {
      throw new HttpsError("invalid-argument", "Invalid request");
    }

    // 3. Prevent replay: check if this token was already used by another account
    const db = getFirestore();
    const tokenRef = db.collection("purchaseTokens").doc(purchaseToken);
    const tokenDoc = await tokenRef.get();
    if (tokenDoc.exists && tokenDoc.data().uid !== request.auth.uid) {
      throw new HttpsError("already-exists", "Token already used");
    }

    // 4. Authenticate using the function's runtime service account
    //    (no credentials object — google.auth picks up the identity automatically)
    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const androidPublisher = google.androidpublisher({
      version: "v3",
      auth: await auth.getClient(),
    });

    try {
      // 5. Verify with Google
      const response = await androidPublisher.purchases.products.get({
        packageName: PACKAGE_NAME,
        productId,
        token: purchaseToken,
      });

      // 6. purchaseState === 0 means PURCHASED
      if (response.data.purchaseState !== 0) {
        return { status: "failed", verified: false, reason: "not_purchased" };
      }

      // 7. Acknowledge the purchase so Google doesn't auto-refund after 3 days
      if (response.data.acknowledgementState === 0) {
        await androidPublisher.purchases.products.acknowledge({
          packageName: PACKAGE_NAME,
          productId,
          token: purchaseToken,
        });
      }

      // 8. Persist purchase — keyed by token (replay protection) and by uid (entitlement)
      await tokenRef.set({
        uid: request.auth.uid,
        productId,
        verifiedAt: FieldValue.serverTimestamp(),
      });

      await db.collection("users").doc(request.auth.uid).set(
        {
          purchases: {
            [productId]: {
              active: true,
              purchaseToken,
              verifiedAt: FieldValue.serverTimestamp(),
            },
          },
        },
        { merge: true }
      );

      return { status: "success", verified: true };
    } catch (error) {
      logger.error("Verification error:", error);
      throw new HttpsError("internal", "Verification failed");
    }
  }
);