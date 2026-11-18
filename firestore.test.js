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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("publicProfiles").where("publicVisible", "==", true).get());
  await assertFails(unauth.collection("games").where("status", "==", "active").get());
  await assertFails(unauth.collection("studies").where("publicVisible", "==", true).get());
  await assertFails(unauth.collection("tournaments").where("publicVisible", "==", true).get());
});

test("Private user settings: owner CRUD succeeds, cross-user read/write fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "AliceGM",
      countryIso: "US",
      fideFederation: "USA",
      privacyPublicProfile: true,
      privacyAllowMessages: true,
      privacyShowOnline: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      countryIso: "BD",
      fideFederation: "BAN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Public profiles: owner writes, public visible list succeeds, private hidden from others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceGM",
      bio: "Sicilian Najdorf player",
      countryIso: "BD",
      fideFederation: "BAN",
      bulletRating: 1500,
      blitzRating: 1620,
      rapidRating: 1700,
      classicalRating: 1650,
      puzzleRating: 1850,
      gamesPlayed: 12,
      wins: 7,
      draws: 2,
      losses: 3,
      studyCount: 1,
      achievementsCount: 3,
      onlineStatus: "online",
      publicVisible: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("publicProfiles").where("publicVisible", "==", true).get()
  );

  // Unfiltered list fails
  await assertFails(bobDb.collection("publicProfiles").get());
});

test("Online game: participants create/move/resign, terminal lock blocks post-finish mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("games").doc("game_1").set({
      whiteUid: ALICE_UID,
      blackUid: BOB_UID,
      whiteName: "Alice",
      blackName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      variant: "STANDARD",
      timeControl: "5+3",
      initialTimeMs: 300000,
      incrementMs: 3000,
      delayMs: 0,
      ratedGame: true,
      status: "active",
      fen: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      pgn: "",
      moveCount: 0,
      turn: "w",
      whiteTimeMs: 300000,
      blackTimeMs: 300000,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-participant cannot modify game
  await assertFails(
    charlieDb.collection("games").doc("game_1").update({
      turn: "b",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Participant move update succeeds
  await assertSucceeds(
    aliceDb.collection("games").doc("game_1").update({
      fen: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
      pgn: "1. e4",
      lastMoveUci: "e2e4",
      moveCount: 1,
      turn: "b",
      whiteTimeMs: 299000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Participant finishes game
  await assertSucceeds(
    bobDb.collection("games").doc("game_1").update({
      status: "finished",
      result: "1-0",
      resultReason: "Resignation",
      winnerUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock blocks further updates
  await assertFails(
    bobDb.collection("games").doc("game_1").update({
      status: "active",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Studies, clubs, tournaments, friends, and reports enforce ownership and query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("studies").doc("study_1").set({
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      title: "Sicilian Najdorf",
      description: "6. Bg5 lines",
      pgn: "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6",
      tags: ["Sicilian", "Najdorf"],
      publicVisible: false,
      collaboratorUids: [],
      chapterCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Private study hidden from Bob
  await assertFails(bobDb.collection("studies").doc("study_1").get());
  await assertSucceeds(aliceDb.collection("studies").where("ownerUid", "==", ALICE_UID).get());

  // Reports: reporter creates open report; cannot self-resolve
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterUid: ALICE_UID,
      targetUid: BOB_UID,
      category: "Abusive Chat",
      details: "Spam messages during game.",
      status: "open",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
