package com.example.eventmanagement2.util

import android.text.TextUtils
import java.util.*

object ValidationUtils {
    
    fun isTitleValid(title: String?): Boolean {
        return !TextUtils.isEmpty(title?.trim())
    }
    
    fun isDateValid(date: Date?): Boolean {
        if (date == null) return false
        return !date.before(Calendar.getInstance().time)
    }
    
    fun isTimeValid(time: Date?): Boolean {
        return time != null
    }
    
    fun isLocationValid(location: String?): Boolean {
        return !TextUtils.isEmpty(location?.trim())
    }
    
    fun isCategoryValid(category: String?): Boolean {
        return !TextUtils.isEmpty(category?.trim())
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String = ""
    )
    
    fun validateEvent(
        title: String?,
        date: Date?,
        time: Date?,
        location: String?,
        category: String?
    ): ValidationResult {
        return when {
            !isTitleValid(title) -> ValidationResult(false, "Title is required")
            !isDateValid(date) -> ValidationResult(false, "Please select a valid future date")
            !isTimeValid(time) -> ValidationResult(false, "Please select a valid time")
            !isLocationValid(location) -> ValidationResult(false, "Location is required")
            !isCategoryValid(category) -> ValidationResult(false, "Category is required")
            else -> ValidationResult(true)
        }
    }
}
