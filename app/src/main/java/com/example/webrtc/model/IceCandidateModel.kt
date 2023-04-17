package com.example.webrtc.model

data class IceCandidateModel(
    val sdpMid : String,
    val sdpMLineIndex : Double,
    val sdpCandidate : String,
)