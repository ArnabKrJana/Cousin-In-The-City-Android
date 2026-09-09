package com.oneforth.cousininthecity.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.TimeZone
import androidx.core.net.toUri

object NativeIntentUtils {

    /**
     * JARVIS MODE: Silently inserts an event into the user's primary calendar without opening any UI.
     * Requires READ_CALENDAR and WRITE_CALENDAR permissions.
     */
    fun silentlyAddCalendarEvent(
        context: Context,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Jarvis", "Missing WRITE_CALENDAR permission")
            return false
        }

        try {
            // 1. Find the primary calendar ID
            var calendarId: Long = -1
            val projection = arrayOf(CalendarContract.Calendars._ID)
            val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1"
            
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    calendarId = cursor.getLong(0)
                }
            }

            if (calendarId == -1L) {
                // Fallback to the first available calendar if no primary is marked
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        calendarId = cursor.getLong(0)
                    }
                }
            }

            if (calendarId == -1L) return false

            // 2. Insert the event silently
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, endTimeMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri != null
        } catch (e: Exception) {
            Log.e("Jarvis", "Failed to insert calendar event silently", e)
            return false
        }
    }

    /**
     * JARVIS MODE: Automatically launches Google Maps for navigation/viewing.
     */
    fun openGoogleMaps(context: Context, locationQuery: String) {
        val uri = "geo:0,0?q=${Uri.encode(locationQuery)}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if Google Maps is not installed
            val browserUri =
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationQuery)}".toUri()
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    /**
     * JARVIS MODE: Automatically pops up Google Keep to save a note.
     */
    fun saveToGoogleKeep(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.google.android.keep")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard share sheet if Keep is not installed
            val chooser = Intent.createChooser(intent, "Save Note").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        }
    }
}
