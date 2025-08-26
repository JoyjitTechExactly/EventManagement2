package com.example.eventmanagement2.util

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale

fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: (() -> Unit)? = null
) {
    view?.let { view ->
        val snackbar = Snackbar.make(view, message, duration)
        actionText?.let { text ->
            snackbar.setAction(text) { action?.invoke() }
        }
        snackbar.show()
    }
}

fun Fragment.showSnackbar(
    @StringRes messageRes: Int,
    duration: Int = Snackbar.LENGTH_SHORT,
    @StringRes actionTextRes: Int? = null,
    action: (() -> Unit)? = null
) {
    val message = getString(messageRes)
    val actionText = actionTextRes?.let { getString(it) }
    showSnackbar(message, duration, actionText, action)
}

fun Fragment.showError(
    message: String,
    actionText: String? = null,
    action: (() -> Unit)? = null
) {
    showSnackbar(message, Snackbar.LENGTH_LONG, actionText, action)
}

fun Fragment.showError(
    @StringRes messageRes: Int,
    @StringRes actionTextRes: Int? = null,
    action: (() -> Unit)? = null
) {
    showSnackbar(messageRes, Snackbar.LENGTH_LONG, actionTextRes, action)
}

