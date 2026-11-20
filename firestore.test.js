const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("scanHistory").get());
  await assertFails(unauthDb.collection("communityReports").where("moderationStatus", "==", "published").get());
  await assertFails(
    unauthDb.collection("communityReports").doc("rep_1").set({
      userId: ALICE_UID,
      category: "SMS_SCAM",
      description: "Fake electricity disconnect SMS asking to call unknown number.",
      incidentDate: "2026-11-20",
      moderationStatus: "published",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserProfile: owner can create, read, update; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav Sharma",
      indianPhoneNumber: "+919876543210",
      preferredLanguage: "en",
      callScreeningEnabled: false,
      privacyMinimizeStorage: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "hi",
      privacyMinimizeStorage: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field & immutable createdAt tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("ScanHistory: owner CRUD isolated from other users, rejects @DocumentId id key", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const validScan = {
    userId: ALICE_UID,
    scanType: "MESSAGE",
    riskLevel: "CRITICAL_RISK",
    riskScore: 94,
    scamCategory: "Fake Electricity Disconnection",
    summary: "Urgent disconnection threat demanding immediate call.",
    inputPreview: "Dear consumer your power will be disconnected tonight...",
    explanation: "Legitimate electricity boards do not demand instant personal transfers.",
    indicators: ["disconnection tonight", "call officer immediately"],
    recommendedActions: ["Do not call the number", "Verify on official BESCOM/MSEDCL portal"],
    detectedUrls: ["http://bit.ly/fake-bill-pay"],
    language: "en",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").doc("scan_1").set(validScan)
  );

  // List own scanHistory succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").get()
  );

  // Cross-user read & delete fail
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scanHistory").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scanHistory").doc("scan_1").delete()
  );

  // Reject @DocumentId "id" key inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").doc("scan_2").set({
      ...validScan,
      id: "scan_2",
    })
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").doc("scan_1").delete()
  );
});

test("CommunityReports: query alignment, ownership, and anti-poisoning", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("communityReports").doc("rep_1").set({
      userId: ALICE_UID,
      category: "UPI_FRAUD",
      description: "Caller sent fake UPI collect request pretending to buy sofa on OLX.",
      reportedTarget: "+919811122233",
      incidentDate: "2026-11-20",
      moderationStatus: "published",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Query aligned with rules succeeds
  await assertSucceeds(
    bobDb.collection("communityReports").where("moderationStatus", "==", "published").get()
  );
  await assertSucceeds(
    aliceDb.collection("communityReports").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered query must fail
  await assertFails(bobDb.collection("communityReports").get());

  // Bob cannot update or delete Alice's report
  await assertFails(
    bobDb.collection("communityReports").doc("rep_1").update({
      description: "Tampered by another user",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("communityReports").doc("rep_1").delete());

  // Alice can delete her own report
  await assertSucceeds(aliceDb.collection("communityReports").doc("rep_1").delete());
});
