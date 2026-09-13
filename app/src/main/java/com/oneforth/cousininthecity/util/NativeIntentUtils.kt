package com.oneforth.cousininthecity.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log

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
     */
    fun addCalendarEvent(context: Context, title: String?, date: String?) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title ?: "New Event")
            if (!date.isNullOrBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, "Date / Details: $date")
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("NativeIntentUtils", "Failed to launch Calendar intent", e)
        }
    }

    /**
     * Launch Google Keep or System Share Sheet using ACTION_SEND with text/plain.
     */
    fun saveNote(context: Context, title: String?, note: String?) {
        val textToShare = buildString {
            if (!title.isNullOrBlank()) {
                append(title)
                if (!note.isNullOrBlank()) {
                    append("\n\n")
                }
            }
            if (!note.isNullOrBlank()) {
                append(note)
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title ?: "Note")
            putExtra(Intent.EXTRA_TEXT, textToShare)
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
