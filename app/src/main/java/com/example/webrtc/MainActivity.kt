package com.example.webrtc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.webrtc.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_main)
        setContentView(binding.root)

        binding.enterBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO,
                ).request{allGranted,_,_ ->
                    if(allGranted){
                        startActivity(
                            Intent(this,CallActivity::class.java)
                                .putExtra("userName",binding.userName.text.toString())
                        )
                    }else{
                        Toast.makeText(this, "You should accept all permission", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}