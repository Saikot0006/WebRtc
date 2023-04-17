package com.example.webrtc

import com.example.webrtc.model.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message:MessageModel)
}