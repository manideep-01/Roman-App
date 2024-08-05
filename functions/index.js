const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const firestore = admin.firestore();

// Example function: Scheduled task to clean up expired rooms every 30 minutes
exports.deleteExpiredRooms = functions.pubsub.schedule("every 30 minutes")
  .onRun(async (context) => {
    const currentTime = Date.now();
    const roomsRef = firestore.collection("rooms");
    const expiredRooms = await roomsRef
    .where("expiryTimestamp", "<=", currentTime)
    .get();


    const batch = firestore.batch();
    expiredRooms.forEach((doc) => {
      batch.delete(doc.ref);
    });

    await batch.commit();
    console.log("Deleted expired rooms");
  });

// Ensure to include a newline at the end of the file
