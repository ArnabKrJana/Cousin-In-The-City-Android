# CousinInTheCity - Android App Architecture Decisions

## 1. Overview & Business Goals
**CousinInTheCity** is an AI-powered conversational relocation assistant Android application built with Jetpack Compose. It connects to the Spring Boot Agent Orchestrator microservices stack (`http://10.0.2.2:8080/api/chat`) to help users relocate seamlessly across Indian cities.

---

## 2. Technical Stack & Key Decisions

| Category | Library / Technology | Justification |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2+ | Standard for modern Android development |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI, dynamic dark/light theme |
| **Architecture** | Clean Architecture (UDF) | Strict separation into Data, Domain, and Presentation layers |
| **Dependency Injection** | Dagger Hilt | Compile-time safe, lifecycle-aware DI framework |
| **Networking** | Retrofit 2 + Gson | REST API consumption with Gson serialization |
| **HTTP Logging** | OkHttp Logging Interceptor | Intercepts and logs network traffic for debugging |
| **Local Persistence** | AndroidX DataStore Preferences | Lightweight, reactive preference storage for `deviceId` and settings |
| **State Management** | StateFlow + SharedFlow | ViewModel state exposure & single-event streams (`collectAsStateWithLifecycle`) |
| **Navigation** | Navigation Compose + Hilt Navigation | Screen routing and scoped ViewModel injection |
| **System Integrations** | Native Android Intents | Direct actions for Google Maps route, Calendar event creation, and Google Keep notes |

---

## 3. Architecture Blueprint

```
                     ┌──────────────────────────────────────────────┐
                     │              Presentation Layer              │
                     │  - Jetpack Compose UI Screens / Components   │
                     │  - ChatViewModel with StateFlow & SharedFlow │
                     │  - Native Intent Action Helpers               │
                     └──────────────────────┬───────────────────────┘
                                            │
                                            ▼
                     ┌──────────────────────────────────────────────┐
                     │                 Domain Layer                 │
                     │  - Domain Models (AppUser, ChatThread, etc.) │
                     │  - Use Cases (SendMessage, GetHistory, etc.) │
                     │  - Repository Interfaces                     │
                     └──────────────────────┬───────────────────────┘
                                            │
                                            ▼
                     ┌──────────────────────────────────────────────┐
                     │                  Data Layer                  │
                     │  - Repository Implementations                │
                     │  - Retrofit API Service & DTOs               │
                     │  - DataStore Preferences Local Source        │
                     │  - DTO <-> Domain Model Mappers              │
                     └──────────────────────┬───────────────────────┘
                                            │
                                            ▼
                     ┌──────────────────────────────────────────────┐
                     │            Spring Boot Backend               │
                     │   http://10.0.2.2:8080/api/chat             │
                     └──────────────────────────────────────────────┘
```

---

## 4. Package Structure

```
com.oneforth.cousininthecity/
├── CousinApplication.kt                  # @HiltAndroidApp Application entry point
├── MainActivity.kt                       # @AndroidEntryPoint Single activity host
├── data/
│   ├── local/
│   │   └── UserPreferencesDataSource.kt # DataStore manager for persistent DeviceId
│   ├── remote/
│   │   ├── CousinApi.kt                  # Retrofit REST interface
│   │   └── dto/
│   │       ├── ChatInputDto.kt
│   │       ├── ChatOutputDto.kt
│   │       ├── MessageDto.kt
│   │       ├── AppUserDto.kt
│   │       └── ChatThreadDto.kt
│   ├── repository/
│   │   ├── ChatRepositoryImpl.kt
│   │   └── UserRepositoryImpl.kt
│   └── mapper/
│       └── ChatMappers.kt                # DTO <-> Domain model extensions
├── domain/
│   ├── model/
│   │   ├── AppUser.kt
│   │   ├── ChatThread.kt
│   │   └── ChatMessage.kt
│   ├── repository/
│   │   ├── ChatRepository.kt
│   │   └── UserRepository.kt
│   └── usecase/
│       ├── RegisterUserUseCase.kt
│       ├── GetChatThreadsUseCase.kt
│       ├── CreateChatThreadUseCase.kt
│       ├── GetChatHistoryUseCase.kt
│       └── SendMessageUseCase.kt
├── di/
│   ├── NetworkModule.kt                  # Hilt module for OkHttp & Retrofit
│   ├── DataModule.kt                     # Hilt module for DataStore & Repositories
│   └── DomainModule.kt                   # Hilt module for Use Cases
├── ui/
│   ├── theme/                            # Color, Typography, Shape, Theme
│   ├── components/
│   │   ├── ChatBubble.kt                 # Message bubble with intent chips
│   │   ├── ChatInputBar.kt               # Message text input field
│   │   └── ThreadDrawerContent.kt        # Past threads navigation drawer
│   └── chat/
│       ├── ChatScreen.kt                 # Main screen view
│       ├── ChatViewModel.kt              # ViewModel managing StateFlow
│       └── ChatUiState.kt                # UI State & UiEvent definitions
└── util/
    └── NativeIntentUtils.kt              # Calendar, Google Keep & Maps intent handlers
```

---

## 5. Native Android Intents Specification

### A. Add Calendar Event
- **Action**: `Intent.ACTION_INSERT` with `CalendarContract.EVENTS`
- **Use Case**: Save flight departure times or relocation milestones to user calendar.

### B. Save Note to Google Keep / System Share
- **Action**: `com.google.android.gms.actions.CREATE_NOTE` (Fallback: `Intent.ACTION_SEND`)
- **Use Case**: Export housing options, budget breakdowns, or relocation checklists directly to Google Keep.

### C. Open Google Maps Directions
- **Action**: `Intent.ACTION_VIEW` with `https://www.google.com/maps/dir/?api=1&origin=...&destination=...&travelmode=transit`
- **Use Case**: Display commute transit routes between accommodation and office.
