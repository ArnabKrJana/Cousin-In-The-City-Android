package com.oneforth.cousininthecity.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NativeIntentUtils {

    /**
     * Launch Google Maps with a location query using ACTION_VIEW (geo:0,0?q={location}).
     */
    fun openGoogleMaps(context: Context, locationQuery: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(locationQuery)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("NativeIntentUtils", "Could not launch Maps app, trying web browser fallback", e)
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationQuery)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Log.e("NativeIntentUtils", "Failed to launch web browser for maps", ex)
            }
        }
    }

    /**
     * Launch Calendar app to create an event using ACTION_INSERT with CalendarContract.Events.CONTENT_URI.
     * Parses separate date and time strings into Epoch Milliseconds for EXTRA_EVENT_BEGIN_TIME and EXTRA_EVENT_END_TIME.
     */
    fun addCalendarEvent(context: Context, title: String?, dateStr: String?, timeStr: String?) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            // Reverted to standard data URI (removed strict MIME type which caused the OS block)
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title ?: "New Event")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (!dateStr.isNullOrBlank() && !timeStr.isNullOrBlank()) {
                try {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.ENGLISH)
                    val localDateTime = LocalDateTime.parse("$dateStr $timeStr", formatter)
                    val startMillis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + (1000 * 60 * 60))
                } catch (e: Exception) {
                    Log.e("NativeIntentUtils", "Failed to parse calendar date/time", e)
                    // Fallback to text description if parse fails
                    putExtra(CalendarContract.Events.DESCRIPTION, "Date: $dateStr\nTime: $timeStr")
                }
            } else if (!dateStr.isNullOrBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, "Date: $dateStr")
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("NativeIntentUtils", "Failed to launch Calendar intent", e)
        }
    }

    /**
     * Launch Google Keep or System Share Sheet using ACTION_SEND with text/plain.
     * Sets Intent.EXTRA_TITLE to title and Intent.EXTRA_TEXT to note body.
     */
    fun saveNote(context: Context, title: String?, note: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title ?: "Note")
            putExtra(Intent.EXTRA_TEXT, note)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            // Attempt to target Google Keep directly
            val keepIntent = Intent(intent).apply {
                setPackage("com.google.android.keep")
            }
            context.startActivity(keepIntent)
        } catch (e: Exception) {
            // Fallback to chooser for saving note or sharing
            val chooser = Intent.createChooser(intent, "Save Note").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Log.e("NativeIntentUtils", "Failed to launch share/keep intent", ex)
            }
        }
    }
}
