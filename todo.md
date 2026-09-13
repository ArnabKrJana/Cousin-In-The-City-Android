# Android Agent Instructions: CousinInTheCity Mobile App

## 🎯 Goal
You are tasked with fixing and updating the Jetpack Compose Android application for "CousinInTheCity". The backend orchestrator has recently undergone a major architectural refactor to support structured JSON outputs, multi-agent tool calling, and long-term memory.

Your job is to investigate the existing Android codebase, update the Retrofit networking layer to match the new API contracts, fix the Jetpack Compose chat UI to consume the new structured responses, and implement native Android Intents for agentic automation.

## 🔍 Step 1: Codebase Investigation
Before writing any code, please analyze the following layers in the Android project:
1. **Network Layer:** Locate the Retrofit API interfaces and DTOs.
2. **ViewModel Layer:** Understand how chat state is currently managed.
3. **UI Layer:** Inspect the Jetpack Compose screens (especially the Chat Screen).

## 🛠️ Step 2: Update API Contracts & DTOs
The backend now returns a strict structured JSON output instead of a raw string. Update the Kotlin Data Classes to match the following API contracts exactly:

### 1. Send Message Endpoint (CRITICAL UPDATE)
**POST** `http://<backend-ip>:8080/api/chat`
**Request DTO (`ChatInput`):**
```json
{
  "prompt": "String (User's message)",
  "conversationId": "String (The Thread ID)" 
}

Response DTO (AgentResponse):

json


{
  "message": "String (The markdown text to display in the chat bubble)",
  "intentType": "String? (Nullable. Can be 'MAP', 'CALENDAR', or 'KEEP')",
  "actionData": "Map<String, String>? (Nullable. Key-value pairs for the intent)"
}
2. Thread Management Endpoints
Register Device: POST /api/chat/users/{deviceId}
Create Thread: POST /api/chat/users/{deviceId}/threads?title={title}
Get Threads: GET /api/chat/users/{deviceId}/threads
Get History: GET /api/chat/history/{threadId} (Returns List<MessageDto> containing role and content)
🎨 Step 3: Fix Jetpack Compose UI
Chat Bubbles: The Chat UI must render Markdown. It should extract the message string from the AgentResponse and display it in the Assistant's chat bubble.
Thread History: When opening a thread, fetch the history using the GET /api/chat/history/{threadId} endpoint and populate the LazyColumn.
🤖 Step 4: Implement Native Android Intents (Agentic Actions)
The backend AI acts as an autonomous agent. If the user asks for directions or to save a note, the backend will return an intentType. You must write Kotlin code to intercept this response and launch the corresponding native Android Intent automatically:

If intentType == "MAP":
Read actionData["location"].
Launch an ACTION_VIEW Intent with URI geo:0,0?q={location} to open Google Maps.
If intentType == "CALENDAR":
Read actionData["title"] and actionData["date"].
Launch an ACTION_INSERT Intent (Events.CONTENT_URI) to open the Calendar app.
If intentType == "KEEP":
Read actionData["title"] and actionData["note"].
Launch an ACTION_SEND Intent with type text/plain to save a note.