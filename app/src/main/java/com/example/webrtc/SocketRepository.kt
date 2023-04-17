package com.example.webrtc

import android.net.Uri
import android.util.Log
import com.example.webrtc.model.MessageModel
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class SocketRepository(private val messageInterface: NewMessageInterface) {
    private var webSocket : WebSocketClient? = null
    private var userName : String? = null
    private val TAG = "SocketRepository"
    private val gson = Gson()

    fun initSocket(username : String){
        //172.16.3.184:3000
        userName = username
        webSocket = object : WebSocketClient(URI("ws://172.16.3.184:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(MessageModel(
                    "store_user",userName,null,null
                ))
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.e(TAG, "onClose: "+reason )
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "onError: "+ex)
            }
        }
        webSocket?.connect()
    }

    fun sendMessageToSocket(message: MessageModel){
        try {
            webSocket?.send(Gson().toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}