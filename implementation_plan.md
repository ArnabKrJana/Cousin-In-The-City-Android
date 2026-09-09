# CousinInTheCity (Relocation Assistant)

## 1. Problem Domain
When students or professionals relocate to a new city for an internship or permanent job, they lack local context. They typically face several challenges:
- **Inter-city Travel**: Choosing the optimal way to reach the city (flight, train, bus) based on cost and time.
- **Location Intelligence**: Deciding where to live (which neighborhood) that balances affordable rent, safety, and a short commute to the office.
- **Accommodation**: Finding a suitable PG (Paying Guest), hostel, mess, or apartment.
- **Budget Management & Commute Trade-offs**: Figuring out the hidden costs of relocating. Often, finding a cheaper apartment far from the office results in massive daily commute costs and time loss, negating the rent savings.
- **Intra-city Commute**: Navigating local transport options to travel from their accommodation to the office.

## 2. Domain Solution
Instead of searching across multiple different platforms, **CousinInTheCity** provides a unified, conversational Android application. It acts as a Multi-Agent System where a central Agent Orchestrator delegates tasks to specialized AI tools (via MCP servers) to provide tailored guidance. The system intelligently analyzes trade-offs (like Rent vs Commute Cost) to give holistic financial advice, just like a trusted local relative.

## 3. Architecture & Modularization
*Inherits all best practices from the `spring-ai-travel-agent` reference architecture.*

The system follows a Microservices Architecture utilizing the Model Context Protocol (MCP). The frontend is a Native Android app, and the backend is composed of Spring Boot microservices. 

**Codebase Structure (Multi-module vs Microservices):**
- **Development**: The backend is organized as a **Spring Multi-module project**. 
- **Runtime**: Each module acts as a completely independent **Microservice**.

**Infrastructure Enhancements:**
- **Local LLM**: Integrated with **Ollama (llama3.2:3b)** for deep reasoning and complex Multi-Agent routing within 4GB VRAM.
- **Memory & Storage**: **PostgreSQL (pgvector) + JDBC** to store thread-isolated persistent chat history and vector embeddings for the RAG KnowledgeBase.
- **Caching**: **Redis** caching layer utilizing `@EnableCaching` on `@GetExchange` clients to eliminate redundant microservice HTTP calls.
- **Message Broker**: **RabbitMQ** to asynchronously process heavy background tasks (like the `PriceTrackerJob` FCM Notifications) using a robust Main Queue and Dead Letter Queue (DLQ).
- **Dockerized Environment**: A `compose.yaml` manages PostgreSQL, Redis, and RabbitMQ (Ollama runs natively on the host).

## 4. Architectural Standards (Clean Architecture)
To ensure the system is enterprise-ready, all microservices and the orchestrator strictly adhere to Clean Architecture principles:
- **Separation of Concerns**: Controllers only handle HTTP mappings. Business logic resides strictly in `@Service` classes.
- **Declarative HTTP Clients**: The Orchestrator communicates with other microservices via Spring's `@GetExchange` interface bindings.
- **DTOs and Entities**: Explicit segregation between Database Entities (e.g., `AppUser`, `ChatThread`) and DTOs.
- **AI Tool Integration**: AI functionality relies exclusively on the `@Tool` annotation attached to Service layer methods.

```mermaid
graph TD
    User([User]) <-->|Chat Interface| UI[Android App<br>Jetpack Compose]
    UI <-->|REST API| Agent[Agent Orchestrator<br>Spring Boot]
    
    %% Infrastructure
    Agent <-->|Chat Threads / Embeddings| PG[(PostgreSQL + pgvector)]
    Agent <-->|HTTP Client Caching| Redis[(Redis)]
    Agent <-->|Async Tasks| MQ[[RabbitMQ]]
    Agent <-->|Local AI (llama3.2:3b)| Ollama[Host Ollama]
    Agent <-->|Fault Tolerance| R4J{Resilience4j<br>Circuit Breaker}
    MQ -.->|Notification Consumer| FCM[Firebase Cloud Messaging]
    
    %% Domain Microservices (Protected by Circuit Breaker)
    R4J <-->|Declarative HTTP Clients| MCP_Travel[Travel Service]
    R4J <-->|Declarative HTTP Clients| MCP_Accomm[Accommodation Service]
    R4J <-->|Declarative HTTP Clients| MCP_Finance[Finance Service]
    R4J <-->|Declarative HTTP Clients| MCP_Location[Location Service]
```

## 5. Tech Stack & Design Decisions
- **Languages**: **Kotlin 2.3+** across the entire stack. Backend runs on **Java 25 JVM**.
- **Frontend**: Android Native, Jetpack Compose, Retrofit/Ktor.
- **Backend Framework**: Spring Boot 4.1+ with Spring AI.
- **AI Model**: **Ollama (gemma4:e2b)** and **nomic-embed-text:latest**.

---

## 6. Interview Reference: Architectural Decisions & Challenges

*This section is dedicated to helping you answer common system design and behavioral interview questions about this project.*

### Q1: Why did you choose a Multi-module Microservices Architecture instead of a Monolith?
**Answer:** "I realized early on that an AI Orchestrator needs to be extremely resilient. If I built a monolith and the flight search API crashed or timed out, it would bring down the entire AI Agent. By separating the domains (`mcp-travel`, `mcp-accommodation`, `mcp-finance`) into independent microservices, the Orchestrator can seamlessly handle failures. If the travel service goes down, the AI can still help the user find a PG or calculate a budget. It also perfectly mimics the Model Context Protocol (MCP) paradigm, allowing me to scale individual tools independently."

### Q2: How did you handle third-party API instability? (The Amadeus/Duffel Challenge)
**Answer:** "A major real-world challenge I faced was that third-party travel APIs like Amadeus deprecated their free developer sandboxes, and modern alternatives like Duffel required strict corporate banking entities which I didn't have as a student. Instead of abandoning the feature, I engineered a **Dynamic Mock Engine**. Inside the `mcp-travel` microservice, I wrote an algorithm that intercepts the user's origin/destination and dynamically generates highly realistic domestic flights (Indigo, Vistara), randomized prices in INR, and logical time intervals. This proved my ability to build decoupled systems—the AI Orchestrator has no idea the data is mocked because the HTTP API contract remains strictly identical to a real vendor's."

### Q3: Why did you use OpenStreetMap instead of Google Maps?
**Answer:** "Google Cloud Platform mandates strict bank verification and holds high authorizations just to use the free tier of the Google Maps API. To keep the infrastructure fully open and accessible, I pivoted to the **OpenStreetMap Nominatim API**. This allowed the Accommodation microservice to hit a real-world geocoding endpoint, fetch exact Latitude/Longitude coordinates for the user's office, and generate highly precise, clickable map links for nearby housing. It taught me how to adapt quickly to infrastructure blockers without compromising the user experience."

### Q4: How did you design the business logic for the Finance/Budget module?
**Answer:** "Initially, I was going to use a simple 'City Multiplier' (e.g., Mumbai = 1.5x cost). But I realized that's not how real people budget. If a user has a low salary in an expensive city, they don't give up; they just look for cheaper neighborhoods. So, I redesigned the Finance engine into an **Affordability Analyzer**. It takes the user's income and city, categorizes local neighborhoods into three tiers (Premium, Standard, Affordable), and flags which tiers fit the user's budget. The LLM reads this JSON, identifies the affordable neighborhood (e.g., Thane instead of South Bombay), and automatically passes that neighborhood string into the Accommodation tool to find actual housing they can afford. It's a perfect example of multi-agent chaining where one microservice's output directly drives the logic of another."

### Q5: How did you handle the hidden costs of relocating, like the trade-off between cheap rent and expensive commutes?
**Answer:** "This was one of the most critical business logic challenges. A user might find a very cheap flat in a distant suburb, but end up spending huge amounts of money and time commuting to their office, negating the rent savings. I engineered a **Commute Trade-off Analyzer** inside the Finance module. By taking the `officeLocation` as a reference point, the engine calculates the inverse relationship between Rent and Commute Cost. If it recommends an affordable neighborhood far from the office, it automatically spikes the daily commute cost (Uber/Metro). This allows the AI to provide holistic advice, telling the user: 'Thane is cheaper, but your commute to Bandra will cost ₹10,000/month. You are better off finding a mid-tier flat in Andheri.'"

### Q6: Why did you refactor the codebase to Clean Architecture?
**Answer:** "During development, I noticed that dumping mock data generation and third-party API calls directly into the Spring `@RestController` was turning the app into tightly coupled spaghetti code. I paused feature development and performed an 'Enterprise Refactor'. I enforced strict Separation of Concerns (SoC) by moving all business logic into `@Service` classes, implemented rigorous `Data Transfer Objects (DTOs)`, and utilized Spring's `@GetExchange` declarative HTTP clients. I also migrated the AI tool configurations from legacy functional beans to Spring AI's modern `@Tool` annotation on the service methods. This made the codebase highly modular, testable, and strictly aligned with enterprise MVC standards."

### Q7: What was the hardest bug you faced when integrating the LLM with your Microservices, and how did you solve it?
**Answer:** "The most challenging bug was when the LLM completely ignored all four of my microservice tools and hallucinated responses instead. It required deep debugging across three architectural layers. First, I discovered a Kotlin reflection bug where compiler optimizations were erasing my method parameter names into generic `arg0` and `arg1`. When the AI saw these, it panicked because it didn't know what data to provide. I fixed this by injecting the `-java-parameters` argument into Gradle. Second, I found a dependency injection flaw where my Controller was instantiating a raw `ChatClient.Builder` instead of injecting the properly configured Spring `@Bean` that contained the tools. Finally, I realized that the 2-Billion parameter `gemma` model I was using simply lacked the reasoning capacity to choose between 4 complex microservices simultaneously. I migrated the backend to Meta's officially supported `llama3.2:3b` model, which solved the issue perfectly while still fitting inside my 4GB VRAM constraint."

### Q8: How does your Agent handle unstructured queries like cultural nuances, and how do you protect the context window?
**Answer:** "While structured APIs handled flights and budgets, I needed a way to answer subjective questions like 'How do broker fees work in Mumbai?'. I engineered a **Retrieval-Augmented Generation (RAG)** pipeline inside the Orchestrator. On boot, the server reads unstructured Markdown guides, splits them using a `TokenTextSplitter`, and ingests them into a `pgvector` database. I exposed this as a 5th Agentic Tool. To ensure the LLM's 4096-token context window didn't overflow when querying massive documents, I engineered a `SearchRequest` utilizing strict **Top-K filtering** (`topK=3`). This guarantees the AI only pulls the 3 most mathematically relevant chunks of text from the database, protecting the context window and preventing hallucinations."

### Q9: Multi-Agent systems are notorious for generating aggressive HTTP traffic. How did you optimize network overhead?
**Answer:** "LLMs are unpredictable and will often request the same data multiple times during a 'Chain of Thought' reasoning loop (e.g. asking for 'Delhi to Mumbai flights' twice). To prevent the Orchestrator from accidentally DDOSing my internal microservices, I implemented a distributed caching layer using **Redis**. By placing Spring's `@Cacheable` annotations directly onto the `@GetExchange` declarative HTTP client interfaces, the Orchestrator intercepts redundant tool calls before they ever hit the network, serving the JSON instantly from Redis memory. This drastically reduced internal network latency and protected the downstream microservices from compute exhaustion."

### Q10: How did you design the background Notification Scheduler to handle high-volume push alerts without blocking the AI Orchestrator?
**Answer:** "In Phase 6, we needed the AI to automatically track flight prices overnight and send push notifications to users. Rather than processing API calls sequentially (which would block the main server thread), I decoupled the architecture using **RabbitMQ**. I wrote a Spring `@Scheduled` cron job that acts as a Producer, pushing 'FCM Notification' JSON payloads onto a Direct Exchange. I then built a `@RabbitListener` Consumer to asynchronously pull these tasks off the queue and send the physical Firebase Cloud Messaging (FCM) alerts. I also implemented a **Dead Letter Queue (DLQ)** to safely catch any failed tasks for manual review, ensuring zero message loss at scale."

### Q11: How did you ensure the Orchestrator's API was robust and safe for the Mobile App to consume?
**Answer:** "Mobile apps crash instantly if they receive unexpected HTML stack traces instead of JSON. To prevent this, I implemented a **Global Exception Handler** using Spring's `@RestControllerAdvice`. I created custom `ApiException` classes and utilized Kotlin Extension Functions to cleanly intercept and map every backend crash into a strict `ApiErrorResponse` JSON DTO (containing the error type, message, and cause). Furthermore, I integrated **OpenAPI (Swagger UI)** to auto-generate interactive API documentation, making it significantly easier to build the Android Retrofit networking layer."

### Q12: Your Orchestrator relies on multiple downstream microservices. How did you prevent cascading failures and thread exhaustion if a service goes down?
**Answer:** "To prevent the AI Orchestrator from hanging indefinitely while waiting for a dead microservice, I integrated **Resilience4j**. I wrapped all declarative `@GetExchange` HTTP calls within the AI `@Tool` services using `@Retry` (to handle transient network blips up to 3 times) and `@CircuitBreaker`. If a microservice fails consistently and crosses a 50% failure rate threshold, the circuit 'trips' to an `OPEN` state. Instead of blocking threads, Resilience4j instantly redirects the AI to a local `fallbackMethod` which safely returns an `emptyList()`. This allows the LLM to gracefully inform the user that the specific data is temporarily unavailable, without crashing the entire system."

### Q13: If the RabbitMQ server crashes, do you lose all your pending push notifications? And how does your backend actually talk to FCM?
**Answer:** "No, we don't lose any messages. When I configured RabbitMQ, I explicitly set the queues to be `durable=true` and utilized Spring AMQP's default persistent delivery mode. This forces RabbitMQ to write the notification JSON payloads directly to the physical disk. If the Docker container crashes and restarts, it reads the disk and restores the exact state of the queue. As for FCM communication, the backend currently mocks the push via a Logger. In production, once the Android App generates the Firebase project credentials, I will drop the `serviceAccountKey.json` into the backend and use the official **Firebase Admin SDK** inside the `@RabbitListener` consumer to fire the physical POST requests to Google's servers."

## Resilience4j: Theory & Concepts (Quick Reference)
Since fault-tolerance is a critical enterprise pattern, here are the core Resilience4j concepts used in this project:
- **The Circuit Breaker Pattern**: A state machine that monitors network calls.
  - **CLOSED**: Everything is working normally. Requests go through.
  - **OPEN**: The error threshold (e.g., 50%) is reached. The circuit "trips" and instantly rejects all new requests, sending them directly to the `fallbackMethod` to prevent thread exhaustion.
  - **HALF_OPEN**: After a `waitDurationInOpenState` (e.g., 15s), it allows a few test requests through. If they succeed, it closes the circuit. If they fail, it re-opens.
- **@Retry**: Automatically attempts the network call again (e.g., 3 times) before giving up and triggering the fallback.
- **Fallback Method**: A safety net method. It **must** have the exact same method signature as the original function, plus a final `Throwable` parameter.
- **Key YAML Properties**:
  - `slidingWindowSize: 5`: It looks at the last 5 calls to calculate the failure rate.
  - `failureRateThreshold: 50`: If 50% of the calls in the window fail, trip the circuit.
  - `waitDurationInOpenState: 15s`: How long to stay `OPEN` before testing `HALF_OPEN`.

---

## 7. Execution Roadmap (How we will code this)

**Step 1: The Brain (Agent Orchestrator Core)**
- Configure the Spring AI `ChatClient`.
- Implement JDBC Chat Memory so the AI remembers previous messages.
- Write the System Prompts (giving the AI its "Trusted Cousin" persona).
- Expose a simple REST API (`/chat`).

**Step 2: The Tools (MCP Servers)**
- Set up domain microservices (`mcp-travel`, `mcp-accommodation`, etc.).
- Build realistic Dynamic Mock Engines for APIs to decouple from fragile third-party limits (e.g., flight engines, local PGs).

**Step 3: The Protocol (Integration via Clean Architecture)**
- Extract HTTP communication into `@GetExchange` Interfaces.
- Use `@Tool` annotated services.

**Step 4: Real APIs & Long Term Memory**
- Integrate OpenStreetMap (Nominatim API) for precise Geolocation.
- Integrate `pgvector` for RAG (retrieval-augmented generation).

**Step 5: The Face (Android App & Mobile API)**
- Implemented `AppUser` and `ChatThread` backend models with Spring Data JPA.
- Exposed mobile-friendly endpoints (`GET /api/chat/history/{threadId}`) to populate Android chat bubbles.
- Move to Android Studio to build the Jetpack Compose UI.
- Wire up local Android Intents (Google Calendar, Google Keep, WhatsApp) directly from the AI's JSON output for true Autonomous Agent automation.
