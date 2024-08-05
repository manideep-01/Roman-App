package com.example.roman

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.webrtc.*

class CallActivity : AppCompatActivity() {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var localVideoView: SurfaceViewRenderer
    private val rootEglBase = EglBase.create()
    private var isVideoCall = false // Flag to determine if it's a video call

    private companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val ROOM_CODE_KEY = "ROOM_CODE"
        private const val IS_VIDEO_CALL_KEY = "IS_VIDEO_CALL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        localVideoView = findViewById(R.id.localVideoView)
        val audioCallButton: Button = findViewById(R.id.audioCallButton)
        val videoCallButton: Button = findViewById(R.id.videoCallButton)

        val roomCode = intent.getStringExtra(ROOM_CODE_KEY)
        isVideoCall = intent.getBooleanExtra(IS_VIDEO_CALL_KEY, false)

        audioCallButton.setOnClickListener { startAudioCall() }
        videoCallButton.setOnClickListener { startVideoCall() }

        requestPermissions()
        initializePeerConnectionFactory()
        startLocalVideo()
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Permissions already granted
            if (isVideoCall) {
                startVideoCall()
            } else {
                startAudioCall()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, you can initiate the call here
                if (isVideoCall) {
                    startVideoCall()
                } else {
                    startAudioCall()
                }
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish() // Close activity if permissions are not granted
            }
        }
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(
                rootEglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun startLocalVideo() {
        localVideoView.init(rootEglBase.eglBaseContext, null)
        localVideoView.setEnableHardwareScaler(true)
        localVideoView.setMirror(true)

        val videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        val videoCapturer = createCameraCapturer(Camera1Enumerator(false))
        videoCapturer?.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)

        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource)
        localVideoTrack.addSink(localVideoView)

        videoCapturer?.startCapture(1280, 720, 30)

        // Create audio source and local audio track
        val audioConstraints = MediaConstraints()
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        peerConnectionFactory.createAudioTrack("101", audioSource).also { localAudioTrack = it }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    private fun startAudioCall() {
        // Implement logic for audio call
        Toast.makeText(this, "Audio Call Started", Toast.LENGTH_SHORT).show()
        // Example: Connect to signaling server, initiate call, handle events
    }

    private fun startVideoCall() {
        // Implement logic for video call
        Toast.makeText(this, "Video Call Started", Toast.LENGTH_SHORT).show()
        // Example: Connect to signaling server, initiate call, handle events
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        localVideoTrack.dispose()
        localAudioTrack.dispose()
        peerConnectionFactory.dispose()
        rootEglBase.release()
    }
}
