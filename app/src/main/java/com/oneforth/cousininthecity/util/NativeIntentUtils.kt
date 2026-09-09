package com.oneforth.cousininthecity.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

object NativeIntentUtils {

    fun launchCalendarIntent(
        context: Context,
        title: String,
        description: String,
        location: String? = null,
        startTimeMillis: Long? = null
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            startTimeMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
        }
        context.startActivity(intent)
    }

    fun launchSaveNoteIntent(
        context: Context,
        title: String,
        noteContent: String
    ) {
        val noteIntent = Intent("com.google.android.gms.actions.CREATE_NOTE").apply {
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, noteContent)
            type = "text/plain"
        }

        if (noteIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(noteIntent)
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$noteContent")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save Note To..."))
        }
    }

    fun launchGoogleMapsRouteIntent(
        context: Context,
        origin: String,
        destination: String,
        travelMode: String = "transit"
    ) {
        val gmmIntentUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${Uri.encode(origin)}" +
                    "&destination=${Uri.encode(destination)}" +
                    "&travelmode=$travelMode"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
        }
    }
}
