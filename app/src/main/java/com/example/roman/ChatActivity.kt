package com.example.roman

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class ChatActivity : AppCompatActivity() {

    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var uploadFileButton: Button
    private lateinit var startAudioCallButton: Button
    private lateinit var startVideoCallButton: Button
    private lateinit var messagesRecyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var roomCode: String
    private lateinit var chatListener: ListenerRegistration

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFile(it)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        roomCode = intent.getStringExtra("ROOM_CODE") ?: return

        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        uploadFileButton = findViewById(R.id.uploadFileButton)
        startAudioCallButton = findViewById(R.id.startAudioCallButton)
        startVideoCallButton = findViewById(R.id.startVideoCallButton)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesAdapter = MessagesAdapter()
        messagesRecyclerView.adapter = messagesAdapter

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageInput.text.clear() // Clear the input after sending
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        uploadFileButton.setOnClickListener {
            pickFile.launch("*/*")
        }

        startAudioCallButton.setOnClickListener {
            startCall(false) // Start audio call
        }

        startVideoCallButton.setOnClickListener {
            startCall(true) // Start video call
        }

        listenForMessages()
    }

    private fun sendMessage(message: String) {
        val messageData = hashMapOf(
            "text" to message,
            "timestamp" to System.currentTimeMillis(),
            "roomCode" to roomCode
        )
        // Assuming you have a Firestore reference to your chat room
        firestore.collection("rooms").document(roomCode)
            .collection("messages").add(messageData)
            .addOnSuccessListener {
                // Handle success if needed
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFile(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "file"
        val fileRef = storage.reference.child("rooms/$roomCode/$fileName")

        fileRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val message = "File: ${downloadUri.toString()}"
                    sendMessage(message)  // Pass the message to sendMessage function
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "File upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startCall(isVideoCall: Boolean) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("ROOM_CODE", roomCode)
            putExtra("IS_VIDEO_CALL", isVideoCall)
        }
        startActivity(intent)
    }

    private fun listenForMessages() {
        chatListener = firestore.collection("rooms").document(roomCode)
            .collection("messages").orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to listen for messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val messages = snapshots.documents.mapNotNull { it.getString("text") }
                    messagesAdapter.submitList(messages)

                    // Ensure messagesRecyclerView has been laid out and there are items in the adapter
                    if (messages.isNotEmpty()) {
                        messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener.remove()
    }
}
