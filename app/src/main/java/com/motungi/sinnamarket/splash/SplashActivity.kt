package com.motungi.sinnamarket.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.LoginActivity
import com.motungi.sinnamarket.main.MainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        Handler(Looper.getMainLooper()).postDelayed({
            if(currentUser != null){ //자동 로그인
                startActivity(Intent(this, MainActivity::class.java))
            }
            else{ //로그인 필요
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2000) //2초 후 이동
    }
}
