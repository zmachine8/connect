const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");

initializeApp();
const db = getFirestore();
const REGION = "europe-west1";

function preview(message) {
  const text = String(message.text || "").trim();
  if (text) return text.slice(0, 180);
  switch (message.type) {
    case "image": return "Sent a photo";
    case "document": return `Sent a file${message.documentName ? `: ${message.documentName}` : ""}`;
    case "location": return "Shared a location";
    default: return "Sent a message";
  }
}

async function processNewMessage(event, { conversationType, conversationId, titleFor }) {
  const message = event.data?.data();
  if (!message || typeof message.senderId !== "string") return;

  const conversationRef = conversationType === "direct"
    ? db.collection("chats").doc(conversationId)
    : db.collection("groups").doc(conversationId);
  const conversation = await conversationRef.get();
  if (!conversation.exists) return;
  const memberIds = conversationType === "direct"
    ? (conversation.get("participantIds") || [])
    : (conversation.get("memberIds") || []);
  const recipients = [...new Set(memberIds.filter((uid) => typeof uid === "string" && uid !== message.senderId))];
  if (!recipients.length) return;

  // Store processing state on the message so a trigger retry cannot increment unread twice.
  const messageRef = event.data.ref;
  const shouldSendPush = await db.runTransaction(async (tx) => {
    const currentMessage = await tx.get(messageRef);
    if (currentMessage.get("connectNotificationsProcessed") !== true) {
      const unreadRefs = recipients.map((uid) => db.collection("users").doc(uid)
        .collection("unreadChats").doc(`${conversationType}_${conversationId}`));
      const unreadDocs = await Promise.all(unreadRefs.map((ref) => tx.get(ref)));
      tx.update(messageRef, { connectNotificationsProcessed: true });
      unreadRefs.forEach((ref, index) => {
        tx.set(ref, {
          count: (unreadDocs[index].get("count") || 0) + 1,
          type: conversationType,
          conversationId,
          updatedAt: new Date(),
        });
      });
    }
    return currentMessage.get("connectPushSent") !== true;
  });
  if (!shouldSendPush) return;

  // If a retry follows an FCM failure, it can re-attempt delivery without recounting.
  const sender = await db.collection("users").doc(message.senderId).get();
  const senderName = sender.get("displayName") || "Connect user";
  const body = preview(message);
  const title = titleFor(senderName, conversation.data());
  const installationSnapshots = await Promise.all(recipients.map((uid) => db.collection("users").doc(uid)
    .collection("fcmInstallations").get()));
  const installationEntries = installationSnapshots.flatMap((snapshot) => snapshot.docs.map((doc) => ({
    ref: doc.ref,
    fid: doc.get("installationId"),
  }))).filter((entry) => typeof entry.fid === "string" && entry.fid.length > 0);
  if (!installationEntries.length) {
    await messageRef.update({ connectPushSent: true });
    return;
  }

  for (let start = 0; start < installationEntries.length; start += 500) {
    const batch = installationEntries.slice(start, start + 500);
    try {
      const response = await getMessaging().sendEachForMulticast({
        fids: batch.map((entry) => entry.fid),
        notification: { title, body },
        data: { type: conversationType, conversationId, senderId: message.senderId },
        android: { notification: { channelId: "connect_messages" } },
      });
      const stale = [];
      response.responses.forEach((result, index) => {
        const code = result.error?.code || "";
        if (code.includes("installation-id-not-registered")) {
          stale.push(batch[index].ref.delete());
        }
      });
      await Promise.all(stale);
    } catch (error) {
      logger.error("Could not send Connect message push", { conversationType, conversationId, error });
      throw error;
    }
  }
  await messageRef.update({ connectPushSent: true });
}

exports.notifyDirectMessage = onDocumentCreated({
  document: "chats/{chatId}/messages/{messageId}",
  region: REGION,
  retry: true,
}, (event) => processNewMessage(event, {
  conversationType: "direct",
  conversationId: event.params.chatId,
  titleFor: (senderName) => senderName,
}));

exports.notifyGroupMessage = onDocumentCreated({
  document: "groups/{groupId}/messages/{messageId}",
  region: REGION,
  retry: true,
}, (event) => processNewMessage(event, {
  conversationType: "group",
  conversationId: event.params.groupId,
  titleFor: (senderName, group) => `${senderName} · ${group.name || "Group"}`,
}));
