package com.example.eventmanagement2.util

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

fun View.hideKeyboard() {
    val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}
