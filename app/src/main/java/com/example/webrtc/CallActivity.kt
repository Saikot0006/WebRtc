package com.example.webrtc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.webrtc.databinding.ActivityCallBinding
import com.example.webrtc.model.IceCandidateModel
import com.example.webrtc.model.MessageModel
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(),NewMessageInterface {
    lateinit var binding: ActivityCallBinding
    private var userName : String? = null
    private var socketRepository : SocketRepository? = null
    private var rtcClient : RTCClient? = null
    private var target : String = ""
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
       // setContentView(R.layout.activity_call)
        setContentView(binding.root)

        init()
    }

    private fun init(){
        userName = intent.getStringExtra("userName")
        socketRepository = SocketRepository(this)
        userName?.let {
            socketRepository?.initSocket(it) }
        rtcClient = RTCClient(application,userName!!,socketRepository!!,object : PeerConnectionObserver(){
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )

                socketRepository?.sendMessageToSocket(
                    MessageModel(
                        "ice_candidate",userName,target,candidate
                    )
                )
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
            }
        })
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)


        binding.apply {
            callBtn.setOnClickListener {
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                        "start_call",userName,targetUserNameEt.text.toString(),null
                    )
                )
                target = targetUserNameEt.text.toString()

            }

            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }
            micButton.setOnClickListener {
                if(isMute){
                    isMute = false
                    micButton.setImageResource(R.drawable.baseline_mic_off_24)
                }else{
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }

            videoButton.setOnClickListener {
                if(isCameraPause){
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.baseline_videocam_off_24)
                }else{
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if(isSpeakerMode){
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.baseline_hearing_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                }else{
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
            }

            endCallButton.setOnClickListener {
                setCallLayoutGone()
                whoToCallLayoutVisible()
                setIncomingCallingLayoutGone()
                rtcClient?.endCall()
            }
        }

    }

    override fun onNewMessage(message: MessageModel) {
        Log.e("message", "onNewMessage: $message" )
        when(message.type){
            "call_response"->{
                if(message.data == "user is not online"){
                    runOnUiThread{
                        Toast.makeText(this, "user is not reachable", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    runOnUiThread{
                        whoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }
                    }
                }
            }
            "answer_received"->{
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                binding.remoteViewLoading.visibility = View.GONE
            }
            "offer_received"->{
                runOnUiThread {
                    setIncomingCallingLayoutVisible()
                    binding.incommingNameTV.text = "${message.name.toString()} is calling you"
                    binding.acceptBtn.setOnClickListener {
                        setIncomingCallingLayoutGone()
                        setCallLayoutVisible()
                        whoToCallLayoutGone()

                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                        }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name!!)
                        target = message.name!!
                    }
                    binding.rejectBtn.setOnClickListener {
                        setIncomingCallingLayoutGone()
                    }
                    binding.remoteViewLoading.visibility = View.GONE
                }
            }
            "ice_candidate"->{
                runOnUiThread{
                    try {
                        val receiveCandidate = gson.fromJson(
                            gson.toJson(message.data),IceCandidateModel::class.java)
                        rtcClient?.addIceCandidate(IceCandidate(
                            receiveCandidate.sdpMid,
                            Math.toIntExact(receiveCandidate.sdpMLineIndex.toLong()),
                            receiveCandidate.sdpCandidate))
                    }catch (e:java.lang.Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun setIncomingCallingLayoutGone(){
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallingLayoutVisible(){
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone(){
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible(){
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun whoToCallLayoutGone(){
        binding.whoToCallLayout.visibility = View.GONE
    }

    private fun whoToCallLayoutVisible(){
        binding.whoToCallLayout.visibility = View.VISIBLE
    }


}