const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

function displayName(role) {
  if (role === "JESSIE") return "Jessie";
  if (role === "MATT") return "Matt";
  return "Someone";
}

exports.sendTaskCompletionBoop = onDocumentCreated(
  "households/{householdId}/completions/{completionId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const completion = snapshot.data();
    const householdId = event.params.householdId;
    const completionId = event.params.completionId;
    const completedByRole = completion.completedBy || "";

    const members = await getFirestore()
      .collection("households")
      .doc(householdId)
      .collection("members")
      .get();

    const tokens = members.docs
      .map((doc) => doc.data())
      .filter((member) => member.role !== completedByRole)
      .map((member) => member.fcmToken)
      .filter((token) => typeof token === "string" && token.length > 0);

    if (tokens.length === 0) return;

    await getMessaging().sendEachForMulticast({
      tokens,
      data: {
        completionId,
        completedBy: displayName(completedByRole),
        taskTitle: String(completion.taskTitle || "Task"),
        room: String(completion.room || "Around the house")
      },
      android: {
        priority: "high"
      }
    });
  }
);
