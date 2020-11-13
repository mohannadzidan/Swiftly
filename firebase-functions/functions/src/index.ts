import admin = require('firebase-admin');
import * as functions from 'firebase-functions';
admin.initializeApp();
// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//
// export const helloWorld = functions.https.onRequest((request, response) => {
//   functions.logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
// });
exports.newMessages = functions.database.ref('/chats/{chatId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        let message = snapshot.val();
        const chatId = context.params.chatId;
        // get chat participants
        let promises = [] as Promise<void>[];
        await admin.database().ref('/chats/' + chatId + '/participants')
            .once('value', particepantsSnapshot => {
                let participants = Object.keys(particepantsSnapshot.val());
                participants.forEach(participantId => {
                    //console.log('pushing promise->'+participantId);
                    if (message.senderId != participantId)
                        promises.push(
                            admin.database().ref('/users/' + participantId + '/chatsIndex/' + chatId).set(message.timestamp)
                        );
                });
            });
        //console.log('promise all of = '+promises.length);
        return Promise.all(promises);
    });
