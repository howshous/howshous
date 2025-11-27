package io.github.howshous.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeFormatter {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    fun formatTime(timestamp: Long): String {
        return try {
            timeFormat.format(Date(timestamp))
        } catch (e: Exception) {
            "--:-- --"
        }
    }

    fun formatDate(timestamp: Long): String {
        return try {
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            "-- ---, ----"
        }
    }

    fun formatDateTime(timestamp: Long): String {
        return try {
            dateTimeFormat.format(Date(timestamp))
        } catch (e: Exception) {
            "-- ---, -- -- a"
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} min ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> formatDate(timestamp)
        }
    }
}

object CurrencyFormatter {
    fun formatPrice(price: Int): String {
        return when {
            price >= 10000000 -> "₱${price / 10000000}Cr"
            price >= 100000 -> "₱${price / 100000}L"
            price >= 1000 -> "₱${price / 1000}K"
            else -> "₱$price"
        }
    }

    fun formatRent(price: Int): String {
        return "₱$price/month"
    }
}

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
