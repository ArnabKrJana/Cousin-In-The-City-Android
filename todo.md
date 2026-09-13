# Bug Fix: Update Intent Handling for Calendar and Keep

The backend prompt engineering has been updated to return more specific structured data for the Android Intents. Please update the `Intent` handling logic in the app to match these new rules:

## 1. Fix the Google Keep Intent (`intentType == "KEEP"`)
Previously, the backend was only sending a title, but it now sends a fully detailed note body.
- You must read `actionData["note"]` and inject it as the main text body of the Keep note.
- Launch an `ACTION_SEND` Intent with type `text/plain`.
- Set `Intent.EXTRA_TITLE` to `actionData["title"]`.
- Set `Intent.EXTRA_TEXT` to `actionData["note"]`.

## 2. Fix the Google Calendar Intent (`intentType == "CALENDAR"`)
Currently, the app is failing to schedule the actual time of the event because it is just pasting the date string into a text field. The backend now returns separate `date` and `time` fields. You **must** parse these into Epoch Milliseconds to use the `EXTRA_EVENT_BEGIN_TIME` extra correctly.

Use this exact logic:
```kotlin
val dateStr = actionData["date"] // e.g. "2026-09-14"
val timeStr = actionData["time"] // e.g. "10:08 PM"

// Parse the string into a LocalDateTime
val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", java.util.Locale.ENGLISH)
val localDateTime = java.time.LocalDateTime.parse("$dateStr $timeStr", formatter)

// Convert to Milliseconds
val startMillis = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

// Launch the Intent properly using Milliseconds!
val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
    .putExtra(android.provider.CalendarContract.Events.TITLE, actionData["title"])
    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, startMillis + (1000 * 60 * 60)) // +1 hour

context.startActivity(intent)