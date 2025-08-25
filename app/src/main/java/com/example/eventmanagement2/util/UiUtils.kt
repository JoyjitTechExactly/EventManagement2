package com.example.eventmanagement2.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

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

fun parseDateToMillis(dateString: String): Long? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return try {
        dateFormat.parse(dateString)?.time
    } catch (e: Exception) {
        null
    }
}

