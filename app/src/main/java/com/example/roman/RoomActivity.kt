package com.example.roman

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RoomActivity : AppCompatActivity() {

    private lateinit var roomCodeInput: EditText
    private lateinit var joinRoomButton: Button
    private lateinit var createRoomButton: Button
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
        FirebaseApp.initializeApp(this);
        roomCodeInput = findViewById(R.id.roomCodeInput)
        joinRoomButton = findViewById(R.id.joinRoomButton)
        createRoomButton = findViewById(R.id.createRoomButton)

        joinRoomButton.setOnClickListener {
            val roomCode = roomCodeInput.text.toString().trim()
            if (roomCode.isNotEmpty()) {
                joinRoom(roomCode)
            }
        }

        createRoomButton.setOnClickListener {
            val roomCode = generateRoomCode()
            createRoom(roomCode)
        }
    }

    private fun generateRoomCode(): String {
        return List(6) { Random.nextInt(0, 10) }.joinToString("")
    }

    private fun createRoom(roomCode: String) {
        val room = hashMapOf(
            "code" to roomCode,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("rooms").document(roomCode).set(room)
            .addOnSuccessListener {
                // Room creation succeeded, you can log or navigate here
                Toast.makeText(this,"Room created successfully:$roomCode",Toast.LENGTH_SHORT).show()
                Log.d("RoomActivity", "Room created successfully: $roomCode")
            }
            .addOnFailureListener { e ->
                // Room creation failed, show error message
                Log.e("RoomActivity", "Failed to create room", e)
                Toast.makeText(this, "Failed to create room: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun joinRoom(roomCode: String) {
        // Log the attempt to join a room for debugging
        Log.d("RoomActivity", "Attempting to join room with code: $roomCode")

        // Firebase Firestore call to retrieve room details
        firestore.collection("rooms").document(roomCode).get()
            .addOnSuccessListener { document: DocumentSnapshot ->
                if (document.exists()) {
                    // Log successful room retrieval
                    Log.d("RoomActivity", "Room found: $roomCode")

                    // Navigate to ChatActivity with room code
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("ROOM_CODE", roomCode)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Room does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e: Exception ->
                // Log the error for debugging
                Log.e("RoomActivity", "Failed to join room", e)
                Toast.makeText(this, "Failed to join room: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
