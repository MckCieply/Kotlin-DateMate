package com.mckcieply.datemate

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast

object ToastHelper {

    fun showCustomToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            val toast = Toast(context)
            toast.duration = Toast.LENGTH_LONG

            val toastText = TextView(context).apply {
                text = message
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.BLACK)
                setTextColor(Color.WHITE)
            }

            toast.view = toastText
            toast.show()
        }
    }
}
