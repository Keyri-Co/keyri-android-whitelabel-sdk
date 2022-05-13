package com.keyrico.keyrisdk

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationBottomDialog

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(view)

        val session = intent.getParcelableExtra<Session>(SESSION) ?: let {
            setResult(RESULT_CANCELED)
            finish()

            return
        }

        val dialog = ConfirmationBottomDialog(session) { isAccepted ->
            val result = if (isAccepted) RESULT_OK else RESULT_CANCELED

            setResult(result)
            finish()
        }

        dialog.show(supportFragmentManager, ConfirmationBottomDialog::class.java.name)

        view.postDelayed({
            dialog.view?.findViewById<Button>(R.id.bYes)?.callOnClick()
        }, 3_000L)
    }

    companion object {
        const val SESSION = "SESSION"
    }
}
