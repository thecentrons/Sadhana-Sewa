const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DEVOTEE_UID = "devotee_arjun_1";
const OTHER_DEVOTEE_UID = "devotee_bhima_2";
const COUNSELLOR_UID = "counsellor_prabhu_1";
const OTHER_COUNSELLOR_UID = "counsellor_other_2";
const INVITE_CODE = "GOUR108";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, role, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    uid,
    displayName: role === "COUNSELLOR" ? "Govinda Das" : "Arjuna Das",
    email: `${uid}@example.com`,
    role,
    inviteCode: role === "COUNSELLOR" ? INVITE_CODE : "",
    whatsappPhone: role === "COUNSELLOR" ? "+919876543210" : "",
    counsellorUid: "",
    counsellorName: "",
    counsellorWhatsapp: "",
    bookReadingTargetMinutes: 360,
    personalHearingTargetMinutes: 180,
    studyDailyTargetMinutes: 120,
    japaRoundsTarget: 16,
    readingTierLabel: "Bhima (6h)",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validLogPayload(devoteeUid, counsellorUid, dateKey = "2026-11-22", weekStartKey = "2026-11-16", overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    devoteeUid,
    counsellorUid,
    dateKey,
    weekStartKey,
    morningProgramAttended: true,
    chantingSlot: "BEFORE_730",
    japaRounds: 16,
    japaBeads: 0,
    bookReadingMinutes: 45,
    bookTitle: "Bhagavad-gita As It Is",
    morningClassAttended: true,
    personalHearingMinutes: 30,
    shlokaLearned: true,
    shlokaNote: "BG 2.13 - dehino 'smin yatha dehe",
    studyMinutes: 120,
    toBedSlot: "BEFORE_2115",
    wakeUpSlot: "BEFORE_0340",
    dayRestSlot: "UNDER_30",
    cardFilledOnTime: true,
    sevaMinutes: 60,
    sevaNote: "Temple hall cleaning",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(DEVOTEE_UID).get());
  await assertFails(unauthDb.collection("users").doc(DEVOTEE_UID).set(validUserPayload(DEVOTEE_UID, "DEVOTEE")));
  await assertFails(unauthDb.collection("inviteCodes").doc(INVITE_CODE).get());
  await assertFails(unauthDb.collection("sadhanaLogs").get());
  await assertFails(unauthDb.collection("weeklyFeedback").get());
});

test("User profile creation, devotee cannot self-modify spiritual targets, assigned counsellor CAN", async () => {
  const devoteeDb = testEnv.authenticatedContext(DEVOTEE_UID).firestore();
  const counsellorDb = testEnv.authenticatedContext(COUNSELLOR_UID).firestore();
  const otherCounsellorDb = testEnv.authenticatedContext(OTHER_COUNSELLOR_UID).firestore();
  const otherDevoteeDb = testEnv.authenticatedContext(OTHER_DEVOTEE_UID).firestore();

  // Create devotee & counsellor profiles
  await assertSucceeds(devoteeDb.collection("users").doc(DEVOTEE_UID).set(validUserPayload(DEVOTEE_UID, "DEVOTEE")));
  await assertSucceeds(counsellorDb.collection("users").doc(COUNSELLOR_UID).set(validUserPayload(COUNSELLOR_UID, "COUNSELLOR")));

  // Devotee links counsellor via allowed fields
  await assertSucceeds(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      counsellorUid: COUNSELLOR_UID,
      counsellorName: "Govinda Das",
      counsellorWhatsapp: "+919876543210",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // CRITICAL: Devotee MUST NOT be able to edit their own bookReading/hearing/study/japa targets
  await assertFails(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      bookReadingTargetMinutes: 120,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      personalHearingTargetMinutes: 60,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      studyDailyTargetMinutes: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Devotee cannot change immutable role or inject ghost fields
  await assertFails(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      role: "COUNSELLOR",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    devoteeDb.collection("users").doc(DEVOTEE_UID).update({
      displayName: "New Name",
      isVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Assigned counsellor CAN read devotee and update ONLY target fields
  await assertSucceeds(counsellorDb.collection("users").doc(DEVOTEE_UID).get());
  await assertSucceeds(
    counsellorDb.collection("users").where("counsellorUid", "==", COUNSELLOR_UID).get()
  );
  await assertSucceeds(
    counsellorDb.collection("users").doc(DEVOTEE_UID).update({
      bookReadingTargetMinutes: 300,
      personalHearingTargetMinutes: 240,
      studyDailyTargetMinutes: 90,
      japaRoundsTarget: 16,
      readingTierLabel: "Arjuna (5h)",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Assigned counsellor CANNOT modify devotee's displayName or email
  await assertFails(
    counsellorDb.collection("users").doc(DEVOTEE_UID).update({
      displayName: "Hacked Name",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Unassigned counsellor & other devotee cannot read or update devotee profile
  await assertFails(otherCounsellorDb.collection("users").doc(DEVOTEE_UID).get());
  await assertFails(otherDevoteeDb.collection("users").doc(DEVOTEE_UID).get());
  await assertFails(
    otherCounsellorDb.collection("users").doc(DEVOTEE_UID).update({
      bookReadingTargetMinutes: 180,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Unfiltered user listing must fail
  await assertFails(counsellorDb.collection("users").get());
});

test("Invite codes support direct get by code but block listing and unauthorized edits", async () => {
  const counsellorDb = testEnv.authenticatedContext(COUNSELLOR_UID).firestore();
  const devoteeDb = testEnv.authenticatedContext(DEVOTEE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    counsellorDb.collection("inviteCodes").doc(INVITE_CODE).set({
      code: INVITE_CODE,
      counsellorUid: COUNSELLOR_UID,
      counsellorName: "Govinda Das",
      counsellorWhatsapp: "+919876543210",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Direct lookup succeeds for authenticated devotee
  await assertSucceeds(devoteeDb.collection("inviteCodes").doc(INVITE_CODE).get());
  // Listing all invite codes fails
  await assertFails(devoteeDb.collection("inviteCodes").get());
  // Devotee cannot overwrite counsellor's invite code
  await assertFails(
    devoteeDb.collection("inviteCodes").doc(INVITE_CODE).update({
      counsellorWhatsapp: "+1000000000",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Counsellor can update own WhatsApp number on invite code
  await assertSucceeds(
    counsellorDb.collection("inviteCodes").doc(INVITE_CODE).update({
      counsellorWhatsapp: "+919999888877",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("SadhanaLogs: devotee writes & queries own logs, assigned counsellor reads via filtered query, others blocked", async () => {
  const devoteeDb = testEnv.authenticatedContext(DEVOTEE_UID).firestore();
  const counsellorDb = testEnv.authenticatedContext(COUNSELLOR_UID).firestore();
  const otherDevoteeDb = testEnv.authenticatedContext(OTHER_DEVOTEE_UID).firestore();

  const logId = `${DEVOTEE_UID}_20261122`;
  await assertSucceeds(
    devoteeDb.collection("sadhanaLogs").doc(logId).set(validLogPayload(DEVOTEE_UID, COUNSELLOR_UID))
  );

  // Devotee partial update succeeds (preserving createdAt)
  await assertSucceeds(
    devoteeDb.collection("sadhanaLogs").doc(logId).update({
      bookReadingMinutes: 60,
      japaRounds: 16,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    devoteeDb.collection("sadhanaLogs").doc(logId).update({
      bookReadingMinutes: 70,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Counsellor reads devotee log directly and via filtered query
  await assertSucceeds(counsellorDb.collection("sadhanaLogs").doc(logId).get());
  await assertSucceeds(
    counsellorDb.collection("sadhanaLogs").where("counsellorUid", "==", COUNSELLOR_UID).get()
  );
  await assertSucceeds(
    counsellorDb
      .collection("sadhanaLogs")
      .where("devoteeUid", "==", DEVOTEE_UID)
      .where("counsellorUid", "==", COUNSELLOR_UID)
      .get()
  );

  // Counsellor CANNOT modify devotee's sadhana log
  await assertFails(
    counsellorDb.collection("sadhanaLogs").doc(logId).update({
      bookReadingMinutes: 90,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Devotee filtered query succeeds; unfiltered fails
  await assertSucceeds(
    devoteeDb.collection("sadhanaLogs").where("devoteeUid", "==", DEVOTEE_UID).get()
  );
  await assertFails(devoteeDb.collection("sadhanaLogs").get());

  // Other devotee blocked
  await assertFails(otherDevoteeDb.collection("sadhanaLogs").doc(logId).get());
});

test("WeeklyFeedback: only assigned counsellor can create/update, devotee can read", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(DEVOTEE_UID)
      .set(validUserPayload(DEVOTEE_UID, "DEVOTEE", { counsellorUid: COUNSELLOR_UID }));
  });

  const counsellorDb = testEnv.authenticatedContext(COUNSELLOR_UID).firestore();
  const otherCounsellorDb = testEnv.authenticatedContext(OTHER_COUNSELLOR_UID).firestore();
  const devoteeDb = testEnv.authenticatedContext(DEVOTEE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const feedbackId = `${DEVOTEE_UID}_2026-11-16`;

  // Unassigned counsellor blocked from creating feedback
  await assertFails(
    otherCounsellorDb.collection("weeklyFeedback").doc(feedbackId).set({
      devoteeUid: DEVOTEE_UID,
      counsellorUid: OTHER_COUNSELLOR_UID,
      counsellorName: "Other",
      weekStartKey: "2026-11-16",
      blessingTag: "Steady",
      note: "Unauthorized note",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Assigned counsellor creates & updates feedback
  await assertSucceeds(
    counsellorDb.collection("weeklyFeedback").doc(feedbackId).set({
      devoteeUid: DEVOTEE_UID,
      counsellorUid: COUNSELLOR_UID,
      counsellorName: "Govinda Das",
      weekStartKey: "2026-11-16",
      blessingTag: "Wonderful Japa",
      note: "Haribol! Keep up your early morning rounds and steady Bhagavatam reading.",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Devotee reads feedback (direct & filtered list)
  await assertSucceeds(devoteeDb.collection("weeklyFeedback").doc(feedbackId).get());
  await assertSucceeds(
    devoteeDb.collection("weeklyFeedback").where("devoteeUid", "==", DEVOTEE_UID).get()
  );

  // Devotee cannot overwrite counsellor feedback
  await assertFails(
    devoteeDb.collection("weeklyFeedback").doc(feedbackId).update({
      note: "Self edited",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
