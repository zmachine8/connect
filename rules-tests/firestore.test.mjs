import { test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import firebase from 'firebase/compat/app';
import 'firebase/compat/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-connect',
  firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
});

const alice = env.authenticatedContext('alice').firestore();
const bob = env.authenticatedContext('bob').firestore();
const outsider = env.authenticatedContext('charlie').firestore();
const legacy = env.authenticatedContext('legacy').firestore();
const guest = env.unauthenticatedContext().firestore();
const chat = 'alice_bob';
const chatDoc = (db) => db.doc(`chats/${chat}`);
const messages = (db) => db.collection(`chats/${chat}/messages`);

try {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const id of ['alice', 'bob', 'charlie']) {
      await db.doc(`users/${id}`).set({
        displayName: id,
        createdAt: new Date(),
        username: id,
        allowRequests: true,
      });
      await db.doc(`usernames/${id}`).set({ uid: id });
    }
    await db.doc('users/legacy').set({ displayName: 'Old Profile', createdAt: new Date() });
    await db.doc('groups/crew').set({ name: 'Crew', memberIds: ['alice'], createdBy: 'alice', createdAt: new Date() });
    await db.doc('groups/crew/messages/welcome').set({ senderId: 'alice', senderName: 'alice', text: 'Welcome', createdAt: new Date() });
    await db.doc('connections/alice_charlie').set({
      participantIds: ['alice', 'charlie'], requestId: 'seeded-accepted-request', createdAt: new Date(),
    });
    await chatDoc(db).set({ participantIds: ['alice', 'bob'], createdAt: new Date() });
    await messages(db).doc('first').set({ senderId: 'alice', type: 'text', text: 'Hello', createdAt: new Date() });
  });

  await test('only participants read the conversation and messages', async () => {
    await assertSucceeds(chatDoc(alice).get());
    await assertSucceeds(chatDoc(bob).get());
    await assertSucceeds(messages(bob).doc('first').get());
    await assertFails(chatDoc(outsider).get());
    await assertFails(messages(outsider).doc('first').get());
    await assertFails(chatDoc(guest).get());
  });

  await test('a participant can create a deterministic chat, but an outsider cannot', async () => {
    const stamp = () => firebase.firestore.FieldValue.serverTimestamp();
    await assertSucceeds(alice.doc('chats/alice_charlie').get());
    await assertSucceeds(alice.doc('chats/alice_charlie').set({
      participantIds: ['alice', 'charlie'], createdAt: stamp(),
    }));
    await assertFails(outsider.doc('chats/alice_bob_other').set({
      participantIds: ['alice', 'bob'], createdAt: stamp(),
    }));
    await assertFails(guest.doc('chats/bob_charlie').set({
      participantIds: ['bob', 'charlie'], createdAt: stamp(),
    }));
  });

  await test('only a participant can send as their own identity', async () => {
    const valid = { senderId: 'bob', type: 'text', text: 'Reply', createdAt: new Date() };
    // Seeded timestamps differ from request.time; use the Firestore server timestamp transform.
    const stamp = () => firebase.firestore.FieldValue.serverTimestamp();
    await assertSucceeds(messages(bob).add({ ...valid, createdAt: stamp() }));
    await assertFails(messages(outsider).add({ ...valid, senderId: 'charlie', createdAt: stamp() }));
    await assertFails(messages(alice).add({ ...valid, senderId: 'bob', createdAt: stamp() }));
    await assertFails(messages(alice).add({ ...valid, text: '', senderId: 'alice', createdAt: stamp() }));
    await assertFails(messages(alice).add({ senderId: 'alice', type: 'image', imageBytes: 'not bytes', createdAt: stamp() }));
  });

  await test('participants cannot change membership or another profile', async () => {
    await assertFails(chatDoc(alice).update({ participantIds: ['alice', 'charlie'] }));
    await assertFails(bob.doc('users/alice').update({ displayName: 'Impostor' }));
    await assertSucceeds(alice.doc('users/alice').update({ displayName: 'Alice' }));
    await assertFails(alice.doc('users/alice').update({ createdAt: new Date() }));
  });

  await test('legacy profiles without usernames can still be edited or migrated', async () => {
    await assertSucceeds(legacy.doc('users/legacy').update({ displayName: 'Updated Legacy' }));
    await assertFails(alice.doc('users/legacy').update({ displayName: 'Cross-account edit' }));

    const batch = legacy.batch();
    batch.set(legacy.doc('usernames/legacy_user'), { uid: 'legacy' });
    batch.update(legacy.doc('users/legacy'), {
      username: 'legacy_user', allowRequests: true,
    });
    await assertSucceeds(batch.commit());
  });

  await test('contact requests enforce sender, recipient, and acceptance membership', async () => {
    const stamp = () => firebase.firestore.FieldValue.serverTimestamp();
    const requestRef = alice.doc('contactRequests/alice_bob');
    await assertFails(guest.doc('contactRequests/alice_bob').get());
    await assertFails(guest.doc('contactRequests/alice_bob').set({
      senderId: 'alice', senderName: 'alice', recipientId: 'bob', status: 'pending', createdAt: stamp(),
    }));
    await assertFails(bob.doc('contactRequests/bob_alice').set({
      senderId: 'alice', senderName: 'alice', recipientId: 'bob', status: 'pending', createdAt: stamp(),
    }));
    await assertFails(alice.doc('contactRequests/alice_missing').set({
      senderId: 'alice', senderName: 'alice', recipientId: 'missing', status: 'pending', createdAt: stamp(),
    }));
    await assertFails(alice.doc('contactRequests/alice_alice').set({
      senderId: 'alice', senderName: 'alice', recipientId: 'alice', status: 'pending', createdAt: stamp(),
    }));

    await assertSucceeds(requestRef.set({
      senderId: 'alice', senderName: 'alice', recipientId: 'bob', status: 'pending', createdAt: stamp(),
    }));
    await assertSucceeds(requestRef.get());
    await assertSucceeds(bob.doc('contactRequests/alice_bob').get());
    await assertFails(outsider.doc('contactRequests/alice_bob').get());
    await assertSucceeds(bob.collection('contactRequests').where('recipientId', '==', 'bob').get());
    await assertFails(outsider.collection('contactRequests').where('recipientId', '==', 'bob').get());

    const batch = bob.batch();
    batch.update(bob.doc('contactRequests/alice_bob'), { status: 'accepted' });
    batch.set(bob.doc('connections/alice_bob'), {
      participantIds: ['alice', 'bob'], requestId: 'alice_bob', createdAt: stamp(),
    });
    await assertSucceeds(batch.commit());
    await assertFails(outsider.doc('connections/alice_bob').get());
  });

  await test('group membership gates group and message access', async () => {
    const stamp = () => firebase.firestore.FieldValue.serverTimestamp();
    await assertSucceeds(alice.doc('groups/crew').get());
    await assertSucceeds(alice.doc('groups/crew/messages/welcome').get());
    await assertFails(bob.doc('groups/crew').get());
    await assertFails(bob.doc('groups/crew/messages/welcome').get());
    await assertFails(guest.doc('groups/crew').get());

    await assertSucceeds(alice.doc('groups/crew').update({ memberIds: ['alice', 'bob'] }));
    await assertSucceeds(bob.doc('groups/crew').get());
    await assertSucceeds(bob.doc('groups/crew/messages/reply').set({
      senderId: 'bob', senderName: 'bob', text: 'Thanks', createdAt: stamp(),
    }));
    await assertFails(bob.doc('groups/crew/messages/forged').set({
      senderId: 'alice', senderName: 'alice', text: 'Forged sender', createdAt: stamp(),
    }));
    await assertFails(outsider.doc('groups/crew/messages/intrusion').set({
      senderId: 'charlie', senderName: 'charlie', text: 'Not a member', createdAt: stamp(),
    }));
    await assertFails(bob.doc('groups/crew').update({ memberIds: ['alice', 'bob', 'charlie'] }));
    await assertFails(outsider.doc('groups/crew').get());

    await assertSucceeds(alice.doc('groups/created-by-alice').set({
      name: 'New Group', memberIds: ['alice'], createdBy: 'alice', createdAt: stamp(),
    }));
    await assertFails(bob.doc('groups/forged-group').set({
      name: 'Forged Group', memberIds: ['alice'], createdBy: 'alice', createdAt: stamp(),
    }));
  });

  await test('only participants can read or send small chat photos', async () => {
    const imageBytes = firebase.firestore.Blob.fromUint8Array(new Uint8Array([0xff, 0xd8, 0xff, 0xd9]));
    await assertSucceeds(messages(alice).doc('photo123').set({
      senderId: 'alice', type: 'image', imageBytes,
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    }));
    await assertSucceeds(messages(bob).doc('photo123').get());
    await assertSucceeds(messages(alice).add({
      senderId: 'alice', type: 'image', imageBytes, text: 'Our photo',
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    }));
    await assertFails(messages(alice).add({
      senderId: 'alice', type: 'image', imageBytes, text: 'x'.repeat(2001),
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    }));
    await assertFails(messages(outsider).doc('photo123').get());
    await assertFails(messages(outsider).add({ senderId: 'charlie', type: 'image', imageBytes, createdAt: firebase.firestore.FieldValue.serverTimestamp() }));
    await assertFails(messages(alice).add({ senderId: 'alice', type: 'image', imageBytes: firebase.firestore.Blob.fromUint8Array(new Uint8Array(200001)), createdAt: firebase.firestore.FieldValue.serverTimestamp() }));
  });
  await test('document and location messages require valid data and participants', async () => {
    const stamp = () => firebase.firestore.FieldValue.serverTimestamp();
    const documentBytes = firebase.firestore.Blob.fromUint8Array(new Uint8Array([1, 2, 3]));
    const document = { senderId: 'alice', type: 'document', documentName: 'notes.pdf',
      documentMimeType: 'application/pdf', documentBytes, text: '', createdAt: stamp() };
    await assertSucceeds(messages(alice).add(document));
    await assertFails(messages(outsider).add({ ...document, senderId: 'charlie', createdAt: stamp() }));
    await assertFails(messages(alice).add({ ...document, documentName: '', createdAt: stamp() }));
    await assertFails(messages(alice).add({ ...document, documentBytes: firebase.firestore.Blob.fromUint8Array(new Uint8Array(200001)), createdAt: stamp() }));
    const location = { senderId: 'bob', type: 'location', latitude: 58.38, longitude: 26.72, createdAt: stamp() };
    await assertSucceeds(messages(bob).add(location));
    await assertFails(messages(outsider).add({ ...location, senderId: 'charlie', createdAt: stamp() }));
    await assertFails(messages(bob).add({ ...location, latitude: 100, createdAt: stamp() }));
  });
} finally {
  await env.cleanup();
}
