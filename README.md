# AI Live Inspector

A demo application for real-time home visual inspection using a laptop/mobile camera, powered by a local multimodal vision model via Ollama.

## Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Frontend  | React + Vite                        |
| Backend   | Java 21, Spring Boot 4.x, Spring AI |
| AI Model  | Ollama (gemma3 or any vision model) |

---

## Prerequisites

Make sure the following are installed before running:

| Tool       | Version  | Install                              |
|------------|----------|--------------------------------------|
| Java       | 21+      | https://adoptium.net                 |
| Node.js    | 18+      | https://nodejs.org                   |
| npm        | 8+       | Comes with Node.js                   |
| Ollama     | latest   | https://ollama.com                   |

---

## 1. Set Up Ollama

Install and start Ollama, then pull the vision model:

```bash
# Start Ollama (runs on http://localhost:11434)
ollama serve

# Pull the vision model (in a separate terminal)
ollama pull gemma3
```

> To use a different vision model (e.g. `llava`, `qwen2.5vl`), update `src/main/resources/application.yaml`:
> ```yaml
> spring.ai.ollama.chat.model: llava
> ```

---

## 2. Run the Backend

From the project root:

```bash
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**

> On Windows use `mvnw.cmd spring-boot:run`

To verify it's running:
```bash
curl -X POST http://localhost:8080/api/inspection/session
# Expected: {"sessionId":"<uuid>"}
```

---

## 3. Run the Frontend

In a **separate terminal**, from the project root:

```bash
cd src/main/frontend
npm install       # only needed the first time
npm run dev
```

The frontend starts on **http://localhost:5173**

Open **http://localhost:5173** in your browser.

---

## 4. Using the App

1. Allow camera access when the browser prompts
2. Point the camera at a room/wall/area you want to inspect
3. Click **Start Inspection** — frames are sent to the backend every ~1.5 seconds
4. Findings appear in the right panel in real time with type, confidence, severity, and recommended action
5. Click **Stop Inspection** to pause
6. Click **Generate Report** to see only confirmed (persistent) findings

---

## API Endpoints

| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| POST   | `/api/inspection/session`         | Create a new inspection session    |
| POST   | `/api/inspection/{id}/frame`      | Submit a JPEG frame for analysis   |
| GET    | `/api/inspection/{id}`            | Get current session findings       |
| POST   | `/api/inspection/{id}/report`     | Generate confirmed findings report |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/dburnwal/inspectionassistant/
│   │   ├── controller/        # REST endpoints
│   │   ├── inspection/        # Domain models, session, finding tracker
│   │   ├── vision/            # Ollama vision analyzer
│   │   ├── ai/                # Prompt builder
│   │   └── dto/               # Response DTOs
│   ├── resources/
│   │   └── application.yaml   # Config (model, port, logging)
│   └── frontend/              # React + Vite app
│       └── src/
│           └── App.jsx        # Main UI component
```

---

## Configuration

All backend config is in `src/main/resources/application.yaml`:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434   # Ollama URL
      chat:
        model: gemma3                    # Vision model name
        options:
          temperature: 0.2

server:
  port: 8080
```

---

## Troubleshooting

**Port 8080 already in use**
```bash
# Find and kill the process
lsof -ti :8080 | xargs kill -9
```

**Session not found error in browser**
- The backend was restarted while the frontend was still running
- Click **Start Inspection** again — this creates a fresh session

**Camera not working**
- Make sure you're accessing the app over `http://localhost` (not an IP address)
- Grant camera permissions when the browser prompts

**Ollama model not responding**
- Confirm Ollama is running: `curl http://localhost:11434`
- Confirm the model is pulled: `ollama list`
- The model name in `application.yaml` must exactly match the pulled model name

**macOS Netty DNS warning**
- Already suppressed via logging config — safe to ignore if it still appears

---

## Running Both Services — Quick Reference

```bash
# Terminal 1 — Ollama
ollama serve

# Terminal 2 — Backend
./mvnw spring-boot:run

# Terminal 3 — Frontend
cd src/main/frontend && npm run dev
```

Then open **http://localhost:5173**
