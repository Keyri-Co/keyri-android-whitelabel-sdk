package com.keyri.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyri.R
import com.keyri.auth.AuthActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    override fun onBackPressed() {
        startActivity(
            Intent(this, AuthActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    companion object {
        fun openHomeActivity(activity: AppCompatActivity) {
            activity.startActivity(Intent(activity, HomeActivity::class.java))
        }
    }
}
